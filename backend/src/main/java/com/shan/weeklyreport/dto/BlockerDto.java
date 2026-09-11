package com.shan.weeklyreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockerDto(
        Long id,

        @NotBlank(message = "Blocker description is required")
        @Size(max = 500, message = "Blocker description cannot exceed 500 characters")
        String description,

        boolean isKeyIssue,
        boolean resolved,
        int sortOrder
) {}
