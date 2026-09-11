package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.common.TaskType;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.ReportReview;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.repository.ReportRepository;
import com.shan.weeklyreport.repository.ReportReviewRepository;
import com.shan.weeklyreport.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for manager executive dashboard metrics, charts, and activity feed (C6-T03 to C6-T08).
 */
@Service
@Transactional(readOnly = true)
public class DashboardMetricsService {

    private Clock clock;
    private final TeamDashboardService teamDashboardService;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ReportReviewRepository reportReviewRepository;

    public DashboardMetricsService(
            Clock clock,
            TeamDashboardService teamDashboardService,
            UserRepository userRepository,
            ReportRepository reportRepository,
            ReportReviewRepository reportReviewRepository) {
        this.clock = clock;
        this.teamDashboardService = teamDashboardService;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.reportReviewRepository = reportReviewRepository;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Compute current-week summary metrics and all-time backlogs (C6-T03).
     */
    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now(clock);
        DayOfWeek day = today.getDayOfWeek();
        LocalDate thisWeekMonday = today.minusDays(day.getValue() - 1);

        List<TeamWeekStatusRow> rows = teamDashboardService.getTeamStatus(thisWeekMonday, null, null);

        LocalDateTime deadline = thisWeekMonday.plusDays(4).atTime(23, 59, 59);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean isPastDeadline = now.isAfter(deadline);

        int submitted = 0;
        int pending = 0;
        int late = 0;

        for (TeamWeekStatusRow row : rows) {
            String status = row.status();
            if ("SUBMITTED".equals(status) || "NEEDS_CORRECTION".equals(status) || "APPROVED".equals(status)) {
                submitted++;
            } else {
                if (isPastDeadline) {
                    late++;
                } else {
                    pending++;
                }
            }
        }

        int totalActive = rows.size();
        long needsCorrectionCount = reportRepository.countNeedsCorrectionReports();
        long openBlockersCount = reportRepository.countOpenBlockers();

        ComplianceMetricsResponse compliance = new ComplianceMetricsResponse(submitted, pending, late, totalActive);
        return new DashboardSummaryResponse(submitted, compliance, needsCorrectionCount, openBlockersCount);
    }

    /**
     * Tasks-completed trend line data over multi-week period (C6-T04).
     */
    public List<TrendPointResponse> getTasksCompletedTrend(int weeks, Long userId) {
        int window = weeks <= 0 ? 8 : weeks;

        LocalDate today = LocalDate.now(clock);
        LocalDate thisWeekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate latestReportMonday = reportRepository.findLatestSubmittedReportWeek().orElse(thisWeekMonday);
        LocalDate endMonday = latestReportMonday.isAfter(thisWeekMonday) ? latestReportMonday : thisWeekMonday;

        List<TrendPointResponse> points = new ArrayList<>(window);
        for (int i = window - 1; i >= 0; i--) {
            LocalDate monday = endMonday.minusWeeks(i);
            long count = reportRepository.countCompletedTasksByWeek(monday, userId);
            points.add(new TrendPointResponse(monday, count));
        }

        return points;
    }

