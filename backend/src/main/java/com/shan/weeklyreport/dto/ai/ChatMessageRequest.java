package com.shan.weeklyreport.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request payload for sending a chat message to the AI assistant (SRS08 C8-T06).
 */
public record ChatMessageRequest(
        List<@Valid ChatMessageDto> history,

        @NotBlank(message = "Message must not be blank")
        String message
) {
    public ChatMessageRequest {
        if (history == null) {
            history = List.of();
        }
    }
}
