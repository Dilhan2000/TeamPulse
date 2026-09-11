package com.shan.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.common.*;
import com.shan.weeklyreport.domain.*;
import com.shan.weeklyreport.repository.*;
import com.shan.weeklyreport.security.JwtService;
import com.shan.weeklyreport.service.TeamMemberProfileService;
import jakarta.servlet.http.Cookie;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TeamMemberProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private ReportReviewRepository reportReviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TeamMemberProfileService teamMemberProfileService;

    private User managerUser;
    private User memberUser;
    private User disabledMember;
    private User adminUser;
    private Project sampleProject;

    @BeforeEach
    void setUp() {
        reportReviewRepository.deleteAll();
        reportVersionRepository.deleteAll();
        reportRepository.deleteAll();

        userRepository.findAll().forEach(u -> {
            if (!"admin@weeklyreport.local".equals(u.getEmail())) {
                userRepository.delete(u);
            }
        });

        adminUser = userRepository.findByEmail("admin@weeklyreport.local")
                .orElseGet(() -> userRepository.save(new User(
                        "Admin",
                        "admin@weeklyreport.local",
                        passwordEncoder.encode("ChangeMe123!"),
                        Role.ADMIN,
                        AccountStatus.ACTIVE
                )));

        projectRepository.deleteAll();

        managerUser = new User(
                "Manager Alice",
                "manager-prof@test.com",
                passwordEncoder.encode("Password123!"),
                Role.MANAGER,
                AccountStatus.ACTIVE
        );
        managerUser = userRepository.save(managerUser);

        memberUser = new User(
                "Bob Member",
                "member-prof@test.com",
                passwordEncoder.encode("Password123!"),
                Role.TEAM_MEMBER,
                AccountStatus.ACTIVE
        );
        memberUser = userRepository.save(memberUser);

        disabledMember = new User(
                "Charlie Disabled",
                "disabled-prof@test.com",
                passwordEncoder.encode("Password123!"),
                Role.TEAM_MEMBER,
                AccountStatus.DISABLED
        );
        disabledMember = userRepository.save(disabledMember);

        adminUser = new User(
                "Admin Dave",
                "admin-prof@test.com",
                passwordEncoder.encode("Password123!"),
                Role.ADMIN,
                AccountStatus.ACTIVE
        );
        adminUser = userRepository.save(adminUser);

        sampleProject = new Project("Project Alpha", "Description", true);
        sampleProject = projectRepository.save(sampleProject);
    }

    private Cookie getAuthCookie(User user) {
        String token = jwtService.generateAccessToken(user);
        return new Cookie("access_token", token);
    }

    @Test
    @DisplayName("C7-T02: Non-manager caller gets 403")
    void nonManagerGets403() throws Exception {
        Cookie memberCookie = getAuthCookie(memberUser);
        Cookie adminCookie = getAuthCookie(adminUser);

        mockMvc.perform(get("/api/manager/team-members/" + memberUser.getId()).cookie(memberCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/manager/team-members/" + memberUser.getId()).cookie(adminCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C7-T02: Requesting manager, admin, or non-existent user returns 404")
    void requestingNonTeamMemberReturns404() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        // Manager ID -> 404
        mockMvc.perform(get("/api/manager/team-members/" + managerUser.getId()).cookie(managerCookie))
                .andExpect(status().isNotFound());

        // Admin ID -> 404
        mockMvc.perform(get("/api/manager/team-members/" + adminUser.getId()).cookie(managerCookie))
                .andExpect(status().isNotFound());

        // Non-existent ID -> 404
        mockMvc.perform(get("/api/manager/team-members/999999").cookie(managerCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("C7-T02: Disabled team member profile is fetchable for historical review")
    void disabledMemberProfileIsFetchable() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        mockMvc.perform(get("/api/manager/team-members/" + disabledMember.getId()).cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(disabledMember.getId().intValue())))
                .andExpect(jsonPath("$.fullName", is("Charlie Disabled")))
                .andExpect(jsonPath("$.status", is("DISABLED")))
                .andExpect(jsonPath("$.stats.currentWeekStatus", is("NOT_STARTED")));
    }

    @Test
    @DisplayName("C7-T01 & C7-T03: End-to-end multi-week, multi-status stats calculation")
    void endToEndStatsCalculation() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        LocalDate mondayWeek1 = LocalDate.of(2026, 8, 24);
        LocalDate mondayWeek2 = LocalDate.of(2026, 8, 31);
        LocalDate mondayWeek3 = LocalDate.of(2026, 9, 7);

        // Pin current week to mondayWeek3
        teamMemberProfileService.setClock(Clock.fixed(
                mondayWeek3.plusDays(2).atTime(12, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        ));

        // Report 1 (Week 1): APPROVED, 10 hours, 2 completed tasks
        Report r1 = new Report(memberUser, sampleProject, mondayWeek1, mondayWeek1.plusDays(6));
        r1.setStatus(ReportStatus.APPROVED);
        r1.setSubmittedAt(mondayWeek1.atTime(10, 0));

        ReportTaskItem t1 = new ReportTaskItem();
        t1.setReport(r1);
        t1.setTaskName("Task 1");
        t1.setPriority(TaskPriority.HIGH);
        t1.setStatus(TaskProgressStatus.COMPLETED);
        t1.setTimeSpentHours(BigDecimal.valueOf(6));
        r1.getTaskItems().add(t1);

        ReportTaskItem t2 = new ReportTaskItem();
        t2.setReport(r1);
        t2.setTaskName("Task 2");
        t2.setPriority(TaskPriority.MEDIUM);
        t2.setStatus(TaskProgressStatus.COMPLETED);
        t2.setTimeSpentHours(BigDecimal.valueOf(4));
        r1.getTaskItems().add(t2);

        r1.getHoursByType().add(new ReportHoursByType(r1, TaskType.DEVELOPMENT, BigDecimal.valueOf(10)));
        r1 = reportRepository.save(r1);

        ReportVersion v1 = new ReportVersion(r1, 1, "{}", mondayWeek1.atTime(10, 0));
        v1 = reportVersionRepository.save(v1);

        ReportReview rev1 = new ReportReview(r1, v1, managerUser, ReviewAction.APPROVED, "Well done");
        reportReviewRepository.save(rev1);

        // Report 2 (Week 2): Sent back for CHANGES_REQUESTED (needs correction), 8 hours, 1 completed task
        Report r2 = new Report(memberUser, sampleProject, mondayWeek2, mondayWeek2.plusDays(6));
        r2.setStatus(ReportStatus.NEEDS_CORRECTION);
        r2.setSubmittedAt(mondayWeek2.atTime(11, 0));

        ReportTaskItem t3 = new ReportTaskItem();
        t3.setReport(r2);
        t3.setTaskName("Task 3");
        t3.setPriority(TaskPriority.MEDIUM);
        t3.setStatus(TaskProgressStatus.COMPLETED);
        t3.setTimeSpentHours(BigDecimal.valueOf(8));
        r2.getTaskItems().add(t3);

        r2.getHoursByType().add(new ReportHoursByType(r2, TaskType.TESTING, BigDecimal.valueOf(8)));
        r2 = reportRepository.save(r2);

        ReportVersion v2 = new ReportVersion(r2, 1, "{}", mondayWeek2.atTime(11, 0));
        v2 = reportVersionRepository.save(v2);

        ReportReview rev2 = new ReportReview(r2, v2, managerUser, ReviewAction.CHANGES_REQUESTED, "Please clarify blockers");
        reportReviewRepository.save(rev2);

        // Report 3 (Week 3 - Current Week): DRAFT, 5 hours (draft hours/tasks must NOT count into totals)
        Report r3 = new Report(memberUser, sampleProject, mondayWeek3, mondayWeek3.plusDays(6));
        r3.setStatus(ReportStatus.DRAFT);

        ReportTaskItem t4 = new ReportTaskItem();
        t4.setReport(r3);
        t4.setTaskName("Draft Task");
        t4.setPriority(TaskPriority.LOW);
        t4.setStatus(TaskProgressStatus.COMPLETED);
        t4.setTimeSpentHours(BigDecimal.valueOf(5));
        r3.getTaskItems().add(t4);

        r3.getHoursByType().add(new ReportHoursByType(r3, TaskType.DEVELOPMENT, BigDecimal.valueOf(5)));
        reportRepository.save(r3);

        // Expected stats:
        // totalReportsSubmitted = 2 (r1, r2; r3 is draft so submittedAt is null)
        // approvedCount = 1 (r1)
        // needsCorrectionSentBackCount = 1 (rev2)
        // currentWeekStatus = DRAFT (r3 is current week's report)
        // totalHoursLogged = 18.0 (10 + 8; draft 5 excluded)
        // totalTasksCompleted = 3 (t1, t2, t3; draft t4 excluded)
        mockMvc.perform(get("/api/manager/team-members/" + memberUser.getId()).cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(memberUser.getId().intValue())))
                .andExpect(jsonPath("$.fullName", is("Bob Member")))
                .andExpect(jsonPath("$.email", is("member-prof@test.com")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.stats.totalReportsSubmitted", is(2)))
                .andExpect(jsonPath("$.stats.approvedCount", is(1)))
                .andExpect(jsonPath("$.stats.needsCorrectionSentBackCount", is(1)))
                .andExpect(jsonPath("$.stats.currentWeekStatus", is("DRAFT")))
                .andExpect(jsonPath("$.stats.totalHoursLogged", is(18.0)))
                .andExpect(jsonPath("$.stats.totalTasksCompleted", is(3)));
    }
}
