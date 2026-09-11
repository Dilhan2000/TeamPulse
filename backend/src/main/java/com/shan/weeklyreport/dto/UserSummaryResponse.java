package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;

import java.time.LocalDateTime;

/**
 * Summary DTO for admin user listings (C1-T10).
 */
public record UserSummaryResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt
) {}
