package com.shan.weeklyreport.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageParam;
import com.shan.weeklyreport.config.AiConfig;
import com.shan.weeklyreport.dto.ai.ChatMessageDto;
import com.shan.weeklyreport.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level AI assistant service for conversational Q&A and team summary generation (SRS08 C8-T04, C8-T05).
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are an assistant for engineering managers reviewing their team's weekly reports. \
            Use the get_team_reports_data tool to look up real data before answering any question about team activity. \
            Only discuss what the tool actually returns — never speculate about work that isn't reflected in the data. \
            Be structured, clear, and professional.\
            """;

    private final AiConfig aiConfig;
    private final AiToolLoopRunner toolLoopRunner;
    private final ProjectRepository projectRepository;
    private final AnthropicClient anthropicClient;

    @Autowired
    public AiChatService(
            AiConfig aiConfig,
            AiToolLoopRunner toolLoopRunner,
            ProjectRepository projectRepository,
            @Autowired(required = false) AnthropicClient anthropicClient) {
        this.aiConfig = aiConfig;
        this.toolLoopRunner = toolLoopRunner;
        this.projectRepository = projectRepository;
        this.anthropicClient = anthropicClient;
    }

    public boolean isAvailable() {
        return aiConfig.isEnabled() && anthropicClient != null;
    }

    /**
     * Multi-turn conversational Q&A for managers (C8-T04).
     */
    public String chat(Long managerId, List<ChatMessageDto> history, String newMessage) {
        log.info("Manager {} sending message to AI Assistant", managerId);

        List<MessageParam> conversation = new ArrayList<>();

        if (history != null) {
            for (ChatMessageDto msg : history) {
                if ("user".equalsIgnoreCase(msg.role())) {
                    conversation.add(MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .content(msg.content())
                            .build());
                } else if ("assistant".equalsIgnoreCase(msg.role())) {
                    conversation.add(MessageParam.builder()
                            .role(MessageParam.Role.ASSISTANT)
                            .content(msg.content())
                            .build());
                }
            }
        }

        conversation.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(newMessage)
                .build());

        return toolLoopRunner.run(DEFAULT_SYSTEM_PROMPT, conversation);
    }

    /**
     * One-click weekly team summary generation (C8-T05).
     */
    public String generateSummary(Long managerId, LocalDate weekStartDate, Long projectId) {
        log.info("Manager {} generating team summary for week {} (project: {})", managerId, weekStartDate, projectId);

        String projectClause = "";
        if (projectId != null) {
            String projectName = projectRepository.findById(projectId)
                    .map(p -> p.getName())
                    .orElse("ID " + projectId);
            projectClause = ", for project " + projectName;
        }

        String openingUserMessage = String.format(
                "Generate a concise summary of the team's activity for the week of %s%s. " +
                "Highlight: completed work, recurring blockers, and any workload imbalances you notice across team members.",
                weekStartDate, projectClause
        );

        List<MessageParam> singleMessageList = List.of(
                MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content(openingUserMessage)
                        .build()
        );

        return toolLoopRunner.run(DEFAULT_SYSTEM_PROMPT, singleMessageList);
    }
}
