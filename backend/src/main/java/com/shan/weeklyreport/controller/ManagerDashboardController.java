package com.shan.weeklyreport.controller;

import com.shan.weeklyreport.common.SectionType;
import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.service.DashboardMetricsService;
import com.shan.weeklyreport.service.TeamDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for manager team dashboard views, metrics, charts, and activity feed (C4-T03, C4-T04, C4-T06, C6-T01, C6-T03 to C6-T08).
 */
@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerDashboardController {

    private final TeamDashboardService teamDashboardService;
    private final DashboardMetricsService dashboardMetricsService;
    private final com.shan.weeklyreport.service.TeamMemberProfileService teamMemberProfileService;

    public ManagerDashboardController(
            TeamDashboardService teamDashboardService,
            DashboardMetricsService dashboardMetricsService,
            com.shan.weeklyreport.service.TeamMemberProfileService teamMemberProfileService) {
        this.teamDashboardService = teamDashboardService;
        this.dashboardMetricsService = dashboardMetricsService;
        this.teamMemberProfileService = teamMemberProfileService;
    }

    /**
     * Get single-week team status grid (C4-T03).
     */
    @GetMapping("/dashboard/team-status")
    public ResponseEntity<List<TeamWeekStatusRow>> getTeamStatus(
            @RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        List<TeamWeekStatusRow> result = teamDashboardService.getTeamStatus(weekStart, projectId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get active team members for manager dropdowns (C4-T04).
     */
    @GetMapping("/team-members")
    public ResponseEntity<List<TeamMemberOptionResponse>> getTeamMembers() {
        List<TeamMemberOptionResponse> result = teamDashboardService.getActiveTeamMembers();
        return ResponseEntity.ok(result);
    }

    /**
     * Get team member profile and historical statistics (C7-T02).
     */
    @GetMapping("/team-members/{userId}")
    public ResponseEntity<TeamMemberProfileResponse> getTeamMemberProfile(@org.springframework.web.bind.annotation.PathVariable Long userId) {
        TeamMemberProfileResponse profile = teamMemberProfileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Side-by-side section view (Blockers or Achievements) across team (C4-T06).
     */
    @GetMapping("/dashboard/section")
    public ResponseEntity<List<SectionRowResponse>> getSectionAcrossTeam(
            @RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam("section") SectionType section,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        List<SectionRowResponse> result = teamDashboardService.getSectionAcrossTeam(weekStart, section, projectId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Executive KPI summary metrics for current week (C6-T03).
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        DashboardSummaryResponse response = dashboardMetricsService.getSummary();
        return ResponseEntity.ok(response);
    }

    /**
     * Chart 1: Tasks-completed multi-week trend (C6-T04).
     */
    @GetMapping("/dashboard/charts/tasks-completed-trend")
    public ResponseEntity<List<TrendPointResponse>> getTasksCompletedTrend(
            @RequestParam(value = "weeks", defaultValue = "8") int weeks,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        List<TrendPointResponse> response = dashboardMetricsService.getTasksCompletedTrend(weeks, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Chart 2: Report status distribution per team member (C6-T05).
     */
    @GetMapping("/dashboard/charts/status-by-member")
    public ResponseEntity<List<StatusByMemberResponse>> getStatusByMember(
            @RequestParam(value = "weekStartFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartFrom,
            @RequestParam(value = "weekStartTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartTo
    ) {
        List<StatusByMemberResponse> response = dashboardMetricsService.getStatusByMember(weekStartFrom, weekStartTo);
        return ResponseEntity.ok(response);
    }

    /**
     * Chart 3: Workload hours spent grouped by project (C6-T06).
     */
    @GetMapping("/dashboard/charts/workload-by-project")
    public ResponseEntity<List<WorkloadByProjectResponse>> getWorkloadByProject(
            @RequestParam(value = "weekStartFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartFrom,
            @RequestParam(value = "weekStartTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartTo
    ) {
        List<WorkloadByProjectResponse> response = dashboardMetricsService.getWorkloadByProject(weekStartFrom, weekStartTo);
        return ResponseEntity.ok(response);
    }

    /**
     * Chart 4: Time spent grouped by task type (C6-T07).
     */
    @GetMapping("/dashboard/charts/time-by-task-type")
    public ResponseEntity<List<TimeByTaskTypeResponse>> getTimeByTaskType(
            @RequestParam(value = "weekStartFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartFrom,
            @RequestParam(value = "weekStartTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartTo
    ) {
        List<TimeByTaskTypeResponse> response = dashboardMetricsService.getTimeByTaskType(weekStartFrom, weekStartTo);
        return ResponseEntity.ok(response);
    }

    /**
     * Activity feed of recent submissions and manager reviews (C6-T08).
     */
    @GetMapping("/dashboard/activity-feed")
    public ResponseEntity<List<ActivityFeedItemResponse>> getActivityFeed(
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        List<ActivityFeedItemResponse> response = dashboardMetricsService.getActivityFeed(limit);
        return ResponseEntity.ok(response);
    }
}
