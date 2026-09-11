package com.shan.weeklyreport;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.config.AiConfig;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.ai.ChatMessageDto;
import com.shan.weeklyreport.dto.ai.ChatMessageRequest;
import com.shan.weeklyreport.dto.ai.GenerateSummaryRequest;
import com.shan.weeklyreport.repository.*;
import com.shan.weeklyreport.security.JwtService;
import com.shan.weeklyreport.service.ai.AiReportDataService;
import com.shan.weeklyreport.service.ai.AiToolLoopRunner;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ManagerAiChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiConfig aiConfig;

    @Autowired
    private AiReportDataService reportDataService;

    @Autowired
    private ReportReviewRepository reportReviewRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ProjectTeamMemberRepository projectTeamMemberRepository;

    private User manager;
    private User teamMember;
    private User admin;

    private Cookie managerCookie;
    private Cookie memberCookie;
    private Cookie adminCookie;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        reportReviewRepository.deleteAll();
        reportVersionRepository.deleteAll();
        reportRepository.deleteAll();
        projectTeamMemberRepository.deleteAll();

        userRepository.findAll().forEach(u -> {
            if (!"admin@weeklyreport.local".equals(u.getEmail())) {
                userRepository.delete(u);
            }
        });

        admin = userRepository.findByEmail("admin@weeklyreport.local").orElseGet(() -> {
            User a = new User("Admin User", "admin@weeklyreport.local", passwordEncoder.encode("ChangeMe123!"), Role.ADMIN, AccountStatus.ACTIVE);
            return userRepository.save(a);
        });

        manager = new User("Manager Carol", "carol.manager@example.com", passwordEncoder.encode("Password123!"), Role.MANAGER, AccountStatus.ACTIVE);
        manager = userRepository.save(manager);

        teamMember = new User("Member Alice", "alice.member@example.com", passwordEncoder.encode("Password123!"), Role.TEAM_MEMBER, AccountStatus.ACTIVE);
        teamMember = userRepository.save(teamMember);

        managerCookie = new Cookie("access_token", jwtService.generateAccessToken(manager));
        memberCookie = new Cookie("access_token", jwtService.generateAccessToken(teamMember));
        adminCookie = new Cookie("access_token", jwtService.generateAccessToken(admin));
    }

    @Test
    @DisplayName("C8-T06: GET /availability returns false when AI is disabled in test profile")
    void availabilityReturnsFalseWhenDisabled() throws Exception {
        mockMvc.perform(get("/api/manager/ai-chat/availability")
                        .cookie(managerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    @DisplayName("C8-T06: Non-MANAGER roles (TEAM_MEMBER, ADMIN) receive 403 Forbidden")
    void nonManagerRolesGetForbidden() throws Exception {
        // TEAM_MEMBER
        mockMvc.perform(get("/api/manager/ai-chat/availability")
                        .cookie(memberCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/manager/ai-chat/message")
                        .cookie(memberCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatMessageRequest(List.of(), "Hello"))))
                .andExpect(status().isForbidden());

        // ADMIN
        mockMvc.perform(get("/api/manager/ai-chat/availability")
                        .cookie(adminCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C8-T06: POST /message and /summary return 503 when AI assistant is disabled")
    void messageAndSummaryReturn503WhenDisabled() throws Exception {
        ChatMessageRequest chatReq = new ChatMessageRequest(
                List.of(new ChatMessageDto("user", "What were last week's blockers?")),
                "Please list them"
        );

        mockMvc.perform(post("/api/manager/ai-chat/message")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", is("AI assistant is not configured")));

        GenerateSummaryRequest summaryReq = new GenerateSummaryRequest(LocalDate.of(2026, 3, 2), null);

        mockMvc.perform(post("/api/manager/ai-chat/summary")
                        .cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(summaryReq)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", is("AI assistant is not configured")));
    }

    @Test
    @DisplayName("C8-T07: Upstream API failure returns clean 502 without leaking raw error")
    void upstreamErrorReturns502() {
        AnthropicClient mockClient = Mockito.mock(AnthropicClient.class);
        when(mockClient.messages()).thenThrow(new RuntimeException("Connection timeout to api.anthropic.com"));

        // Temporarily enable AI in config for runner
        ReflectionTestUtils.setField(aiConfig, "enabled", true);

        AiToolLoopRunner runner = new AiToolLoopRunner(aiConfig, reportDataService, objectMapper, mockClient);

        try {
            org.junit.jupiter.api.Assertions.assertThrows(
                    com.shan.weeklyreport.exception.AiServiceUnavailableException.class,
                    () -> runner.run("System prompt", List.of(MessageParam.builder().role(MessageParam.Role.USER).content("Hello").build()))
            );
        } finally {
            ReflectionTestUtils.setField(aiConfig, "enabled", false);
        }
    }

    @Test
    @DisplayName("C8-T03: Tool loop terminates when iteration cap is reached")
    void toolLoopTerminatesAtIterationCap() {
        AnthropicClient mockClient = Mockito.mock(AnthropicClient.class);
        com.anthropic.services.blocking.MessageService mockMessageService = Mockito.mock(com.anthropic.services.blocking.MessageService.class);
        when(mockClient.messages()).thenReturn(mockMessageService);

        // ToolUseBlock stub
        ToolUseBlock mockToolUse = ToolUseBlock.builder()
                .id("tool_u_123")
                .name("get_team_reports_data")
                .caller(DirectCaller.builder().build())
                .input(com.anthropic.core.JsonValue.from(java.util.Map.of("weekStartDate", "2026-03-02")))
                .build();

        Message stubbedToolResponse = Mockito.mock(Message.class);
        when(stubbedToolResponse.content()).thenReturn(List.of(ContentBlock.ofToolUse(mockToolUse)));
        when(stubbedToolResponse.toParam()).thenReturn(
                MessageParam.builder().role(MessageParam.Role.ASSISTANT).content("using tool").build()
        );

        when(mockMessageService.create(any(MessageCreateParams.class))).thenReturn(stubbedToolResponse);

        ReflectionTestUtils.setField(aiConfig, "enabled", true);
        AiToolLoopRunner runner = new AiToolLoopRunner(aiConfig, reportDataService, objectMapper, mockClient);

        try {
            String result = runner.run("System prompt", List.of(MessageParam.builder().role(MessageParam.Role.USER).content("Query").build()));
            assertThat(result).contains("maximum inquiry steps");
        } finally {
            ReflectionTestUtils.setField(aiConfig, "enabled", false);
        }
    }
}
