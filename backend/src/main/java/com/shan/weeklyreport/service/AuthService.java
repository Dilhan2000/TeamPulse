package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.RefreshToken;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.ChangePasswordRequest;
import com.shan.weeklyreport.dto.LoginRequest;
import com.shan.weeklyreport.dto.MessageResponse;
import com.shan.weeklyreport.dto.RegisterRequest;
import com.shan.weeklyreport.dto.UserProfileResponse;
import com.shan.weeklyreport.exception.AccountNotActiveException;
import com.shan.weeklyreport.exception.BadRequestException;
import com.shan.weeklyreport.exception.DuplicateEmailException;
import com.shan.weeklyreport.exception.InvalidCredentialsException;
import com.shan.weeklyreport.exception.ResourceNotFoundException;
import com.shan.weeklyreport.mapper.UserMapper;
import com.shan.weeklyreport.repository.RefreshTokenRepository;
import com.shan.weeklyreport.repository.UserRepository;
import com.shan.weeklyreport.security.AuthCookieFactory;
import com.shan.weeklyreport.security.JwtService;
import com.shan.weeklyreport.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Authentication service implementing register, login, refresh, logout, and me (C1-T11..T15).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthCookieFactory cookieFactory;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthCookieFactory cookieFactory) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cookieFactory = cookieFactory;
    }

    /**
     * Registers a new user with status PENDING_APPROVAL (C1-T11).
     */
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (request.requestedRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot self-register with ADMIN role.");
        }

        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new DuplicateEmailException("Email is already registered: " + request.email().trim());
        }

        User user = new User(
                request.fullName().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.requestedRole(),
                AccountStatus.PENDING_APPROVAL
        );

        userRepository.save(user);
        return new MessageResponse("Registration submitted. An administrator will review your account.");
    }

    /**
     * Authenticates a user and issues JWT access and refresh token cookies (C1-T12).
     */
    @Transactional
    public UserProfileResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(user.getStatus());
        }

        issueTokensAndSetCookies(user, response);
        return UserMapper.toProfileResponse(user);
    }

    /**
     * Revokes the current refresh token and clears auth cookies (C1-T13).
     */
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> rawRefreshToken = cookieFactory.extractRefreshToken(request);
        if (rawRefreshToken.isPresent()) {
            String hash = jwtService.hashToken(rawRefreshToken.get());
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }

        clearAuthCookies(response);
    }

    /**
     * Validates and rotates the refresh token, issuing new access and refresh tokens (C1-T14).
     */
    @Transactional
    public UserProfileResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = cookieFactory.extractRefreshToken(request)
                .orElseThrow(() -> {
                    clearAuthCookies(response);
                    return new InvalidCredentialsException("Refresh token is missing.");
                });

        String hash = jwtService.hashToken(rawRefreshToken);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> {
                    clearAuthCookies(response);
                    return new InvalidCredentialsException("Invalid refresh token.");
                });

        if (existingToken.isRevoked() || existingToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            clearAuthCookies(response);
            throw new InvalidCredentialsException("Refresh token is expired or revoked.");
        }

        User user = existingToken.getUser();
        if (user.getStatus() != AccountStatus.ACTIVE) {
            clearAuthCookies(response);
            throw new AccountNotActiveException(user.getStatus());
        }

        // Token rotation: revoke old token
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        // Issue new token pair
        issueTokensAndSetCookies(user, response);

        return UserMapper.toProfileResponse(user);
    }

    /**
     * Fetches the current authenticated user's fresh database profile (C1-T15).
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Not authenticated.");
        }

        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.id()));

        return UserMapper.toProfileResponse(user);
    }

    /**
     * Changes the current user's password and revokes all refresh tokens (C7-B01).
     */
    @Transactional
    public MessageResponse changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        if (principal == null) {
            throw new InvalidCredentialsException("Not authenticated.");
        }

        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.id()));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllForUser(user.getId());
        return new MessageResponse("Password changed successfully.");
    }

    private void issueTokensAndSetCookies(User user, HttpServletResponse response) {
        // Access token
        String accessToken = jwtService.generateAccessToken(user);
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieFactory.createAccessTokenCookie(accessToken, jwtService.getAccessTtl()).toString()
        );

        // Refresh token
        String rawRefreshToken = jwtService.generateRefreshTokenValue();
        String refreshHash = jwtService.hashToken(rawRefreshToken);
        LocalDateTime expiry = LocalDateTime.now().plus(jwtService.getRefreshTtl());

        RefreshToken tokenEntity = new RefreshToken(user, refreshHash, expiry);
        refreshTokenRepository.save(tokenEntity);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieFactory.createRefreshTokenCookie(rawRefreshToken, jwtService.getRefreshTtl()).toString()
        );
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.createClearAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.createClearRefreshTokenCookie().toString());
    }
}
