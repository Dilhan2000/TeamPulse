package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.ReportStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(
        Long id,
        Long userId,
        String userName,
        Long projectId,
        String projectName,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        ReportStatus status,
        String notes,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        List<TaskItemDto> tasksCompleted,
        List<NextWeekTaskDto> tasksPlannedNextWeek,
        List<BlockerDto> blockers,
        List<AchievementDto> achievements,
        List<HoursByTypeDto> hoursByType,
        ReviewResponse latestReview
) {
    public ReportResponse(
            Long id,
            Long userId,
            String userName,
            Long projectId,
            String projectName,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            ReportStatus status,
            String notes,
            LocalDateTime submittedAt,
            LocalDateTime createdAt,
            List<TaskItemDto> tasksCompleted,
            List<NextWeekTaskDto> tasksPlannedNextWeek,
            List<BlockerDto> blockers,
            List<AchievementDto> achievements,
            List<HoursByTypeDto> hoursByType
    ) {
        this(id, userId, userName, projectId, projectName, weekStartDate, weekEndDate, status, notes,
                submittedAt, createdAt, tasksCompleted, tasksPlannedNextWeek, blockers, achievements, hoursByType, null);
    }
}
