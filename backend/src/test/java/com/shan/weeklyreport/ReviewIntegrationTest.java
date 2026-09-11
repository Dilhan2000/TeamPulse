package com.shan.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.ReviewAction;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.common.TaskPriority;
import com.shan.weeklyreport.common.TaskProgressStatus;
import com.shan.weeklyreport.domain.Project;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.repository.*;
import com.shan.weeklyreport.security.AuthCookieFactory;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReviewIntegrationTest {

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
    private User managerUser;
    private User adminUser;
    private Project activeProject;

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

        managerUser = userRepository.save(new User(
                "Carol Manager",
                "carol@weeklyreport.local",
                passwordEncoder.encode("Password123!"),
                Role.MANAGER,
                AccountStatus.ACTIVE
        ));

        activeProject = projectRepository.findAll().stream()
                .filter(Project::isActive)
                .findFirst()
                .orElseGet(() -> projectRepository.save(new Project("Default Project", "Description", true)));
    }

    private Cookie createAuthCookie(User user) {
        String token = jwtService.generateAccessToken(user);
        return new Cookie(AuthCookieFactory.ACCESS_TOKEN_COOKIE, token);
    }

    @Test
    @DisplayName("C3-T10, C3-T14: Non-manager users are rejected with 403 from /api/manager/reports/**")
    void shouldDenyNonManagersFromManagerEndpoints() throws Exception {
        mockMvc.perform(get("/api/manager/reports")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/manager/reports")
                        .cookie(createAuthCookie(adminUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C3-T08, C3-T11: Request changes with blank comment returns 400 Bad Request")
    void shouldRejectBlankCommentWhenRequestingChanges() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Report report = reportRepository.save(new Report(memberUser1, activeProject, monday, monday.plusDays(6)));
        report.setStatus(ReportStatus.SUBMITTED);
        reportRepository.save(report);

        RequestChangesRequest req = new RequestChangesRequest("");

        mockMvc.perform(post("/api/manager/reports/" + report.getId() + "/request-changes")
                        .cookie(createAuthCookie(managerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Comment is required")));
    }

    @Test
    @DisplayName("C3-T07, C3-T08: Review actions on non-SUBMITTED report return 409 Conflict")
    void shouldRejectReviewOnNonSubmittedReport() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Report report = reportRepository.save(new Report(memberUser1, activeProject, monday, monday.plusDays(6)));
        // Report is DRAFT

        mockMvc.perform(post("/api/manager/reports/" + report.getId() + "/approve")
                        .cookie(createAuthCookie(managerUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only submitted reports")));

        RequestChangesRequest req = new RequestChangesRequest("Need changes");
        mockMvc.perform(post("/api/manager/reports/" + report.getId() + "/request-changes")
                        .cookie(createAuthCookie(managerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C3-T12, C3-T14: Cross-user access to owner version endpoints returns 404")
    void shouldPreventCrossUserAccessToVersions() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Report aliceReport = reportRepository.save(new Report(memberUser1, activeProject, monday, monday.plusDays(6)));

        // Bob tries to access Alice's versions
        mockMvc.perform(get("/api/reports/" + aliceReport.getId() + "/versions")
                        .cookie(createAuthCookie(memberUser2)))
                .andExpect(status().isNotFound());

        // Bob tries to access Alice's reviews
        mockMvc.perform(get("/api/reports/" + aliceReport.getId() + "/reviews")
                        .cookie(createAuthCookie(memberUser2)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("C3-T15: Full end-to-end correction cycle with immutable version snapshots")
    void shouldExecuteFullCorrectionCycleSuccessfully() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);

        // 1. Alice creates draft report
        CreateReportRequest createReq = new CreateReportRequest(activeProject.getId(), monday);
        String createResponse = mockMvc.perform(post("/api/reports")
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ReportResponse reportRes = objectMapper.readValue(createResponse, ReportResponse.class);
        Long reportId = reportRes.id();

        // 2. Alice updates draft with initial content
        UpdateReportRequest initialUpdate = new UpdateReportRequest(
                activeProject.getId(),
                "Initial draft notes",
                List.of(new TaskItemDto(null, "Task v1", TaskPriority.MEDIUM, 50, 50, TaskProgressStatus.IN_PROGRESS, new BigDecimal("10.00"), new BigDecimal("8.00"), "Deliverable v1", 0)),
                List.of(new NextWeekTaskDto(null, "Next week v1", 0)),
                List.of(new BlockerDto(null, "Blocker v1", true, false, 0)),
                List.of(new AchievementDto(null, "Win v1", true, 0)),
                List.of()
        );

        mockMvc.perform(put("/api/reports/" + reportId)
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialUpdate)))
                .andExpect(status().isOk());

        // 3. Alice submits report -> creates Version 1
        mockMvc.perform(post("/api/reports/" + reportId + "/submit")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")));

        // Verify Version 1 was created
        assertEquals(1, reportVersionRepository.findByReportIdOrderByVersionNumberDesc(reportId).size());

        // 4. Carol (Manager) requests changes with comment
        RequestChangesRequest changesReq = new RequestChangesRequest("Please add more details to task deliverable.");
        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .cookie(createAuthCookie(managerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changesReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NEEDS_CORRECTION")))
                .andExpect(jsonPath("$.latestReview.action", is("CHANGES_REQUESTED")))
                .andExpect(jsonPath("$.latestReview.comment", containsString("Please add more details")))
                .andExpect(jsonPath("$.latestReview.versionNumber", is(1)));

        // 5. Alice edits report while in NEEDS_CORRECTION -> remains NEEDS_CORRECTION (C3-T05)
        UpdateReportRequest correctedUpdate = new UpdateReportRequest(
                activeProject.getId(),
                "Corrected notes with detailed deliverable",
                List.of(new TaskItemDto(null, "Task v2 corrected", TaskPriority.HIGH, 100, 100, TaskProgressStatus.COMPLETED, new BigDecimal("10.00"), new BigDecimal("12.00"), "PR #42 with test evidence", 0)),
                List.of(new NextWeekTaskDto(null, "Next week v2", 0)),
                List.of(new BlockerDto(null, "Blocker v1", true, true, 0)),
                List.of(new AchievementDto(null, "Win v1", true, 0)),
                List.of()
        );

        mockMvc.perform(put("/api/reports/" + reportId)
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctedUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NEEDS_CORRECTION")))
                .andExpect(jsonPath("$.notes", is("Corrected notes with detailed deliverable")));

        // 6. Alice resubmits report -> creates Version 2 and status flips to SUBMITTED (C3-T06)
        mockMvc.perform(post("/api/reports/" + reportId + "/submit")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")));

        assertEquals(2, reportVersionRepository.findByReportIdOrderByVersionNumberDesc(reportId).size());

        // 7. Carol approves report -> status becomes APPROVED (C3-T07)
        ApproveReportRequest approveReq = new ApproveReportRequest("Great job, looks complete!");
        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .cookie(createAuthCookie(managerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.latestReview.action", is("APPROVED")))
                .andExpect(jsonPath("$.latestReview.versionNumber", is(2)));

        // 8. Verify both historical versions are independently retrievable with their distinct content
        String versionsListJson = mockMvc.perform(get("/api/reports/" + reportId + "/versions")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].versionNumber", is(2)))
                .andExpect(jsonPath("$[1].versionNumber", is(1)))
                .andReturn().getResponse().getContentAsString();

        List<ReportVersionSummaryResponse> versionSummaries = objectMapper.readValue(
                versionsListJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ReportVersionSummaryResponse.class)
        );

        Long v2Id = versionSummaries.get(0).id();
        Long v1Id = versionSummaries.get(1).id();

        // Inspect Version 1 snapshot: retains "Initial draft notes" and "Task v1"
        mockMvc.perform(get("/api/reports/" + reportId + "/versions/" + v1Id)
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", is("Initial draft notes")))
                .andExpect(jsonPath("$.tasksCompleted[0].taskName", is("Task v1")));

        // Inspect Version 2 snapshot: has "Corrected notes" and "Task v2 corrected"
        mockMvc.perform(get("/api/reports/" + reportId + "/versions/" + v2Id)
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", is("Corrected notes with detailed deliverable")))
                .andExpect(jsonPath("$.tasksCompleted[0].taskName", is("Task v2 corrected")));

        // 9. Verify review history shows both review records in order
        mockMvc.perform(get("/api/reports/" + reportId + "/reviews")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action", is("APPROVED")))
                .andExpect(jsonPath("$[0].versionNumber", is(2)))
                .andExpect(jsonPath("$[1].action", is("CHANGES_REQUESTED")))
                .andExpect(jsonPath("$[1].versionNumber", is(1)));
    }
}
