package com.shan.weeklyreport.dto;

import java.time.LocalDateTime;

/**
 * Row in the single-week team status grid (C4-T02).
 * Shows active team members and their report status for a selected week.
 */
public record TeamWeekStatusRow(
        Long userId,
        String fullName,
        Long reportId,
        Long projectId,
        String projectName,
        String status,
        LocalDateTime submittedAt
) {
}
