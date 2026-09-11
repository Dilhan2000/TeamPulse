package com.shan.weeklyreport.dto;

/**
 * Top-level dashboard summary metrics response (C6-T03).
 */
public record DashboardSummaryResponse(
        int totalSubmittedThisWeek,
        ComplianceMetricsResponse compliance,
        long needsCorrectionCount,
        long openBlockersCount
) {
}
