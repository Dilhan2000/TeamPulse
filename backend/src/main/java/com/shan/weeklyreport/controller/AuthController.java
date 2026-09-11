package com.shan.weeklyreport.controller;

import com.shan.weeklyreport.dto.ChangePasswordRequest;
import com.shan.weeklyreport.dto.LoginRequest;
import com.shan.weeklyreport.dto.MessageResponse;
import com.shan.weeklyreport.dto.RegisterRequest;
import com.shan.weeklyreport.dto.UserProfileResponse;
import com.shan.weeklyreport.security.UserPrincipal;
import com.shan.weeklyreport.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST controller (C1-T11..T15).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        MessageResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserProfileResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        UserProfileResponse userProfile = authService.login(request, response);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserProfileResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        UserProfileResponse userProfile = authService.refresh(request, response);
        return ResponseEntity.ok(userProfile);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse userProfile = authService.getCurrentUser(principal);
        return ResponseEntity.ok(userProfile);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        MessageResponse response = authService.changePassword(principal, request);
        return ResponseEntity.ok(response);
    }
}
