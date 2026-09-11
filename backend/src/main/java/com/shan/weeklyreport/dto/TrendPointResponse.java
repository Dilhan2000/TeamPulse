package com.shan.weeklyreport.dto;

import java.time.LocalDate;

/**
 * Data point for tasks-completed trend chart (C6-T04).
 */
public record TrendPointResponse(
        LocalDate weekStartDate,
        long completedCount
) {
}
