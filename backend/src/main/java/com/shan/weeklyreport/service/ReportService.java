package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.TaskType;
import com.shan.weeklyreport.domain.*;
import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.exception.ConflictException;
import com.shan.weeklyreport.exception.ResourceNotFoundException;
import com.shan.weeklyreport.mapper.ReportMapper;
import com.shan.weeklyreport.repository.ProjectRepository;
import com.shan.weeklyreport.repository.ReportRepository;
import com.shan.weeklyreport.repository.ReportReviewRepository;
import com.shan.weeklyreport.repository.ReportVersionRepository;
import com.shan.weeklyreport.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for personal weekly report operations (C2-T08, C2-T09, C2-T10, C2-T12, C2-T14, C3-T05, C3-T06, C3-T12).
 * Strictly enforces user ownership and data isolation.
 */
@Service
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReportReviewRepository reportReviewRepository;

    public ReportService(ReportRepository reportRepository,
                         ProjectRepository projectRepository,
                         UserRepository userRepository,
                         ReportVersionRepository reportVersionRepository,
                         ReportReviewRepository reportReviewRepository) {
        this.reportRepository = reportRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.reportVersionRepository = reportVersionRepository;
        this.reportReviewRepository = reportReviewRepository;
    }

    /**
     * Create a new draft report (C2-T08).
     */
    public ReportResponse createDraft(Long userId, CreateReportRequest request) {
        if (request.weekStartDate().getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("Week start date must be a Monday");
        }

        if (reportRepository.existsByUserIdAndWeekStartDate(userId, request.weekStartDate())) {
            throw new ConflictException("A report already exists for this week");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.isActive()) {
            throw new IllegalArgumentException("Project must be active");
        }

        LocalDate weekEndDate = request.weekStartDate().plusDays(6);
        Report report = new Report(user, project, request.weekStartDate(), weekEndDate);
        report.setStatus(ReportStatus.DRAFT);

        Report saved = reportRepository.save(report);
        return ReportMapper.toResponse(saved);
    }

    /**
     * Update an existing editable report (C2-T09, C3-T05).
     * Guard widened in C3-T05 to permit both DRAFT and NEEDS_CORRECTION.
     */
    public ReportResponse updateDraft(Long userId, Long reportId, UpdateReportRequest request) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new ConflictException("Only draft or needs-correction reports can be edited");
        }

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.isActive()) {
            throw new IllegalArgumentException("Project must be active");
        }

        report.setProject(project);
        report.setNotes(request.notes());

        // Validate key issue mutual exclusion
        if (request.blockers() != null) {
            long keyIssueCount = request.blockers().stream().filter(BlockerDto::isKeyIssue).count();
            if (keyIssueCount > 1) {
                throw new IllegalArgumentException("At most one blocker can be designated as the key issue");
            }
        }

        // Validate key achievement mutual exclusion
        if (request.achievements() != null) {
            long keyAchievementCount = request.achievements().stream().filter(AchievementDto::isKeyAchievement).count();
            if (keyAchievementCount > 1) {
                throw new IllegalArgumentException("At most one achievement can be designated as the key achievement");
            }
        }

        // Update task items
        report.getTaskItems().clear();
        if (request.tasksCompleted() != null) {
            int order = 0;
            for (TaskItemDto itemDto : request.tasksCompleted()) {
                ReportTaskItem item = new ReportTaskItem();
                item.setReport(report);
                item.setTaskName(itemDto.taskName());
                item.setPriority(itemDto.priority());
                item.setPlannedPercent(itemDto.plannedPercent());
                item.setActualPercent(itemDto.actualPercent());
                item.setStatus(itemDto.status());
                item.setTimePlannedHours(itemDto.timePlannedHours());
                item.setTimeSpentHours(itemDto.timeSpentHours());
                item.setDeliverable(itemDto.deliverable());
                item.setSortOrder(itemDto.sortOrder() != 0 ? itemDto.sortOrder() : order++);
                report.getTaskItems().add(item);
            }
        }

        // Update next week tasks
        report.getNextWeekTasks().clear();
        if (request.tasksPlannedNextWeek() != null) {
            int order = 0;
            for (NextWeekTaskDto nextDto : request.tasksPlannedNextWeek()) {
                ReportNextWeekTask nextTask = new ReportNextWeekTask();
                nextTask.setReport(report);
                nextTask.setDescription(nextDto.description());
                nextTask.setSortOrder(nextDto.sortOrder() != 0 ? nextDto.sortOrder() : order++);
                report.getNextWeekTasks().add(nextTask);
            }
        }

        // Update blockers
        report.getBlockers().clear();
        if (request.blockers() != null) {
            int order = 0;
            for (BlockerDto blockerDto : request.blockers()) {
                ReportBlocker blocker = new ReportBlocker();
                blocker.setReport(report);
                blocker.setDescription(blockerDto.description());
                blocker.setKeyIssue(blockerDto.isKeyIssue());
                blocker.setResolved(blockerDto.resolved());
                blocker.setSortOrder(blockerDto.sortOrder() != 0 ? blockerDto.sortOrder() : order++);
                report.getBlockers().add(blocker);
            }
        }

        // Update achievements
        report.getAchievements().clear();
        if (request.achievements() != null) {
            int order = 0;
            for (AchievementDto achDto : request.achievements()) {
                ReportAchievement ach = new ReportAchievement();
                ach.setReport(report);
                ach.setDescription(achDto.description());
                ach.setKeyAchievement(achDto.isKeyAchievement());
                ach.setSortOrder(achDto.sortOrder() != 0 ? achDto.sortOrder() : order++);
                report.getAchievements().add(ach);
            }
        }

        // Update hours by type (in-place update to prevent duplicate key collision with uq_hours_by_type)
        Map<TaskType, BigDecimal> requestedHoursMap = new HashMap<>();
        if (request.hoursByType() != null) {
            for (HoursByTypeDto hoursDto : request.hoursByType()) {
                if (hoursDto.taskType() != null && hoursDto.hours() != null && hoursDto.hours().compareTo(BigDecimal.ZERO) > 0) {
                    requestedHoursMap.put(hoursDto.taskType(), hoursDto.hours());
                }
            }
        }

        report.getHoursByType().removeIf(h -> !requestedHoursMap.containsKey(h.getTaskType()));

        for (Map.Entry<TaskType, BigDecimal> entry : requestedHoursMap.entrySet()) {
            Optional<ReportHoursByType> existing = report.getHoursByType().stream()
                    .filter(h -> h.getTaskType() == entry.getKey())
                    .findFirst();
            if (existing.isPresent()) {
                existing.get().setHours(entry.getValue());
            } else {
                report.getHoursByType().add(new ReportHoursByType(report, entry.getKey(), entry.getValue()));
            }
        }

        Report saved = reportRepository.save(report);
        ReviewResponse latestReview = reportReviewRepository
                .findTopByReportIdOrderByIdDesc(reportId)
                .map(ReportMapper::toReviewResponse)
                .orElse(null);

        return ReportMapper.toResponse(saved, latestReview);
    }

    /**
     * Submit/resubmit a report with version snapshotting (C2-T10, C3-T06).
     */
    public ReportResponse submit(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new ConflictException("Only draft or needs-correction reports can be submitted");
        }

        int nextVersionNumber = reportVersionRepository
                .findTopByReportIdOrderByVersionNumberDesc(reportId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        LocalDateTime now = LocalDateTime.now();
        String snapshot = ReportMapper.snapshotOf(report);
        ReportVersion version = new ReportVersion(report, nextVersionNumber, snapshot, now);
        reportVersionRepository.save(version);

        report.setStatus(ReportStatus.SUBMITTED);
        report.setSubmittedAt(now);

        Report saved = reportRepository.save(report);

        ReviewResponse latestReview = reportReviewRepository
                .findTopByReportIdOrderByIdDesc(reportId)
                .map(ReportMapper::toReviewResponse)
                .orElse(null);

        return ReportMapper.toResponse(saved, latestReview);
    }

    /**
     * Get a report by ID strictly scoped to user (C2-T12, C2-T14, C3-T13).
     */
    @Transactional(readOnly = true)
    public ReportResponse getReport(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        ReviewResponse latestReview = reportReviewRepository
                .findTopByReportIdOrderByIdDesc(reportId)
                .map(ReportMapper::toReviewResponse)
                .orElse(null);

        return ReportMapper.toResponse(report, latestReview);
    }

    /**
     * List current user's reports with optional filters and pagination (C2-T12).
     */
    @Transactional(readOnly = true)
    public Page<ReportSummaryResponse> getMyReports(Long userId,
                                                    ReportStatus status,
                                                    LocalDate weekStartFrom,
                                                    LocalDate weekStartTo,
                                                    Pageable pageable) {
        Specification<Report> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (weekStartFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("weekStartDate"), weekStartFrom));
            }
            if (weekStartTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("weekStartDate"), weekStartTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return reportRepository.findAll(spec, pageable)
                .map(ReportMapper::toSummaryResponse);
    }

    /**
     * Get version list for a user's own report (C3-T12).
     */
    @Transactional(readOnly = true)
    public List<ReportVersionSummaryResponse> getVersions(Long userId, Long reportId) {
        reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        return reportVersionRepository.findByReportIdOrderByVersionNumberDesc(reportId)
                .stream()
                .map(ReportMapper::toVersionSummary)
                .toList();
    }

    /**
     * Get historical snapshot for a user's own report version (C3-T12).
     */
    @Transactional(readOnly = true)
    public ReportResponse getVersion(Long userId, Long reportId, Long versionId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        ReportVersion version = reportVersionRepository.findByIdAndReportId(versionId, reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));

        return ReportMapper.deserializeSnapshot(version.getContentSnapshot(), report, version.getSubmittedAt());
    }

    /**
     * Get review history for a user's own report (C3-T12).
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(Long userId, Long reportId) {
        reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        return reportReviewRepository.findByReportIdOrderByIdDesc(reportId)
                .stream()
                .map(ReportMapper::toReviewResponse)
                .toList();
    }
}
