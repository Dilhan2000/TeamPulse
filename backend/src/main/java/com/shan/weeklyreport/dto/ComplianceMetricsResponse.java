package com.shan.weeklyreport.dto;

/**
 * Compliance metrics breakdown for the current reporting week (C6-T03).
 */
public record ComplianceMetricsResponse(
        int submitted,
        int pending,
        int late,
        int totalActiveTeamMembers
) {
}
