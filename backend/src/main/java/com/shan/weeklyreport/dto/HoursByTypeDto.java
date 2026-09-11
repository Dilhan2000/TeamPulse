package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.TaskType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record HoursByTypeDto(
        @NotNull(message = "Task type is required")
        TaskType taskType,

        @NotNull(message = "Hours is required")
        @DecimalMin(value = "0", message = "Hours must be >= 0")
        @DecimalMax(value = "168", message = "Hours cannot exceed 168")
        BigDecimal hours
) {}
