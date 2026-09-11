package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.AccountStatus;

import java.time.LocalDateTime;

/**
 * Team member profile response for manager view (C7-T02).
 */
public record TeamMemberProfileResponse(
        Long id,
        String fullName,
        String email,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        TeamMemberStatsResponse stats
) {
}
