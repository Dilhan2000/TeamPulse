package com.shan.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.Project;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.AssignProjectMemberRequest;
import com.shan.weeklyreport.dto.CreateProjectRequest;
import com.shan.weeklyreport.dto.UpdateProjectRequest;
import com.shan.weeklyreport.dto.UpdateProjectStatusRequest;
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

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ManagerProjectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTeamMemberRepository projectTeamMemberRepository;

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

    private User managerUser;
    private User memberUser;
    private User adminUser;
    private Project sampleProject;

    private static final List<String> SEED_PROJECTS = List.of("Client A", "Internal Tooling", "R&D", "Marketing");

    @BeforeEach
    void setUp() {
        projectTeamMemberRepository.deleteAll();
        reportReviewRepository.deleteAll();
        reportVersionRepository.deleteAll();
        reportRepository.deleteAll();

        // Clean up test projects while preserving and resetting seed projects
        projectRepository.findAll().forEach(p -> {
            if (!SEED_PROJECTS.contains(p.getName())) {
                projectRepository.delete(p);
            } else {
                p.setActive(true);
                p.setDescription("Seed project description");
                projectRepository.save(p);
            }
        });

        // Ensure all seed projects exist and are active
        for (String seedName : SEED_PROJECTS) {
            if (projectRepository.findAll().stream().noneMatch(p -> p.getName().equals(seedName))) {
                projectRepository.save(new Project(seedName, "Seed project description", true));
            }
        }

        sampleProject = projectRepository.findAll().stream()
                .filter(p -> p.getName().equals("Client A"))
                .findFirst()
                .orElseThrow();

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
                "manager-proj@test.com",
                passwordEncoder.encode("Pass123!"),
                Role.MANAGER,
                AccountStatus.ACTIVE
        ));

        memberUser = userRepository.save(new User(
                "Bob Member",
                "member-proj@test.com",
                passwordEncoder.encode("Pass123!"),
                Role.TEAM_MEMBER,
                AccountStatus.ACTIVE
        ));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
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
    @DisplayName("C5-T07: RBAC - Non-manager cannot access manager project endpoints")
    void rbacEnforcementOnManagerProjects() throws Exception {
        Cookie memberCookie = getAuthCookie(memberUser);
        Cookie adminCookie = getAuthCookie(adminUser);

        // GET
        mockMvc.perform(get("/api/manager/projects").cookie(memberCookie))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/projects").cookie(adminCookie))
                .andExpect(status().isForbidden());

        // POST
        CreateProjectRequest createReq = new CreateProjectRequest("Forbidden Project", "desc");
        mockMvc.perform(post("/api/manager/projects")
                        .cookie(memberCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // PUT
        UpdateProjectRequest updateReq = new UpdateProjectRequest("Forbidden Project", "desc");
        mockMvc.perform(put("/api/manager/projects/" + sampleProject.getId())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // PATCH status
        UpdateProjectStatusRequest statusReq = new UpdateProjectStatusRequest(false);
        mockMvc.perform(patch("/api/manager/projects/" + sampleProject.getId() + "/status")
                        .cookie(memberCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C5-T03 & C5-T07: Create project happy path and duplicate name 409 conflict")
    void createProjectAndDuplicateHandling() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        CreateProjectRequest validReq = new CreateProjectRequest("Project Custom Alpha", "Beta Description");
        mockMvc.perform(post("/api/manager/projects")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Project Custom Alpha")))
                .andExpect(jsonPath("$.description", is("Beta Description")))
                .andExpect(jsonPath("$.active", is(true)));

        // Duplicate name (same name)
        mockMvc.perform(post("/api/manager/projects")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // Duplicate name (different case or with whitespace)
        CreateProjectRequest caseDup = new CreateProjectRequest("  project custom alpha  ", "Another desc");
        mockMvc.perform(post("/api/manager/projects")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseDup)))
                .andExpect(status().isConflict());

        // Validation error on blank name
        CreateProjectRequest blankReq = new CreateProjectRequest("", "desc");
        mockMvc.perform(post("/api/manager/projects")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("C5-T04 & C5-T07: Update project happy path, no-op rename, and collision 409")
    void updateProjectAndRenameCollision() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        // Update Client A's description and name
        UpdateProjectRequest updateReq = new UpdateProjectRequest("Client A Revised", "Revised desc");
        mockMvc.perform(put("/api/manager/projects/" + sampleProject.getId())
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Client A Revised")))
                .andExpect(jsonPath("$.description", is("Revised desc")));

        // No-op rename (same name) succeeds
        mockMvc.perform(put("/api/manager/projects/" + sampleProject.getId())
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // Collision with existing "Internal Tooling" returns 409
        UpdateProjectRequest collisionReq = new UpdateProjectRequest("Internal Tooling", "Colliding name");
        mockMvc.perform(put("/api/manager/projects/" + sampleProject.getId())
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collisionReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("C5-T05 & C5-T08: Soft-delete project with existing reports does not fail or corrupt reports")
    void softDeleteProjectReferencedByReports() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);
        Cookie memberCookie = getAuthCookie(memberUser);

        // Create a report referencing sampleProject
        LocalDate monday = LocalDate.of(2026, 9, 7);
        Report report = new Report(memberUser, sampleProject, monday, monday.plusDays(6));
        report = reportRepository.save(report);

        // Deactivate sampleProject via manager endpoint
        UpdateProjectStatusRequest statusReq = new UpdateProjectStatusRequest(false);
        mockMvc.perform(patch("/api/manager/projects/" + sampleProject.getId() + "/status")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        // Verify report is unaffected and still references sampleProject
        Report fetchedReport = reportRepository.findById(report.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(sampleProject.getId(), fetchedReport.getProject().getId());

        // Verify public dropdown endpoint GET /api/projects?activeOnly=true excludes deactivated project (3 remaining)
        mockMvc.perform(get("/api/projects?activeOnly=true").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", not(hasItem("Client A"))));

        // But activeOnly=false still returns it (4 total)
        mockMvc.perform(get("/api/projects?activeOnly=false").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].name", hasItem("Client A")));
    }

    @Test
    @DisplayName("C5-T06 & C5-T07: Manager paginated search and active filter")
    void managerSearchAndActiveFilter() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);
        projectRepository.save(new Project("Project Zeta Inactive", "Beta desc", false));
        projectRepository.save(new Project("Project Zeta Active", "Gamma desc", true));

        // Search "Zeta"
        mockMvc.perform(get("/api/manager/projects?search=zeta").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Project Zeta Inactive", "Project Zeta Active")));

        // Search "Zeta" with active=true filter
        mockMvc.perform(get("/api/manager/projects?search=zeta&active=true").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Project Zeta Active")));

        // Search "Zeta" with active=false filter
        mockMvc.perform(get("/api/manager/projects?search=zeta&active=false").cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Project Zeta Inactive")));
    }

    @Test
    @DisplayName("C5-B02 to C5-B05: Project-member assignment, duplicate handling, role validation, and removal")
    void projectTeamMemberAssignmentLifecycle() throws Exception {
        Cookie managerCookie = getAuthCookie(managerUser);

        // Assign memberUser to sampleProject
        AssignProjectMemberRequest assignReq = new AssignProjectMemberRequest(memberUser.getId());
        mockMvc.perform(post("/api/manager/projects/" + sampleProject.getId() + "/members")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isCreated());

        // Verify member appears in GET members
        mockMvc.perform(get("/api/manager/projects/" + sampleProject.getId() + "/members")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(memberUser.getId().intValue())))
                .andExpect(jsonPath("$[0].fullName", is("Bob Member")));

        // Duplicate assignment returns 409
        mockMvc.perform(post("/api/manager/projects/" + sampleProject.getId() + "/members")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isConflict());

        // Assigning non-TEAM_MEMBER (e.g. Admin) returns 400
        AssignProjectMemberRequest invalidRoleReq = new AssignProjectMemberRequest(adminUser.getId());
        mockMvc.perform(post("/api/manager/projects/" + sampleProject.getId() + "/members")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoleReq)))
                .andExpect(status().isBadRequest());

        // Delete / Unassign member
        mockMvc.perform(delete("/api/manager/projects/" + sampleProject.getId() + "/members/" + memberUser.getId())
                        .cookie(managerCookie))
                .andExpect(status().isNoContent());

        // List is now empty
        mockMvc.perform(get("/api/manager/projects/" + sampleProject.getId() + "/members")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Unassigning again returns 404
        mockMvc.perform(delete("/api/manager/projects/" + sampleProject.getId() + "/members/" + memberUser.getId())
                        .cookie(managerCookie))
                .andExpect(status().isNotFound());
    }
}
