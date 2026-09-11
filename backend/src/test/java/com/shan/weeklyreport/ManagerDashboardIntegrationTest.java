package com.shan.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.common.TaskPriority;
import com.shan.weeklyreport.common.TaskProgressStatus;
import com.shan.weeklyreport.common.TaskType;
import com.shan.weeklyreport.domain.*;
import com.shan.weeklyreport.dto.HoursByTypeDto;
import com.shan.weeklyreport.dto.TaskItemDto;
import com.shan.weeklyreport.dto.UpdateReportRequest;
import com.shan.weeklyreport.repository.*;
import com.shan.weeklyreport.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ManagerDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private User memberUser1;
    private User memberUser2;
    private User disabledMember;
    private User managerUser;
    private User adminUser;
    private Project projectA;
    private Project projectB;

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

        memberUser1 = userRepository.save(new User(
                "Alice Member",
                "alice@weeklyreport.local",
                passwordEncoder.encode("Password123!"),
                Role.TEAM_MEMBER,
                AccountStatus.ACTIVE
        ));

        memberUser2 = userRepository.save(new User(
                "Bob Member",
                "bob@weeklyreport.local",
                passwordEncoder.encode("Password123!"),
                Role.TEAM_MEMBER,
                AccountStatus.ACTIVE
        ));

        disabledMember = userRepository.save(new User(
                "Disabled Dave",
                "dave@weeklyreport.local",
                passwordEncoder.encode("Password123!"),
                Role.TEAM_MEMBER,
                AccountStatus.DISABLED
        ));

        managerUser = userRepository.save(new User(
                "Carol Manager",
                "carol@weeklyreport.local",
                passwordEncoder.encode("Password123!"),
                Role.MANAGER,
                AccountStatus.ACTIVE
        ));

        projectA = projectRepository.findAll().stream()
                .filter(p -> "Client A".equals(p.getName()))
                .findFirst()
                .orElseGet(() -> projectRepository.save(new Project("Client A", "Description", true)));

        projectB = projectRepository.findAll().stream()
                .filter(p -> "Internal Tooling".equals(p.getName()))
                .findFirst()
                .orElseGet(() -> projectRepository.save(new Project("Internal Tooling", "Description", true)));
    }

    private Cookie createAuthCookie(User user) {
        String token = jwtService.generateAccessToken(user);
        return new Cookie("access_token", token);
    }

    @Test
    @DisplayName("C4-T03/C4-T04/C4-T06: Role restriction - Non-managers get 403 Forbidden")
    void testNonManagerRbac() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);

        mockMvc.perform(get("/api/manager/dashboard/team-status")
                        .param("weekStart", monday.toString())
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/manager/team-members")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/manager/dashboard/section")
                        .param("weekStart", monday.toString())
                        .param("section", "BLOCKERS")
                        .cookie(createAuthCookie(adminUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C4-T03/C4-T06: Date validation - Non-Monday weekStart returns 400 Bad Request")
    void testNonMondayValidation() throws Exception {
        LocalDate tuesday = LocalDate.of(2026, 9, 8);

        mockMvc.perform(get("/api/manager/dashboard/team-status")
                        .param("weekStart", tuesday.toString())
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/manager/dashboard/section")
                        .param("weekStart", tuesday.toString())
                        .param("section", "BLOCKERS")
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C4-T04: Active team members list returns only active team members")
    void testActiveTeamMembers() throws Exception {
        mockMvc.perform(get("/api/manager/team-members")
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fullName", is("Alice Member")))
                .andExpect(jsonPath("$[1].fullName", is("Bob Member")));
    }

    @Test
    @DisplayName("C4-T02: Team status-by-week synthesizes NOT_STARTED and project filter excludes NOT_STARTED")
    void testTeamStatusSynthesisAndProjectFilter() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);

        // Alice has submitted a report for Project A
        Report report = new Report(memberUser1, projectA, monday, monday.plusDays(6));
        report.setStatus(ReportStatus.SUBMITTED);
        report.setSubmittedAt(monday.atTime(10, 0));
        reportRepository.save(report);

        // Bob has no report for this week (NOT_STARTED)

        // Without project filter: both appear
        mockMvc.perform(get("/api/manager/dashboard/team-status")
                        .param("weekStart", monday.toString())
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fullName", is("Alice Member")))
                .andExpect(jsonPath("$[0].status", is("SUBMITTED")))
                .andExpect(jsonPath("$[0].projectName", is("Client A")))
                .andExpect(jsonPath("$[1].fullName", is("Bob Member")))
                .andExpect(jsonPath("$[1].status", is("NOT_STARTED")))
                .andExpect(jsonPath("$[1].reportId", nullValue()));

        // With projectId filter for Project A: Alice matches, Bob (NOT_STARTED) is excluded
        mockMvc.perform(get("/api/manager/dashboard/team-status")
                        .param("weekStart", monday.toString())
                        .param("projectId", projectA.getId().toString())
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName", is("Alice Member")))
                .andExpect(jsonPath("$[0].status", is("SUBMITTED")));

        // With projectId filter for Project B: neither matches
        mockMvc.perform(get("/api/manager/dashboard/team-status")
                        .param("weekStart", monday.toString())
                        .param("projectId", projectB.getId().toString())
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("C4-T01: Manager personal report never appears in team queries and Draft content is protected")
    void testManagerReportExclusionAndDraftContentProtection() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);

        // Manager creates personal submitted report
        Report managerReport = new Report(managerUser, projectA, monday, monday.plusDays(6));
        managerReport.setStatus(ReportStatus.SUBMITTED);
        managerReport.setSubmittedAt(monday.atTime(9, 0));
        Report savedMgrReport = reportRepository.save(managerReport);

        // Alice creates a DRAFT report
        Report aliceDraft = new Report(memberUser1, projectA, monday, monday.plusDays(6));
        aliceDraft.setStatus(ReportStatus.DRAFT);
        Report savedAliceDraft = reportRepository.save(aliceDraft);

        // 1. Manager's report does not appear in team-status
        mockMvc.perform(get("/api/manager/dashboard/team-status")
                        .param("weekStart", monday.toString())
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fullName", not(hasItem("Carol Manager"))));

        // 2. Manager's report does not appear in manager reports list
        mockMvc.perform(get("/api/manager/reports")
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].userName", not(hasItem("Carol Manager"))));

        // 3. Manager's report content cannot be fetched via GET /api/manager/reports/{id} -> 404
        mockMvc.perform(get("/api/manager/reports/" + savedMgrReport.getId())
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isNotFound());

        // 4. Alice's draft report appears in team status as DRAFT
        mockMvc.perform(get("/api/manager/dashboard/team-status")
                        .param("weekStart", monday.toString())
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.fullName == 'Alice Member')].status", contains("DRAFT")));

        // 5. Alice's draft report content CANNOT be fetched by manager -> 404
        mockMvc.perform(get("/api/manager/reports/" + savedAliceDraft.getId())
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isNotFound());

        // 6. Alice's draft report does NOT appear in side-by-side section view
        mockMvc.perform(get("/api/manager/dashboard/section")
                        .param("weekStart", monday.toString())
                        .param("section", "BLOCKERS")
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("C4-T05/C4-T06: Side-by-side section view returns Blockers and Achievements with key flags")
    void testSideBySideSectionView() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);

        // Alice report with blocker (key issue) and achievement (key achievement)
        Report aliceReport = new Report(memberUser1, projectA, monday, monday.plusDays(6));
        aliceReport.setStatus(ReportStatus.SUBMITTED);
        ReportBlocker b1 = new ReportBlocker();
        b1.setReport(aliceReport);
        b1.setDescription("API auth timeout");
        b1.setKeyIssue(true);
        b1.setResolved(false);
        b1.setSortOrder(0);
        aliceReport.getBlockers().add(b1);

        ReportAchievement a1 = new ReportAchievement();
        a1.setReport(aliceReport);
        a1.setDescription("Shipped v1.0");
        a1.setKeyAchievement(true);
        a1.setSortOrder(0);
        aliceReport.getAchievements().add(a1);
        reportRepository.save(aliceReport);

        // Bob report with blocker (resolved, not key) and achievement (not key)
        Report bobReport = new Report(memberUser2, projectA, monday, monday.plusDays(6));
        bobReport.setStatus(ReportStatus.APPROVED);
        ReportBlocker b2 = new ReportBlocker();
        b2.setReport(bobReport);
        b2.setDescription("Network blip");
        b2.setKeyIssue(false);
        b2.setResolved(true);
        b2.setSortOrder(0);
        bobReport.getBlockers().add(b2);

        ReportAchievement a2 = new ReportAchievement();
        a2.setReport(bobReport);
        a2.setDescription("Wrote docs");
        a2.setKeyAchievement(false);
        a2.setSortOrder(0);
        bobReport.getAchievements().add(a2);
        reportRepository.save(bobReport);

        // Test Section: BLOCKERS
        mockMvc.perform(get("/api/manager/dashboard/section")
                        .param("weekStart", monday.toString())
                        .param("section", "BLOCKERS")
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fullName", is("Alice Member")))
                .andExpect(jsonPath("$[0].items[0].description", is("API auth timeout")))
                .andExpect(jsonPath("$[0].items[0].flagged", is(true)))
                .andExpect(jsonPath("$[0].items[0].resolved", is(false)))
                .andExpect(jsonPath("$[1].fullName", is("Bob Member")))
                .andExpect(jsonPath("$[1].items[0].description", is("Network blip")))
                .andExpect(jsonPath("$[1].items[0].flagged", is(false)))
                .andExpect(jsonPath("$[1].items[0].resolved", is(true)));

        // Test Section: ACHIEVEMENTS
        mockMvc.perform(get("/api/manager/dashboard/section")
                        .param("weekStart", monday.toString())
                        .param("section", "ACHIEVEMENTS")
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fullName", is("Alice Member")))
                .andExpect(jsonPath("$[0].items[0].description", is("Shipped v1.0")))
                .andExpect(jsonPath("$[0].items[0].flagged", is(true)))
                .andExpect(jsonPath("$[1].fullName", is("Bob Member")))
                .andExpect(jsonPath("$[1].items[0].description", is("Wrote docs")))
                .andExpect(jsonPath("$[1].items[0].flagged", is(false)));
    }
}
