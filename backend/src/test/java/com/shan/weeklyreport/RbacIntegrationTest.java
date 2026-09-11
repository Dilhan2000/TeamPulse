package com.shan.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.RefreshToken;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.ApproveUserRequest;
import com.shan.weeklyreport.dto.LoginRequest;
import com.shan.weeklyreport.dto.RegisterRequest;
import com.shan.weeklyreport.dto.UpdateUserRoleRequest;
import com.shan.weeklyreport.dto.UpdateUserStatusRequest;
import com.shan.weeklyreport.repository.RefreshTokenRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.shan.weeklyreport.repository.ReportRepository reportRepository;

    @Autowired
    private com.shan.weeklyreport.repository.ReportReviewRepository reportReviewRepository;

    @Autowired
    private com.shan.weeklyreport.repository.ReportVersionRepository reportVersionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User adminUser;
    private User memberUser;
    private User managerUser;
    private User pendingUser;

    @BeforeEach
    void setUp() {
        reportReviewRepository.deleteAll();
        reportVersionRepository.deleteAll();
        reportRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        // Delete non-admin test users to keep test clean
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

        memberUser = userRepository.save(new User(
                "John Member",
                "member@weeklyreport.local",
                passwordEncoder.encode("Password123!"),
                Role.TEAM_MEMBER,
                AccountStatus.ACTIVE
        ));

        managerUser = userRepository.save(new User(
                "Mary Manager",
                "manager@weeklyreport.local",
                passwordEncoder.encode("Password123!"),
                Role.MANAGER,
                AccountStatus.ACTIVE
        ));

        pendingUser = userRepository.save(new User(
                "Peter Pending",
                "pending@weeklyreport.local",
                passwordEncoder.encode("Password123!"),
                Role.TEAM_MEMBER,
                AccountStatus.PENDING_APPROVAL
        ));
    }

    private Cookie createAuthCookie(User user) {
        String token = jwtService.generateAccessToken(user);
        return new Cookie(AuthCookieFactory.ACCESS_TOKEN_COOKIE, token);
    }

    // ─── Registration Tests (C1-T11) ──────────────────────────────────────────

    @Test
    @DisplayName("C1-T11: Successful registration creates PENDING_APPROVAL user")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest req = new RegisterRequest("New Dev", "newdev@example.com", "Password123!", Role.TEAM_MEMBER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", containsString("Registration submitted")));

        User saved = userRepository.findByEmail("newdev@example.com").orElse(null);
        assertNotNull(saved);
        assertEquals(AccountStatus.PENDING_APPROVAL, saved.getStatus());
        assertEquals(Role.TEAM_MEMBER, saved.getRole());
    }

    @Test
    @DisplayName("C1-T11: Registration with existing email returns 409 Conflict")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        RegisterRequest req = new RegisterRequest("Duplicate Member", "member@weeklyreport.local", "Password123!", Role.TEAM_MEMBER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("C1-T11: Registration with requestedRole ADMIN returns 400 Bad Request")
    void shouldRejectAdminRegistration() throws Exception {
        RegisterRequest req = new RegisterRequest("Hacker Admin", "hacker@example.com", "Password123!", Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("C1-T11: Registration with weak password returns 400 Bad Request")
    void shouldRejectWeakPasswordRegistration() throws Exception {
        RegisterRequest req = new RegisterRequest("Weak Pass", "weak@example.com", "simple", Role.TEAM_MEMBER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password must be at least 8 characters")));
    }

    // ─── Login Tests (C1-T12) ────────────────────────────────────────────────

    @Test
    @DisplayName("C1-T12: Login with seeded admin credentials succeeds and sets cookies")
    void shouldLoginSeededAdminSuccessfully() throws Exception {
        LoginRequest req = new LoginRequest("admin@weeklyreport.local", "ChangeMe123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AuthCookieFactory.ACCESS_TOKEN_COOKIE))
                .andExpect(cookie().exists(AuthCookieFactory.REFRESH_TOKEN_COOKIE))
                .andExpect(cookie().httpOnly(AuthCookieFactory.ACCESS_TOKEN_COOKIE, true))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.email", is("admin@weeklyreport.local")))
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie(AuthCookieFactory.REFRESH_TOKEN_COOKIE);
        assertNotNull(refreshCookie);
        String tokenHash = jwtService.hashToken(refreshCookie.getValue());
        assertTrue(refreshTokenRepository.findByTokenHash(tokenHash).isPresent());
    }

    @Test
    @DisplayName("C1-T12: Login with invalid password returns 401 Unauthorized")
    void shouldRejectInvalidPassword() throws Exception {
        LoginRequest req = new LoginRequest("admin@weeklyreport.local", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password.")))
                .andExpect(cookie().doesNotExist(AuthCookieFactory.ACCESS_TOKEN_COOKIE));
    }

    @Test
    @DisplayName("C1-T12: Login with PENDING_APPROVAL user returns 403 with specific message")
    void shouldRejectPendingUserLogin() throws Exception {
        LoginRequest req = new LoginRequest("pending@weeklyreport.local", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Your account is pending administrator approval.")))
                .andExpect(cookie().doesNotExist(AuthCookieFactory.ACCESS_TOKEN_COOKIE));
    }

    @Test
    @DisplayName("C1-T12: Login with DISABLED user returns 403 with specific message")
    void shouldRejectDisabledUserLogin() throws Exception {
        User disabledUser = userRepository.save(new User(
                "Disabled User",
                "disabled@example.com",
                passwordEncoder.encode("Password123!"),
                Role.TEAM_MEMBER,
                AccountStatus.DISABLED
        ));

        LoginRequest req = new LoginRequest("disabled@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Your account has been disabled. Contact an administrator.")));
    }

    // ─── Current User /me Tests (C1-T15) ─────────────────────────────────────

    @Test
    @DisplayName("C1-T15: Unauthenticated GET /api/auth/me returns 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("C1-T15: Authenticated GET /api/auth/me returns user profile")
    void shouldReturnUserProfileWhenAuthenticated() throws Exception {
        Cookie authCookie = createAuthCookie(memberUser);

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(memberUser.getId().intValue())))
                .andExpect(jsonPath("$.email", is("member@weeklyreport.local")))
                .andExpect(jsonPath("$.role", is("TEAM_MEMBER")));
    }

    // ─── Refresh and Rotation Tests (C1-T14) ──────────────────────────────────

    @Test
    @DisplayName("C1-T14: Refresh endpoint rotates token and prevents reuse of old token")
    void shouldRotateRefreshTokenAndPreventReuse() throws Exception {
        String rawRefresh = jwtService.generateRefreshTokenValue();
        String tokenHash = jwtService.hashToken(rawRefresh);
        RefreshToken rt = new RefreshToken(memberUser, tokenHash, LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(rt);

        Cookie refreshCookie = new Cookie(AuthCookieFactory.REFRESH_TOKEN_COOKIE, rawRefresh);

        // First refresh: must succeed
        MvcResult firstRefresh = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AuthCookieFactory.ACCESS_TOKEN_COOKIE))
                .andExpect(cookie().exists(AuthCookieFactory.REFRESH_TOKEN_COOKIE))
                .andReturn();

        // Old token must now be marked revoked
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(oldToken.isRevoked());

        // Reusing the same old refresh token must fail with 401
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    // ─── Logout Tests (C1-T13) ───────────────────────────────────────────────

    @Test
    @DisplayName("C1-T13: Logout revokes refresh token and clears cookies")
    void shouldLogoutAndClearCookies() throws Exception {
        String rawRefresh = jwtService.generateRefreshTokenValue();
        String tokenHash = jwtService.hashToken(rawRefresh);
        RefreshToken rt = new RefreshToken(memberUser, tokenHash, LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(rt);

        Cookie accessCookie = createAuthCookie(memberUser);
        Cookie refreshCookie = new Cookie(AuthCookieFactory.REFRESH_TOKEN_COOKIE, rawRefresh);

        mockMvc.perform(post("/api/auth/logout").cookie(accessCookie, refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthCookieFactory.ACCESS_TOKEN_COOKIE, 0))
                .andExpect(cookie().maxAge(AuthCookieFactory.REFRESH_TOKEN_COOKIE, 0));

        RefreshToken revokedToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(revokedToken.isRevoked());
    }

    // ─── RBAC & Admin Endpoint Tests (C1-T16, C1-T17, C1-T19) ───────────────

    @Test
    @DisplayName("C1-T19: Non-admin users are rejected with 403 from /api/admin/**")
    void shouldRejectNonAdminAccessToAdminEndpoints() throws Exception {
        Cookie memberCookie = createAuthCookie(memberUser);
        Cookie managerCookie = createAuthCookie(managerUser);

        mockMvc.perform(get("/api/admin/users").cookie(memberCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        mockMvc.perform(get("/api/admin/users").cookie(managerCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("C1-T19: Admin can access /api/admin/users")
    void shouldAllowAdminAccessToAdminEndpoints() throws Exception {
        Cookie adminCookie = createAuthCookie(adminUser);

        mockMvc.perform(get("/api/admin/users").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("C1-T16: Admin approves pending user with optional role override")
    void shouldApprovePendingUserWithRoleOverride() throws Exception {
        Cookie adminCookie = createAuthCookie(adminUser);
        ApproveUserRequest req = new ApproveUserRequest(Role.MANAGER);

        mockMvc.perform(post("/api/admin/users/" + pendingUser.getId() + "/approve")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.role", is("MANAGER")));

        User approved = userRepository.findById(pendingUser.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, approved.getStatus());
        assertEquals(Role.MANAGER, approved.getRole());
        assertNotNull(approved.getApprovedBy());
        assertEquals(adminUser.getId(), approved.getApprovedBy().getId());
    }

    @Test
    @DisplayName("C1-T16: Admin rejects pending user")
    void shouldRejectPendingUser() throws Exception {
        Cookie adminCookie = createAuthCookie(adminUser);

        mockMvc.perform(post("/api/admin/users/" + pendingUser.getId() + "/reject")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        User rejected = userRepository.findById(pendingUser.getId()).orElseThrow();
        assertEquals(AccountStatus.REJECTED, rejected.getStatus());
    }

    @Test
    @DisplayName("C1-T17: Admin cannot disable own account")
    void shouldPreventAdminFromDisablingOwnAccount() throws Exception {
        Cookie adminCookie = createAuthCookie(adminUser);
        UpdateUserStatusRequest req = new UpdateUserStatusRequest(AccountStatus.DISABLED);

        mockMvc.perform(patch("/api/admin/users/" + adminUser.getId() + "/status")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot modify status of your own account")));
    }

    @Test
    @DisplayName("C1-T17: Admin cannot promote user to ADMIN")
    void shouldPreventPromotingUserToAdmin() throws Exception {
        Cookie adminCookie = createAuthCookie(adminUser);
        UpdateUserRoleRequest req = new UpdateUserRoleRequest(Role.ADMIN);

        mockMvc.perform(patch("/api/admin/users/" + memberUser.getId() + "/role")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot promote an account to ADMIN")));
    }
}
