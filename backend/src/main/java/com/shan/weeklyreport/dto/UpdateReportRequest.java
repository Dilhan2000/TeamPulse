package com.shan.weeklyreport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateReportRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
        String notes,

        List<@Valid TaskItemDto> tasksCompleted,
        List<@Valid NextWeekTaskDto> tasksPlannedNextWeek,
        List<@Valid BlockerDto> blockers,
        List<@Valid AchievementDto> achievements,
        List<@Valid HoursByTypeDto> hoursByType
) {}
