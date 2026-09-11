package com.shan.weeklyreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AchievementDto(
        Long id,

        @NotBlank(message = "Achievement description is required")
        @Size(max = 500, message = "Achievement description cannot exceed 500 characters")
        String description,

        boolean isKeyAchievement,
        int sortOrder
) {}
