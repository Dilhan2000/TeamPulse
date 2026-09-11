package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.SectionType;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.ReportAchievement;
import com.shan.weeklyreport.domain.ReportBlocker;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.SectionItemResponse;
import com.shan.weeklyreport.dto.SectionRowResponse;
import com.shan.weeklyreport.dto.TeamMemberOptionResponse;
import com.shan.weeklyreport.dto.TeamWeekStatusRow;
import com.shan.weeklyreport.exception.BadRequestException;
import com.shan.weeklyreport.repository.ReportRepository;
import com.shan.weeklyreport.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for manager team dashboard queries (C4-T02, C4-T04, C4-T05).
 */
@Service
@Transactional(readOnly = true)
public class TeamDashboardService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    public TeamDashboardService(UserRepository userRepository, ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
    }

    /**
     * Get team reporting status for a single week (C4-T02).
     * Synthesizes NOT_STARTED for active team members without reports.
     * Excludes NOT_STARTED rows when projectId filter is present.
     */
    public List<TeamWeekStatusRow> getTeamStatus(LocalDate weekStart, Long projectId, Long userId) {
        validateMonday(weekStart);

        List<User> activeMembers = userRepository.findByRoleAndStatusOrderByFullNameAsc(
                Role.TEAM_MEMBER, AccountStatus.ACTIVE);

        if (userId != null) {
            activeMembers = activeMembers.stream()
                    .filter(u -> u.getId().equals(userId))
                    .toList();
        }

        List<Report> reports = reportRepository.findByWeekStartDateAndUserRole(weekStart, Role.TEAM_MEMBER);
        Map<Long, Report> reportByUser = reports.stream()
                .collect(Collectors.toMap(r -> r.getUser().getId(), r -> r, (r1, r2) -> r1));

        List<TeamWeekStatusRow> result = new ArrayList<>();
        for (User member : activeMembers) {
            Report report = reportByUser.get(member.getId());
            if (report != null) {
                if (projectId != null && !report.getProject().getId().equals(projectId)) {
                    continue; // Excluded by project filter
                }
                result.add(new TeamWeekStatusRow(
                        member.getId(),
                        member.getFullName(),
                        report.getId(),
                        report.getProject().getId(),
                        report.getProject().getName(),
                        report.getStatus().name(),
                        report.getSubmittedAt()
                ));
            } else {
                // Assumption 4: project filter excludes NOT_STARTED rows
                if (projectId == null) {
                    result.add(new TeamWeekStatusRow(
                            member.getId(),
                            member.getFullName(),
                            null,
                            null,
                            null,
                            "NOT_STARTED",
                            null
                    ));
                }
            }
        }

        return result;
    }

    /**
     * Get list of active team members for manager filter dropdown (C4-T04).
     */
    public List<TeamMemberOptionResponse> getActiveTeamMembers() {
        return userRepository.findByRoleAndStatusOrderByFullNameAsc(Role.TEAM_MEMBER, AccountStatus.ACTIVE)
                .stream()
                .map(u -> new TeamMemberOptionResponse(u.getId(), u.getFullName()))
                .toList();
    }

    /**
     * Side-by-side view across team for blockers or achievements (C4-T05).
     * Excludes DRAFT and NOT_STARTED reports (only SUBMITTED, NEEDS_CORRECTION, APPROVED).
     */
    public List<SectionRowResponse> getSectionAcrossTeam(LocalDate weekStart, SectionType section, Long projectId, Long userId) {
        validateMonday(weekStart);
        if (section == null) {
            throw new BadRequestException("Section parameter is required (BLOCKERS or ACHIEVEMENTS)");
        }

        List<Report> reports = reportRepository.findByWeekStartDateAndUserRole(weekStart, Role.TEAM_MEMBER);

        return reports.stream()
                .filter(r -> r.getStatus() != ReportStatus.DRAFT)
                .filter(r -> projectId == null || r.getProject().getId().equals(projectId))
                .filter(r -> userId == null || r.getUser().getId().equals(userId))
                .sorted(Comparator.comparing(r -> r.getUser().getFullName()))
                .map(r -> {
                    List<SectionItemResponse> items;
                    if (section == SectionType.BLOCKERS) {
                        items = r.getBlockers().stream()
                                .sorted(Comparator.comparingInt(ReportBlocker::getSortOrder))
                                .map(b -> new SectionItemResponse(b.getDescription(), b.isKeyIssue(), b.isResolved()))
                                .toList();
                    } else {
                        items = r.getAchievements().stream()
                                .sorted(Comparator.comparingInt(ReportAchievement::getSortOrder))
                                .map(a -> new SectionItemResponse(a.getDescription(), a.isKeyAchievement(), null))
                                .toList();
                    }
                    return new SectionRowResponse(
                            r.getUser().getId(),
                            r.getUser().getFullName(),
                            r.getProject().getName(),
                            r.getId(),
                            r.getStatus(),
                            items
                    );
                })
                .toList();
    }

    private void validateMonday(LocalDate date) {
        if (date == null || date.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BadRequestException("Week start date must be a Monday");
        }
    }
}
