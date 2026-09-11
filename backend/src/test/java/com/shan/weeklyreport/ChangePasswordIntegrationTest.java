package com.shan.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.RefreshToken;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.ChangePasswordRequest;
import com.shan.weeklyreport.dto.LoginRequest;
import com.shan.weeklyreport.repository.*;
import com.shan.weeklyreport.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ChangePasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ReportReviewRepository reportReviewRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        reportReviewRepository.deleteAll();
        reportVersionRepository.deleteAll();
        reportRepository.deleteAll();

        userRepository.findAll().forEach(u -> {
            if (!"admin@weeklyreport.local".equals(u.getEmail())) {
                userRepository.delete(u);
            }
        });

        userRepository.findByEmail("admin@weeklyreport.local")
                .orElseGet(() -> userRepository.save(new User(
                        "Admin",
                        "admin@weeklyreport.local",
                        passwordEncoder.encode("ChangeMe123!"),
                        Role.ADMIN,
                        AccountStatus.ACTIVE
                )));

        testUser = new User(
                "Alice Tester",
                "alice-pwd@test.com",
                passwordEncoder.encode("OldPassword123!"),
                Role.TEAM_MEMBER,
                AccountStatus.ACTIVE
        );
        testUser = userRepository.save(testUser);
    }

    private Cookie getAuthCookie(User user) {
        String token = jwtService.generateAccessToken(user);
        return new Cookie("access_token", token);
    }

    @Test
    @DisplayName("C7-B01: Wrong current password returns 400 without changing password")
    void wrongCurrentPasswordReturns400() throws Exception {
        Cookie authCookie = getAuthCookie(testUser);

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword999!", "NewValidPass123!");

        mockMvc.perform(patch("/api/auth/me/password")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Current password is incorrect")));

        // Old password still works
        LoginRequest loginReq = new LoginRequest("alice-pwd@test.com", "OldPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C7-B01: Weak new password fails validation (400)")
    void weakNewPasswordReturns400() throws Exception {
        Cookie authCookie = getAuthCookie(testUser);

        // Less than 8 characters
        ChangePasswordRequest shortReq = new ChangePasswordRequest("OldPassword123!", "Short1!");
        mockMvc.perform(patch("/api/auth/me/password")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortReq)))
                .andExpect(status().isBadRequest());

        // No numbers
        ChangePasswordRequest noNumReq = new ChangePasswordRequest("OldPassword123!", "NoNumbersAtAll!");
        mockMvc.perform(patch("/api/auth/me/password")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noNumReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C7-B01 & C7-B02: Password change succeeds and revokes existing refresh tokens")
    void changePasswordSuccessAndTokenRevocation() throws Exception {
        Cookie authCookie = getAuthCookie(testUser);

        // 1. Issue a refresh token prior to password change
        String rawRefreshToken = "test-raw-refresh-token-value-12345";
        String tokenHash = jwtService.hashToken(rawRefreshToken);
        RefreshToken tokenEntity = new RefreshToken(testUser, tokenHash, LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(tokenEntity);

        // 2. Perform password change
        ChangePasswordRequest req = new ChangePasswordRequest("OldPassword123!", "BrandNewPassword123!");
        mockMvc.perform(patch("/api/auth/me/password")
                        .cookie(authCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        // 3. Prior refresh token is now revoked and fails at /api/auth/refresh
        Cookie refreshCookie = new Cookie("refresh_token", rawRefreshToken);
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());

        // 4. Can login with new password
        LoginRequest loginWithNew = new LoginRequest("alice-pwd@test.com", "BrandNewPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWithNew)))
                .andExpect(status().isOk());

        // 5. Old password no longer works
        LoginRequest loginWithOld = new LoginRequest("alice-pwd@test.com", "OldPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWithOld)))
                .andExpect(status().isUnauthorized());
    }
}
