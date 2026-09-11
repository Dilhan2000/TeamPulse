package com.shan.weeklyreport.dto;

import java.math.BigDecimal;

/**
 * Workload hours aggregated by project (C6-T06).
 */
public record WorkloadByProjectResponse(
        Long projectId,
        String projectName,
        BigDecimal totalHours
) {
}