    /**
     * Status distribution per active team member in date range (C6-T05).
     */
    public List<StatusByMemberResponse> getStatusByMember(LocalDate from, LocalDate to) {
        LocalDate[] resolved = resolveDateRange(from, to);
        LocalDate rangeFrom = resolved[0];
        LocalDate rangeTo = resolved[1];

        List<User> activeMembers = userRepository.findByRoleAndStatusOrderByFullNameAsc(
                Role.TEAM_MEMBER, AccountStatus.ACTIVE);
        List<Report> reports = reportRepository.findTeamReportsInRange(rangeFrom, rangeTo);

        Map<Long, List<Report>> reportsByUser = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId()));

        return activeMembers.stream().map(u -> {
            List<Report> userReports = reportsByUser.getOrDefault(u.getId(), Collections.emptyList());
            long draft = userReports.stream().filter(r -> r.getStatus() == ReportStatus.DRAFT).count();
            long submitted = userReports.stream().filter(r -> r.getStatus() == ReportStatus.SUBMITTED).count();
            long needsCorrection = userReports.stream().filter(r -> r.getStatus() == ReportStatus.NEEDS_CORRECTION).count();
            long approved = userReports.stream().filter(r -> r.getStatus() == ReportStatus.APPROVED).count();
            return new StatusByMemberResponse(u.getId(), u.getFullName(), draft, submitted, needsCorrection, approved);
        }).toList();
    }

    /**
     * Aggregated hours spent per project in date range (C6-T06).
     */
    public List<WorkloadByProjectResponse> getWorkloadByProject(LocalDate from, LocalDate to) {
        LocalDate[] resolved = resolveDateRange(from, to);
        List<Object[]> raw = reportRepository.findWorkloadByProjectRaw(resolved[0], resolved[1]);

        return raw.stream().map(row -> new WorkloadByProjectResponse(
                (Long) row[0],
                (String) row[1],
                row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO
        )).toList();
    }

    /**
     * Time spent grouped by task type, guaranteed to include all 5 enum values (C6-T07).
     */
    public List<TimeByTaskTypeResponse> getTimeByTaskType(LocalDate from, LocalDate to) {
        LocalDate[] resolved = resolveDateRange(from, to);
        List<Object[]> raw = reportRepository.findTimeByTaskTypeRaw(resolved[0], resolved[1]);

        Map<TaskType, BigDecimal> hourMap = new EnumMap<>(TaskType.class);
        for (TaskType type : TaskType.values()) {
            hourMap.put(type, BigDecimal.ZERO);
        }
        for (Object[] row : raw) {
            TaskType type = (TaskType) row[0];
            BigDecimal hrs = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            hourMap.put(type, hrs);
        }

        return Arrays.stream(TaskType.values())
                .map(type -> new TimeByTaskTypeResponse(type, hourMap.get(type)))
                .toList();
    }

    /**
     * Combined activity feed for submissions and reviews (C6-T08).
     */
    public List<ActivityFeedItemResponse> getActivityFeed(int limit) {
        int max = limit <= 0 ? 20 : limit;

        List<Report> subs = reportRepository.findRecentSubmissions(PageRequest.of(0, max));
        List<ReportReview> revs = reportReviewRepository.findRecentReviews(PageRequest.of(0, max));

        List<ActivityFeedItemResponse> items = new ArrayList<>();
        for (Report r : subs) {
            items.add(new ActivityFeedItemResponse(
                    "SUBMISSION",
                    r.getUser().getFullName(),
                    null,
                    null,
                    r.getWeekStartDate(),
                    r.getSubmittedAt(),
                    r.getId()
            ));
        }
        for (ReportReview rr : revs) {
            items.add(new ActivityFeedItemResponse(
                    "REVIEW",
                    rr.getReviewer().getFullName(),
                    rr.getReport().getUser().getFullName(),
                    rr.getAction(),
                    rr.getReport().getWeekStartDate(),
                    rr.getReviewedAt(),
                    rr.getReport().getId()
            ));
        }

        return items.stream()
                .sorted(Comparator.comparing(ActivityFeedItemResponse::timestamp).reversed())
                .limit(max)
                .toList();
    }

    private LocalDate[] resolveDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return new LocalDate[]{from, to};
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate thisWeekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate latestReportMonday = reportRepository.findLatestSubmittedReportWeek().orElse(thisWeekMonday);
        LocalDate referenceMonday = latestReportMonday.isAfter(thisWeekMonday) ? latestReportMonday : thisWeekMonday;

        LocalDate resolvedTo = (to != null) ? to : referenceMonday;
        LocalDate resolvedFrom = (from != null) ? from : referenceMonday.minusWeeks(7);
        return new LocalDate[]{resolvedFrom, resolvedTo};
    }
}
