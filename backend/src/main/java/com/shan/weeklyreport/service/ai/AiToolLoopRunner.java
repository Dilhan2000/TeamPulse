package com.shan.weeklyreport.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.weeklyreport.config.AiConfig;
import com.shan.weeklyreport.dto.ai.ReportSummaryForAi;
import com.shan.weeklyreport.exception.AiServiceDisabledException;
import com.shan.weeklyreport.exception.AiServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Executes the manual tool-use loop with Anthropic Claude SDK (SRS08 C8-T03).
 */
@Component
public class AiToolLoopRunner {

    private static final Logger log = LoggerFactory.getLogger(AiToolLoopRunner.class);

    private final AiConfig aiConfig;
    private final AiReportDataService reportDataService;
    private final ObjectMapper objectMapper;
    private final AnthropicClient anthropicClient;
    private final Tool reportDataTool;

    @Autowired
    public AiToolLoopRunner(
            AiConfig aiConfig,
            AiReportDataService reportDataService,
            ObjectMapper objectMapper,
            @Autowired(required = false) AnthropicClient anthropicClient) {
        this.aiConfig = aiConfig;
        this.reportDataService = reportDataService;
        this.objectMapper = objectMapper;
        this.anthropicClient = anthropicClient;
        this.reportDataTool = buildReportDataTool();
    }

    private Tool buildReportDataTool() {
        return Tool.builder()
                .name("get_team_reports_data")
                .description("Look up real weekly report data for the team: tasks completed, blockers, and achievements. Always call this before answering any question about team activity — never guess.")
                .inputSchema(Tool.InputSchema.builder()
                        .properties(Tool.InputSchema.Properties.builder()
                                .putAdditionalProperty("weekStartDate", JsonValue.from(Map.of("type", "string", "description", "A single Monday date (YYYY-MM-DD) to query one week")))
                                .putAdditionalProperty("weekStartFrom", JsonValue.from(Map.of("type", "string", "description", "Start date (YYYY-MM-DD) for range queries")))
                                .putAdditionalProperty("weekStartTo", JsonValue.from(Map.of("type", "string", "description", "End date (YYYY-MM-DD) for range queries")))
                                .putAdditionalProperty("projectId", JsonValue.from(Map.of("type", "integer", "description", "Filter by project ID")))
                                .putAdditionalProperty("userId", JsonValue.from(Map.of("type", "integer", "description", "Filter by team member user ID")))
                                .build())
                        .build())
                .build();
    }

    public String run(String systemPrompt, List<MessageParam> initialConversation) {
        if (!aiConfig.isEnabled() || anthropicClient == null) {
            throw new AiServiceDisabledException("AI assistant is not configured");
        }

        List<MessageParam> conversation = new ArrayList<>(initialConversation);
        int maxIterations = aiConfig.getMaxToolIterations();
        int iteration = 0;
        String latestTextResponse = "";

        try {
            while (iteration < maxIterations) {
                iteration++;
                log.debug("AI tool-loop iteration {}/{}", iteration, maxIterations);

                MessageCreateParams params = MessageCreateParams.builder()
                        .model(aiConfig.getModel())
                        .maxTokens(aiConfig.getMaxTokens())
                        .system(systemPrompt)
                        .addTool(reportDataTool)
                        .messages(conversation)
                        .build();

                Message response = anthropicClient.messages().create(params);

                // Collect any text blocks in response
                String currentTurnText = response.content().stream()
                        .filter(ContentBlock::isText)
                        .map(b -> b.asText().text())
                        .collect(Collectors.joining("\n"));

                if (!currentTurnText.isBlank()) {
                    latestTextResponse = currentTurnText;
                }

                // Check for tool use blocks
                List<ToolUseBlock> toolUseBlocks = response.content().stream()
                        .filter(ContentBlock::isToolUse)
                        .map(ContentBlock::asToolUse)
                        .toList();

                if (toolUseBlocks.isEmpty()) {
                    // Turn complete with no tool requests
                    return latestTextResponse;
                }

                // Append assistant turn to history
                conversation.add(response.toParam());

                // Execute each tool request and append results
                List<ContentBlockParam> toolResultBlockParams = new ArrayList<>();
                for (ToolUseBlock toolUse : toolUseBlocks) {
                    String toolName = toolUse.name();
                    if ("get_team_reports_data".equals(toolName)) {
                        String resultJson = executeReportDataQuery(toolUse._input());
                        ToolResultBlockParam toolResult = ToolResultBlockParam.builder()
                                .toolUseId(toolUse.id())
                                .content(resultJson)
                                .build();
                        toolResultBlockParams.add(ContentBlockParam.ofToolResult(toolResult));
                    } else {
                        log.warn("Unknown tool requested: {}", toolName);
                        ToolResultBlockParam errorResult = ToolResultBlockParam.builder()
                                .toolUseId(toolUse.id())
                                .isError(true)
                                .content("Unknown tool: " + toolName)
                                .build();
                        toolResultBlockParams.add(ContentBlockParam.ofToolResult(errorResult));
                    }
                }

                MessageParam userToolResultsMessage = MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .contentOfBlockParams(toolResultBlockParams)
                        .build();

                conversation.add(userToolResultsMessage);
            }

            // If we reached maxIterations without a final response:
            log.warn("AI tool-loop reached max iterations cap ({})", maxIterations);
            return latestTextResponse.isBlank()
                    ? "I gathered team report data but reached the maximum inquiry steps. Please narrow your question for more specific details."
                    : latestTextResponse + "\n\n*(Note: Maximum inquiry steps reached; response may be incomplete.)*";

        } catch (AiServiceDisabledException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Anthropic API error during AI chat execution: {}", ex.getMessage(), ex);
            throw new AiServiceUnavailableException("AI assistant is temporarily unavailable — please try again.", ex);
        }
    }

    private String executeReportDataQuery(JsonValue inputJsonValue) {
        try {
            LocalDate weekStartDate = null;
            LocalDate weekStartFrom = null;
            LocalDate weekStartTo = null;
            Long projectId = null;
            Long userId = null;

            if (inputJsonValue != null) {
                // Parse input using Jackson ObjectMapper safely
                JsonNode root = objectMapper.readTree(inputJsonValue.toString());

                if (root.hasNonNull("weekStartDate")) {
                    weekStartDate = parseLocalDate(root.get("weekStartDate").asText());
                }
                if (root.hasNonNull("weekStartFrom")) {
                    weekStartFrom = parseLocalDate(root.get("weekStartFrom").asText());
                }
                if (root.hasNonNull("weekStartTo")) {
                    weekStartTo = parseLocalDate(root.get("weekStartTo").asText());
                }
                if (root.hasNonNull("projectId")) {
                    projectId = root.get("projectId").asLong();
                }
                if (root.hasNonNull("userId")) {
                    userId = root.get("userId").asLong();
                }
            }

            List<ReportSummaryForAi> reports = reportDataService.queryReports(
                    weekStartDate, weekStartFrom, weekStartTo, projectId, userId
            );

            return objectMapper.writeValueAsString(reports);
        } catch (Exception e) {
            log.error("Failed to parse tool arguments or execute report query", e);
            return "[]";
        }
    }

    private LocalDate parseLocalDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception e) {
            log.debug("Could not parse date string: {}", text);
            return null;
        }
    }
}
