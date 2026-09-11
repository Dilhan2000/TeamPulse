package com.shan.weeklyreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestChangesRequest(
        @NotBlank(message = "Comment is required when requesting changes")
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        String comment
) {}
