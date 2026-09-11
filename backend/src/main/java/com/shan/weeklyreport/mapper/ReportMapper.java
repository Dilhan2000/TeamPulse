package com.shan.weeklyreport.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.domain.Project;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.ReportReview;
import com.shan.weeklyreport.domain.ReportVersion;
import com.shan.weeklyreport.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Static mapper for Report, Project, Version, and Review entities (no MapStruct).
 * Handles content snapshot serialization and deserialization (C3-T04).
 */
public final class ReportMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private ReportMapper() {}

    public static ReportResponse toResponse(Report report) {
        return toResponse(report, null);
    }

    public static ReportResponse toResponse(Report report, ReviewResponse latestReview) {
        if (report == null) {
            return null;
        }

        List<TaskItemDto> tasks = report.getTaskItems() != null
                ? report.getTaskItems().stream()
                .map(t -> new TaskItemDto(
                        t.getId(),
                        t.getTaskName(),
                        t.getPriority(),
                        t.getPlannedPercent(),
                        t.getActualPercent(),
                        t.getStatus(),
                        t.getTimePlannedHours(),
                        t.getTimeSpentHours(),
                        t.getDeliverable(),
                        t.getSortOrder()
                )).toList()
                : Collections.emptyList();

        List<NextWeekTaskDto> nextWeekTasks = report.getNextWeekTasks() != null
                ? report.getNextWeekTasks().stream()
                .map(n -> new NextWeekTaskDto(n.getId(), n.getDescription(), n.getSortOrder()))
                .toList()
                : Collections.emptyList();

        List<BlockerDto> blockers = report.getBlockers() != null
                ? report.getBlockers().stream()
                .map(b -> new BlockerDto(b.getId(), b.getDescription(), b.isKeyIssue(), b.isResolved(), b.getSortOrder()))
                .toList()
                : Collections.emptyList();

        List<AchievementDto> achievements = report.getAchievements() != null
                ? report.getAchievements().stream()
                .map(a -> new AchievementDto(a.getId(), a.getDescription(), a.isKeyAchievement(), a.getSortOrder()))
                .toList()
                : Collections.emptyList();

        List<HoursByTypeDto> hours = report.getHoursByType() != null
                ? report.getHoursByType().stream()
                .map(h -> new HoursByTypeDto(h.getTaskType(), h.getHours()))
                .toList()
                : Collections.emptyList();

        return new ReportResponse(
                report.getId(),
                report.getUser().getId(),
                report.getUser().getFullName(),
                report.getProject().getId(),
                report.getProject().getName(),
                report.getWeekStartDate(),
                report.getWeekEndDate(),
                report.getStatus(),
                report.getNotes(),
                report.getSubmittedAt(),
                report.getCreatedAt(),
                tasks,
                nextWeekTasks,
                blockers,
                achievements,
                hours,
                latestReview
        );
    }

    public static ReportSummaryResponse toSummaryResponse(Report report) {
        if (report == null) {
            return null;
        }
        return new ReportSummaryResponse(
                report.getId(),
                report.getWeekStartDate(),
                report.getWeekEndDate(),
                report.getProject().getId(),
                report.getProject().getName(),
                report.getStatus(),
                report.getSubmittedAt()
        );
    }

    public static ProjectSummaryResponse toProjectSummary(Project project) {
        if (project == null) {
            return null;
        }
        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.isActive()
        );
    }

    public static ReviewResponse toReviewResponse(ReportReview review) {
        if (review == null) {
            return null;
        }
        return new ReviewResponse(
                review.getId(),
                review.getVersion() != null ? review.getVersion().getVersionNumber() : 0,
                review.getAction(),
                review.getComment(),
                review.getReviewer() != null ? review.getReviewer().getFullName() : null,
                review.getReviewedAt()
        );
    }

    public static ReportVersionSummaryResponse toVersionSummary(ReportVersion version) {
        if (version == null) {
            return null;
        }
        return new ReportVersionSummaryResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getSubmittedAt()
        );
    }

    /**
     * Snapshot payload record for JSON serialization.
     */
    public record SnapshotPayload(
            Long projectId,
            String projectName,
            String notes,
            List<TaskItemDto> tasksCompleted,
            List<NextWeekTaskDto> tasksPlannedNextWeek,
            List<BlockerDto> blockers,
            List<AchievementDto> achievements,
            List<HoursByTypeDto> hoursByType
    ) {}

    /**
     * Serialize a Report's content into a JSON snapshot string (C3-T04).
     */
    public static String snapshotOf(Report report) {
        try {
            ReportResponse res = toResponse(report);
            SnapshotPayload payload = new SnapshotPayload(
                    res.projectId(),
                    res.projectName(),
                    res.notes(),
                    res.tasksCompleted(),
                    res.tasksPlannedNextWeek(),
                    res.blockers(),
                    res.achievements(),
                    res.hoursByType()
            );
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize report snapshot", e);
        }
    }

    /**
     * Deserialize a JSON snapshot string into a full ReportResponse (C3-T04).
     */
    public static ReportResponse deserializeSnapshot(String json,
                                                    Report report,
                                                    LocalDateTime submittedAt) {
        try {
            SnapshotPayload payload = OBJECT_MAPPER.readValue(json, SnapshotPayload.class);

            return new ReportResponse(
                    report.getId(),
                    report.getUser().getId(),
                    report.getUser().getFullName(),
                    payload.projectId(),
                    payload.projectName(),
                    report.getWeekStartDate(),
                    report.getWeekEndDate(),
                    ReportStatus.SUBMITTED,
                    payload.notes(),
                    submittedAt,
                    report.getCreatedAt(),
                    payload.tasksCompleted() != null ? payload.tasksCompleted() : Collections.emptyList(),
                    payload.tasksPlannedNextWeek() != null ? payload.tasksPlannedNextWeek() : Collections.emptyList(),
                    payload.blockers() != null ? payload.blockers() : Collections.emptyList(),
                    payload.achievements() != null ? payload.achievements() : Collections.emptyList(),
                    payload.hoursByType() != null ? payload.hoursByType() : Collections.emptyList(),
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize report snapshot", e);
        }
    }
}
