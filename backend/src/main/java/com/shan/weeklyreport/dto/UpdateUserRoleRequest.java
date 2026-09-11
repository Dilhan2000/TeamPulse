package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating user role (C1-T10).
 */
public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}
