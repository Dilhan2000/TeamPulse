package com.shan.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.common.*;
import com.shan.weeklyreport.domain.Project;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.repository.ProjectRepository;
import com.shan.weeklyreport.repository.ReportRepository;
import com.shan.weeklyreport.repository.UserRepository;
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
public class ReportIntegrationTest {

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
    private com.shan.weeklyreport.repository.ReportReviewRepository reportReviewRepository;

    @Autowired
    private com.shan.weeklyreport.repository.ReportVersionRepository reportVersionRepository;

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
    @DisplayName("C2-T13: GET /api/projects returns active projects")
    void shouldReturnActiveProjects() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", notNullValue()));
    }

    @Test
    @DisplayName("C2-T08, C2-T11: POST /api/reports creates draft report on a Monday")
    void shouldCreateDraftReport() throws Exception {
        // Monday date
        LocalDate monday = LocalDate.of(2026, 9, 7);
        CreateReportRequest req = new CreateReportRequest(activeProject.getId(), monday);

        mockMvc.perform(post("/api/reports")
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.weekStartDate", is("2026-09-07")))
                .andExpect(jsonPath("$.weekEndDate", is("2026-09-13")))
                .andExpect(jsonPath("$.projectId", is(activeProject.getId().intValue())))
                .andExpect(jsonPath("$.userName", is("Alice Member")));
    }

    @Test
    @DisplayName("C2-T08: POST /api/reports fails when weekStartDate is not a Monday")
    void shouldRejectNonMondayDate() throws Exception {
        // Wednesday date
        LocalDate wednesday = LocalDate.of(2026, 9, 9);
        CreateReportRequest req = new CreateReportRequest(activeProject.getId(), wednesday);

        mockMvc.perform(post("/api/reports")
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Monday")));
    }

    @Test
    @DisplayName("C2-T08: POST /api/reports returns 409 when report for week already exists")
    void shouldRejectDuplicateReportForSameWeek() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        CreateReportRequest req = new CreateReportRequest(activeProject.getId(), monday);

        // First creation succeeds
        mockMvc.perform(post("/api/reports")
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second creation fails with 409
        mockMvc.perform(post("/api/reports")
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("C2-T11: POST /api/reports by ADMIN returns 403 Forbidden")
    void shouldDenyAdminFromCreatingReport() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        CreateReportRequest req = new CreateReportRequest(activeProject.getId(), monday);

        mockMvc.perform(post("/api/reports")
                        .cookie(createAuthCookie(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C2-T09, C2-T11: PUT /api/reports/{id} updates draft and replaces child rows")
    void shouldUpdateDraftReport() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Report report = reportRepository.save(new Report(memberUser1, activeProject, monday, monday.plusDays(6)));

        UpdateReportRequest req = new UpdateReportRequest(
                activeProject.getId(),
                "Weekly notes here",
                List.of(new TaskItemDto(null, "Implement auth", TaskPriority.HIGH, 100, 100,
                        TaskProgressStatus.COMPLETED, new BigDecimal("10.00"), new BigDecimal("9.50"), "Auth PR", 0)),
                List.of(new NextWeekTaskDto(null, "Implement dashboard", 0)),
                List.of(new BlockerDto(null, "API dependency delayed", true, false, 0)),
                List.of(new AchievementDto(null, "Shipped release on time", true, 0)),
                List.of(new HoursByTypeDto(TaskType.DEVELOPMENT, new BigDecimal("35.00")),
                        new HoursByTypeDto(TaskType.MEETINGS, new BigDecimal("5.00")))
        );

        mockMvc.perform(put("/api/reports/" + report.getId())
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", is("Weekly notes here")))
                .andExpect(jsonPath("$.tasksCompleted", hasSize(1)))
                .andExpect(jsonPath("$.tasksCompleted[0].taskName", is("Implement auth")))
                .andExpect(jsonPath("$.tasksPlannedNextWeek", hasSize(1)))
                .andExpect(jsonPath("$.blockers", hasSize(1)))
                .andExpect(jsonPath("$.blockers[0].isKeyIssue", is(true)))
                .andExpect(jsonPath("$.achievements", hasSize(1)))
                .andExpect(jsonPath("$.achievements[0].isKeyAchievement", is(true)))
                .andExpect(jsonPath("$.hoursByType", hasSize(2)));

        // Verify that updating a second time (e.g. modifying hours) succeeds without duplicate entry error
        UpdateReportRequest secondReq = new UpdateReportRequest(
                activeProject.getId(),
                "Updated notes",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new HoursByTypeDto(TaskType.DEVELOPMENT, new BigDecimal("40.00")))
        );

        mockMvc.perform(put("/api/reports/" + report.getId())
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", is("Updated notes")))
                .andExpect(jsonPath("$.hoursByType", hasSize(1)))
                .andExpect(jsonPath("$.hoursByType[0].taskType", is("DEVELOPMENT")))
                .andExpect(jsonPath("$.hoursByType[0].hours", is(40.0)));
    }

    @Test
    @DisplayName("C2-T07: PUT /api/reports/{id} with multiple key issues returns 400")
    void shouldRejectMultipleKeyIssues() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Report report = reportRepository.save(new Report(memberUser1, activeProject, monday, monday.plusDays(6)));

        UpdateReportRequest req = new UpdateReportRequest(
                activeProject.getId(),
                "Notes",
                List.of(),
                List.of(),
                List.of(
                        new BlockerDto(null, "Blocker 1", true, false, 0),
                        new BlockerDto(null, "Blocker 2", true, false, 1)
                ),
                List.of(),
                List.of()
        );

        mockMvc.perform(put("/api/reports/" + report.getId())
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("key issue")));
    }

    @Test
    @DisplayName("C2-T10, C2-T11: POST /api/reports/{id}/submit changes status to SUBMITTED")
    void shouldSubmitReportSuccessfully() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Report report = reportRepository.save(new Report(memberUser1, activeProject, monday, monday.plusDays(6)));

        mockMvc.perform(post("/api/reports/" + report.getId() + "/submit")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.submittedAt", notNullValue()));

        // Subsequent edit returns 409
        UpdateReportRequest req = new UpdateReportRequest(activeProject.getId(), "After submit", List.of(), List.of(), List.of(), List.of(), List.of());
        mockMvc.perform(put("/api/reports/" + report.getId())
                        .cookie(createAuthCookie(memberUser1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Only draft")));

        // Subsequent submit returns 409
        mockMvc.perform(post("/api/reports/" + report.getId() + "/submit")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C2-T14: Cross-user access returns 404 Not Found (no ID leakage)")
    void shouldPreventCrossUserAccess() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Report aliceReport = reportRepository.save(new Report(memberUser1, activeProject, monday, monday.plusDays(6)));

        // Bob tries to view Alice's report -> 404
        mockMvc.perform(get("/api/reports/" + aliceReport.getId())
                        .cookie(createAuthCookie(memberUser2)))
                .andExpect(status().isNotFound());

        // Bob tries to edit Alice's report -> 404
        UpdateReportRequest req = new UpdateReportRequest(activeProject.getId(), "Hacked", List.of(), List.of(), List.of(), List.of(), List.of());
        mockMvc.perform(put("/api/reports/" + aliceReport.getId())
                        .cookie(createAuthCookie(memberUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        // Bob tries to submit Alice's report -> 404
        mockMvc.perform(post("/api/reports/" + aliceReport.getId() + "/submit")
                        .cookie(createAuthCookie(memberUser2)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("C2-T12: GET /api/reports returns only current user's reports with pagination")
    void shouldReturnOnlyOwnReportsWithFiltering() throws Exception {
        LocalDate week1 = LocalDate.of(2026, 9, 7);
        LocalDate week2 = LocalDate.of(2026, 9, 14);

        Report r1 = reportRepository.save(new Report(memberUser1, activeProject, week1, week1.plusDays(6)));
        r1.setStatus(ReportStatus.SUBMITTED);
        reportRepository.save(r1);

        Report r2 = reportRepository.save(new Report(memberUser1, activeProject, week2, week2.plusDays(6)));
        // r2 is DRAFT

        // Bob's report
        reportRepository.save(new Report(memberUser2, activeProject, week1, week1.plusDays(6)));

        // Alice fetches her reports -> only gets 2, not Bob's
        mockMvc.perform(get("/api/reports")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        // Alice filters by status=SUBMITTED -> only r1
        mockMvc.perform(get("/api/reports?status=SUBMITTED")
                        .cookie(createAuthCookie(memberUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(r1.getId().intValue())));
    }
}
