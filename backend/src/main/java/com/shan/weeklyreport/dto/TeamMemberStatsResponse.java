package com.shan.weeklyreport.dto;

import java.math.BigDecimal;

/**
 * Historical stats for a team member (C7-T01).
 */
public record TeamMemberStatsResponse(
        long totalReportsSubmitted,
        long approvedCount,
        long needsCorrectionSentBackCount,
        String currentWeekStatus,
        BigDecimal totalHoursLogged,
        long totalTasksCompleted
) {
}
