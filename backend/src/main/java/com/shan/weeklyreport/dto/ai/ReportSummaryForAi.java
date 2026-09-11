package com.shan.weeklyreport.dto.ai;

import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.TaskProgressStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Compact LLM-friendly report summary for the AI tool query results (SRS08 C8-T02).
 */
public record ReportSummaryForAi(
        String memberName,
        String projectName,
        LocalDate weekStartDate,
        ReportStatus status,
        List<TaskItemForAi> tasksCompleted,
        List<BlockerForAi> blockers,
        List<AchievementForAi> achievements
) {
    public record TaskItemForAi(String taskName, TaskProgressStatus status) {}
    public record BlockerForAi(String description, boolean resolved) {}
    public record AchievementForAi(String description) {}
}
