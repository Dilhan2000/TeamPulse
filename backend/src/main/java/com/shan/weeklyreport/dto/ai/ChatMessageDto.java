package com.shan.weeklyreport.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Message exchange DTO for AI chat assistant (SRS08 C8-T04).
 */
public record ChatMessageDto(
        @NotBlank
        @Pattern(regexp = "^(user|assistant)$", message = "Role must be 'user' or 'assistant'")
        String role,

        @NotBlank
        String content
) {
}
