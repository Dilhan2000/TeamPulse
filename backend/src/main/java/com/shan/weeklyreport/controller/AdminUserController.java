package com.shan.weeklyreport.controller;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.dto.ApproveUserRequest;
import com.shan.weeklyreport.dto.UpdateUserRoleRequest;
import com.shan.weeklyreport.dto.UpdateUserStatusRequest;
import com.shan.weeklyreport.dto.UserSummaryResponse;
import com.shan.weeklyreport.security.UserPrincipal;
import com.shan.weeklyreport.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin user management controller (C1-T16, C1-T17).
 * Protected by PreAuthorize("hasRole('ADMIN')").
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<UserSummaryResponse>> getPendingUsers(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<UserSummaryResponse> pending = adminUserService.getPendingUsers(pageable);
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<UserSummaryResponse> approveUser(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveUserRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {
        UserSummaryResponse response = adminUserService.approveUser(id, request, adminPrincipal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<UserSummaryResponse> rejectUser(@PathVariable Long id) {
        UserSummaryResponse response = adminUserService.rejectUser(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<UserSummaryResponse>> getUsers(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<UserSummaryResponse> users = adminUserService.getUsers(status, role, search, pageable);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserSummaryResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal) {
        UserSummaryResponse response = adminUserService.updateUserStatus(id, request, adminPrincipal);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserSummaryResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        UserSummaryResponse response = adminUserService.updateUserRole(id, request);
        return ResponseEntity.ok(response);
    }
}
