package com.shan.weeklyreport;

import com.shan.weeklyreport.common.*;
import com.shan.weeklyreport.domain.*;
import com.shan.weeklyreport.repository.*;
import com.shan.weeklyreport.security.JwtService;
import com.shan.weeklyreport.service.DashboardMetricsService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DashboardMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportReviewRepository reportReviewRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DashboardMetricsService dashboardMetricsService;

    @Autowired
    private Clock clock;

    private User managerUser;
    private User memberUser1;
    private User memberUser2;
    private User adminUser;
    private Project sampleProject;

    private static final List<String> SEED_PROJECTS = List.of("Client A", "Internal Tooling", "R&D", "Marketing");

    @BeforeEach
    void setUp() {
        reportReviewRepository.deleteAll();
        reportVersionRepository.deleteAll();
        reportRepository.deleteAll();

        // Clean up test projects while preserving seed projects
        projectRepository.findAll().forEach(p -> {
            if (!SEED_PROJECTS.contains(p.getName())) {
                projectRepository.delete(p);
            } else {
                p.setActive(true);
                projectRepository.save(p);
            }
        });

        for (String seedName : SEED_PROJECTS) {
            if (projectRepository.findAll().stream().noneMatch(p -> p.getName().equals(seedName))) {
                projectRepository.save(new Project(seedName, "Seed desc", true));
            }
        }

        sampleProject = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Client A"))
                .findFirst().orElseThrow();

        // Clean up test users while preserving admin
        userRepository.findAll().forEach(u -> {
            if (!"admin@weeklyreport.local".equals(u.getEmail())) {
                userRepository.delete(u);
            }
        });

        adminUser = userRepository.findByEmail("admin@weeklyreport.local")
                .orElseGet(() -> userRepository.save(new User(
                        "Admin Carol",
                        "admin@weeklyreport.local",
                        passwordEncoder.encode("Pass123!"),
                        Role.ADMIN,
                        AccountStatus.ACTIVE
                )));

        managerUser = userRepository.save(new User(
                "Manager Alice",
                "manager-dash@test.com",
                passwordEncoder.encode("Pass123!"),
                Role.MANAGER,
                AccountStatus.ACTIVE
        ));

        memberUser1 = userRepository.save(new User(
                "Alice Member",
                "member1-dash@test.com",
                passwordEncoder.encode("Pass123!"),
                Role.TEAM_MEMBER,
                AccountStatus.ACTIVE
        ));

        memberUser2 = userRepository.save(new User(
                "Bob Member",
                "member2-dash@test.com",
                passwordEncoder.encode("Pass123!"),
                Role.TEAM_MEMBER,
                AccountStatus.ACTIVE
        ));
    }

    @AfterEach
    void tearDown() {
        // Reset default clock
        dashboardMetricsService.setClock(clock);

        projectRepository.findAll().forEach(p -> {
            if (SEED_PROJECTS.contains(p.getName()) && !p.isActive()) {
                p.setActive(true);
                projectRepository.save(p);
            }
        });
    }

    private Cookie getAuthCookie(User user) {
        String token = jwtService.generateAccessToken(user);
        return new Cookie("access_token", token);
    }

    @Test
    @DisplayName("C6-T01 & C6-T09: RBAC - Non-manager cannot access dashboard metrics and charts")
    void rbacEnforcementOnDashboardEndpoints() throws Exception {
        Cookie memberCookie = getAuthCookie(memberUser1);
        Cookie adminCookie = getAuthCookie(adminUser);

        String[] endpoints = {
                "/api/manager/dashboard/summary",
                "/api/manager/dashboard/charts/tasks-completed-trend",
                "/api/manager/dashboard/charts/status-by-member",
                "/api/manager/dashboard/charts/workload-by-project",
                "/api/manager/dashboard/charts/time-by-task-type",
                "/api/manager/dashboard/activity-feed"
        };

        for (String ep : endpoints) {
            mockMvc.perform(get(ep).cookie(memberCookie))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(ep).cookie(adminCookie))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("C6-T03 & C6-T09: Summary metrics, Friday deadline boundary, and backlog counts")
    void summaryMetricsAndFridayDeadlineBoundary() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        // Monday of week: 2026-09-07 (Friday is 2026-09-11)
        LocalDate monday = LocalDate.of(2026, 9, 7);

        // Member 1 submits report for current week
        Report submittedReport = new Report(memberUser1, sampleProject, monday, monday.plusDays(6));
        submittedReport.setStatus(ReportStatus.SUBMITTED);
        submittedReport.setSubmittedAt(monday.atTime(15, 0));
        reportRepository.save(submittedReport);

        // Member 2 has no report (NOT_STARTED)

        // Prior week report with NEEDS_CORRECTION
        LocalDate priorMonday = monday.minusWeeks(2);
        Report priorNcReport = new Report(memberUser1, sampleProject, priorMonday, priorMonday.plusDays(6));
        priorNcReport.setStatus(ReportStatus.NEEDS_CORRECTION);
        reportRepository.save(priorNcReport);

        // Prior week report with an unresolved blocker
        Report priorBlockerReport = new Report(memberUser2, sampleProject, priorMonday, priorMonday.plusDays(6));
        priorBlockerReport.setStatus(ReportStatus.APPROVED);
        ReportBlocker openBlocker = new ReportBlocker();
        openBlocker.setReport(priorBlockerReport);
        openBlocker.setDescription("Persistent blocker");
        openBlocker.setResolved(false);
        priorBlockerReport.getBlockers().add(openBlocker);
        reportRepository.save(priorBlockerReport);

        // Case 1: Fixed clock on Wednesday 2026-09-09 12:00:00 UTC (before Friday 23:59:59 deadline)
        Instant wednesday = LocalDate.of(2026, 9, 9).atTime(12, 0).toInstant(ZoneOffset.UTC);
        dashboardMetricsService.setClock(Clock.fixed(wednesday, ZoneOffset.UTC));

        mockMvc.perform(get("/api/manager/dashboard/summary").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmittedThisWeek", is(1)))
                .andExpect(jsonPath("$.compliance.submitted", is(1)))
                .andExpect(jsonPath("$.compliance.pending", is(1)))
                .andExpect(jsonPath("$.compliance.late", is(0)))
                .andExpect(jsonPath("$.compliance.totalActiveTeamMembers", is(2)))
                .andExpect(jsonPath("$.needsCorrectionCount", is(1)))
                .andExpect(jsonPath("$.openBlockersCount", is(1)));

        // Case 2: Fixed clock on Saturday 2026-09-12 10:00:00 UTC (past Friday 23:59:59 deadline)
        Instant saturday = LocalDate.of(2026, 9, 12).atTime(10, 0).toInstant(ZoneOffset.UTC);
        dashboardMetricsService.setClock(Clock.fixed(saturday, ZoneOffset.UTC));

        mockMvc.perform(get("/api/manager/dashboard/summary").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compliance.submitted", is(1)))
                .andExpect(jsonPath("$.compliance.pending", is(0)))
                .andExpect(jsonPath("$.compliance.late", is(1)));
    }

    @Test
    @DisplayName("C6-T04 to C6-T07: Charts exclude draft content and manager personal reports")
    void chartsExcludeDraftAndManagerReports() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        LocalDate monday = LocalDate.of(2026, 9, 7);
        Instant wednesday = LocalDate.of(2026, 9, 9).atTime(12, 0).toInstant(ZoneOffset.UTC);
        dashboardMetricsService.setClock(Clock.fixed(wednesday, ZoneOffset.UTC));

        // 1. A DRAFT report by memberUser1 with completed tasks and hours
        Report draftReport = new Report(memberUser1, sampleProject, monday, monday.plusDays(6));
        draftReport.setStatus(ReportStatus.DRAFT);
        ReportTaskItem draftTask = new ReportTaskItem();
        draftTask.setReport(draftReport);
        draftTask.setTaskName("Draft Task");
        draftTask.setPriority(TaskPriority.MEDIUM);
        draftTask.setStatus(TaskProgressStatus.COMPLETED);
        draftTask.setTimeSpentHours(BigDecimal.valueOf(15));
        draftReport.getTaskItems().add(draftTask);

        ReportHoursByType draftHours = new ReportHoursByType(draftReport, TaskType.DEVELOPMENT, BigDecimal.valueOf(15));
        draftReport.getHoursByType().add(draftHours);
        reportRepository.save(draftReport);

        // 2. A SUBMITTED report by managerUser (manager's own personal report) with completed tasks and hours
        Report managerReport = new Report(managerUser, sampleProject, monday, monday.plusDays(6));
        managerReport.setStatus(ReportStatus.SUBMITTED);
        managerReport.setSubmittedAt(monday.atTime(12, 0));
        ReportTaskItem mgrTask = new ReportTaskItem();
        mgrTask.setReport(managerReport);
        mgrTask.setTaskName("Manager Task");
        mgrTask.setPriority(TaskPriority.MEDIUM);
        mgrTask.setStatus(TaskProgressStatus.COMPLETED);
        mgrTask.setTimeSpentHours(BigDecimal.valueOf(20));
        managerReport.getTaskItems().add(mgrTask);

        ReportHoursByType mgrHours = new ReportHoursByType(managerReport, TaskType.DEVELOPMENT, BigDecimal.valueOf(20));
        managerReport.getHoursByType().add(mgrHours);
        reportRepository.save(managerReport);

        // 3. A qualifying SUBMITTED report by memberUser2 with 1 completed task and 5 hours
        Report memberReport = new Report(memberUser2, sampleProject, monday, monday.plusDays(6));
        memberReport.setStatus(ReportStatus.SUBMITTED);
        memberReport.setSubmittedAt(monday.atTime(14, 0));
        ReportTaskItem memberTask = new ReportTaskItem();
        memberTask.setReport(memberReport);
        memberTask.setTaskName("Real Completed Task");
        memberTask.setPriority(TaskPriority.MEDIUM);
        memberTask.setStatus(TaskProgressStatus.COMPLETED);
        memberTask.setTimeSpentHours(BigDecimal.valueOf(5));
        memberReport.getTaskItems().add(memberTask);

        ReportHoursByType memberHours = new ReportHoursByType(memberReport, TaskType.DEVELOPMENT, BigDecimal.valueOf(5));
        memberReport.getHoursByType().add(memberHours);
        reportRepository.save(memberReport);

        // Verify Trend Chart: only memberReport's 1 task counted (draft and manager excluded)
        mockMvc.perform(get("/api/manager/dashboard/charts/tasks-completed-trend?weeks=1").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].completedCount", is(1)));

        // Verify Workload Chart: only memberReport's 5 hours counted (draft and manager excluded)
        mockMvc.perform(get("/api/manager/dashboard/charts/workload-by-project?weekStartFrom=2026-09-07&weekStartTo=2026-09-07").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalHours", is(5.0)));

        // Verify Time by Task Type: DEVELOPMENT is 5.0, other 4 types are 0.0 (all 5 enum values present)
        mockMvc.perform(get("/api/manager/dashboard/charts/time-by-task-type?weekStartFrom=2026-09-07&weekStartTo=2026-09-07").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.taskType == 'DEVELOPMENT')].totalHours", contains(5.0)))
                .andExpect(jsonPath("$[?(@.taskType == 'TESTING')].totalHours", contains(0)));
    }

    @Test
    @DisplayName("C6-T05: Status by member chart includes draft status metadata")
    void statusByMemberChart() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        LocalDate monday = LocalDate.of(2026, 9, 7);
        Instant wednesday = LocalDate.of(2026, 9, 9).atTime(12, 0).toInstant(ZoneOffset.UTC);
        dashboardMetricsService.setClock(Clock.fixed(wednesday, ZoneOffset.UTC));

        // Member 1 has 1 draft and 1 approved
        Report draft = new Report(memberUser1, sampleProject, monday, monday.plusDays(6));
        draft.setStatus(ReportStatus.DRAFT);
        reportRepository.save(draft);

        Report approved = new Report(memberUser1, sampleProject, monday.minusWeeks(1), monday.minusWeeks(1).plusDays(6));
        approved.setStatus(ReportStatus.APPROVED);
        reportRepository.save(approved);

        // Member 2 has 1 submitted
        Report submitted = new Report(memberUser2, sampleProject, monday, monday.plusDays(6));
        submitted.setStatus(ReportStatus.SUBMITTED);
        reportRepository.save(submitted);

        mockMvc.perform(get("/api/manager/dashboard/charts/status-by-member?weekStartFrom=2026-08-31&weekStartTo=2026-09-07").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.fullName == 'Alice Member')].draft", contains(1)))
                .andExpect(jsonPath("$[?(@.fullName == 'Alice Member')].approved", contains(1)))
                .andExpect(jsonPath("$[?(@.fullName == 'Bob Member')].submitted", contains(1)));
    }

    @Test
    @DisplayName("C6-T08: Activity feed combines submissions and reviews ordered desc with limit")
    void activityFeedCombinesSubmissionsAndReviews() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        LocalDate monday = LocalDate.of(2026, 9, 7);

        // 1. Report submitted
        Report rep = new Report(memberUser1, sampleProject, monday, monday.plusDays(6));
        rep.setStatus(ReportStatus.APPROVED);
        rep.setSubmittedAt(monday.atTime(10, 0));
        rep = reportRepository.save(rep);

        // 2. Version created
        ReportVersion v1 = new ReportVersion(rep, 1, "{}", monday.atTime(10, 0));
        v1 = reportVersionRepository.save(v1);

        // 3. Review logged later
        ReportReview review = new ReportReview(rep, v1, managerUser, ReviewAction.APPROVED, "Great job");
        review.setReviewedAt(monday.atTime(14, 0));
        reportReviewRepository.save(review);

        mockMvc.perform(get("/api/manager/dashboard/activity-feed?limit=10").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // Most recent first: REVIEW at 14:00, then SUBMISSION at 10:00
                .andExpect(jsonPath("$[0].type", is("REVIEW")))
                .andExpect(jsonPath("$[0].actorName", is("Manager Alice")))
                .andExpect(jsonPath("$[0].targetUserName", is("Alice Member")))
                .andExpect(jsonPath("$[0].action", is("APPROVED")))
                .andExpect(jsonPath("$[0].reportId", is(rep.getId().intValue())))
                .andExpect(jsonPath("$[1].type", is("SUBMISSION")))
                .andExpect(jsonPath("$[1].actorName", is("Alice Member")))
                .andExpect(jsonPath("$[1].reportId", is(rep.getId().intValue())));
    }
}
