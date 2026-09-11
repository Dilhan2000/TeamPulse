package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.TaskPriority;
import com.shan.weeklyreport.common.TaskProgressStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TaskItemDto(
        Long id,

        @NotBlank(message = "Task name is required")
        @Size(max = 255, message = "Task name cannot exceed 255 characters")
        String taskName,

        @NotNull(message = "Task priority is required")
        TaskPriority priority,

        @Min(value = 0, message = "Planned percent must be between 0 and 100")
        @Max(value = 100, message = "Planned percent must be between 0 and 100")
        int plannedPercent,

        @Min(value = 0, message = "Actual percent must be between 0 and 100")
        @Max(value = 100, message = "Actual percent must be between 0 and 100")
        int actualPercent,

        @NotNull(message = "Task status is required")
        TaskProgressStatus status,

        @NotNull(message = "Time planned is required")
        @DecimalMin(value = "0", message = "Time planned must be >= 0")
        @DecimalMax(value = "168", message = "Time planned cannot exceed 168 hours")
        BigDecimal timePlannedHours,

        @NotNull(message = "Time spent is required")
        @DecimalMin(value = "0", message = "Time spent must be >= 0")
        @DecimalMax(value = "168", message = "Time spent cannot exceed 168 hours")
        BigDecimal timeSpentHours,

        @Size(max = 500, message = "Deliverable link/note cannot exceed 500 characters")
        String deliverable,

        int sortOrder
) {}
