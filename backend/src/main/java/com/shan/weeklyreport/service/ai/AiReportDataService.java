package com.shan.weeklyreport.service.ai;

import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.dto.ai.ReportSummaryForAi;
import com.shan.weeklyreport.dto.ai.ReportSummaryForAi.AchievementForAi;
import com.shan.weeklyreport.dto.ai.ReportSummaryForAi.BlockerForAi;
import com.shan.weeklyreport.dto.ai.ReportSummaryForAi.TaskItemForAi;
import com.shan.weeklyreport.repository.ReportRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service providing report data querying for the AI tool (SRS08 C8-T02).
 * Enforces strict privacy rules: DRAFT reports and non-TEAM_MEMBER reports are completely excluded.
 */
@Service
public class AiReportDataService {

    private static final Logger log = LoggerFactory.getLogger(AiReportDataService.class);
    private static final int MAX_RESULTS_CAP = 50;

    private final ReportRepository reportRepository;

    public AiReportDataService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public List<ReportSummaryForAi> queryReports(
            LocalDate weekStartDate,
            LocalDate weekStartFrom,
            LocalDate weekStartTo,
            Long projectId,
            Long userId) {

        log.debug("AI querying reports with filters: weekStartDate={}, weekStartFrom={}, weekStartTo={}, projectId={}, userId={}",
                weekStartDate, weekStartFrom, weekStartTo, projectId, userId);

        Specification<Report> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. MUST be authored by a TEAM_MEMBER (never a MANAGER's own report)
            predicates.add(cb.equal(root.get("user").get("role"), Role.TEAM_MEMBER));

            // 2. MUST NOT be DRAFT (DRAFT reports excluded entirely per Assumption 4)
            predicates.add(cb.notEqual(root.get("status"), ReportStatus.DRAFT));

            // 3. Optional filter: specific weekStartDate
            if (weekStartDate != null) {
                predicates.add(cb.equal(root.get("weekStartDate"), weekStartDate));
            }

            // 4. Optional filter: date range from
            if (weekStartFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("weekStartDate"), weekStartFrom));
            }

            // 5. Optional filter: date range to
            if (weekStartTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("weekStartDate"), weekStartTo));
            }

            // 6. Optional filter: project
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }

            // 7. Optional filter: user
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(0, MAX_RESULTS_CAP,
                Sort.by(Sort.Direction.DESC, "weekStartDate")
                        .and(Sort.by(Sort.Direction.ASC, "user.fullName")));

        List<Report> reports = reportRepository.findAll(spec, pageRequest).getContent();

        return reports.stream().map(this::mapToSummaryForAi).toList();
    }

    private ReportSummaryForAi mapToSummaryForAi(Report report) {
        List<TaskItemForAi> tasks = report.getTaskItems() != null
                ? report.getTaskItems().stream()
                .map(t -> new TaskItemForAi(t.getTaskName(), t.getStatus()))
                .toList()
                : List.of();

        List<BlockerForAi> blockers = report.getBlockers() != null
                ? report.getBlockers().stream()
                .map(b -> new BlockerForAi(b.getDescription(), b.isResolved()))
                .toList()
                : List.of();

        List<AchievementForAi> achievements = report.getAchievements() != null
                ? report.getAchievements().stream()
                .map(a -> new AchievementForAi(a.getDescription()))
                .toList()
                : List.of();

        return new ReportSummaryForAi(
                report.getUser().getFullName(),
                report.getProject().getName(),
                report.getWeekStartDate(),
                report.getStatus(),
                tasks,
                blockers,
                achievements
        );
    }
}
