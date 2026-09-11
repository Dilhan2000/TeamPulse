package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.TeamMemberProfileResponse;
import com.shan.weeklyreport.dto.TeamMemberStatsResponse;
import com.shan.weeklyreport.exception.ResourceNotFoundException;
import com.shan.weeklyreport.repository.ReportRepository;
import com.shan.weeklyreport.repository.ReportReviewRepository;
import com.shan.weeklyreport.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Service for team member profile queries and stats aggregation (C7-T01, C7-T02).
 */
@Service
@Transactional(readOnly = true)
public class TeamMemberProfileService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ReportReviewRepository reportReviewRepository;
    private Clock clock;

    public TeamMemberProfileService(
            UserRepository userRepository,
            ReportRepository reportRepository,
            ReportReviewRepository reportReviewRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.reportReviewRepository = reportReviewRepository;
        this.clock = clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Retrieve full profile and historical stats for a team member (C7-T01, C7-T02).
     *
     * @param userId ID of the team member
     * @return TeamMemberProfileResponse with profile metadata and stats
     */
    public TeamMemberProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Team member not found with id: " + userId));

        if (user.getRole() != Role.TEAM_MEMBER) {
            throw new ResourceNotFoundException("User " + userId + " is not a team member.");
        }

        long totalReportsSubmitted = reportRepository.countSubmittedByUserId(userId);
        long approvedCount = reportRepository.countApprovedByUserId(userId);
        long needsCorrectionSentBackCount = reportReviewRepository.countChangesRequestedByReportUserId(userId);

        LocalDate today = LocalDate.now(clock);
        LocalDate thisWeekMonday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        Optional<Report> currentWeekReport = reportRepository.findByUserIdAndWeekStartDate(userId, thisWeekMonday);
        String currentWeekStatus = currentWeekReport.map(r -> r.getStatus().name()).orElse("NOT_STARTED");

        BigDecimal totalHoursLogged = reportRepository.sumHoursLoggedByUserId(userId);
        long totalTasksCompleted = reportRepository.countCompletedTasksByUserId(userId);

        TeamMemberStatsResponse stats = new TeamMemberStatsResponse(
                totalReportsSubmitted,
                approvedCount,
                needsCorrectionSentBackCount,
                currentWeekStatus,
                totalHoursLogged != null ? totalHoursLogged : BigDecimal.ZERO,
                totalTasksCompleted
        );

        return new TeamMemberProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getApprovedAt(),
                stats
        );
    }
}
