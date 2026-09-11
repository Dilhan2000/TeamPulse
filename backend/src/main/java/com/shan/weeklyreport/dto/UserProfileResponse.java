package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;

/**
 * Response DTO for user profile (C1-T10).
 */
public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        AccountStatus status
) {}
