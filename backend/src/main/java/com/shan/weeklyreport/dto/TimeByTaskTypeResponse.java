package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.TaskType;

import java.math.BigDecimal;

/**
 * Time spent aggregated by task type (C6-T07).
 */
public record TimeByTaskTypeResponse(
        TaskType taskType,
        BigDecimal totalHours
) {
}
