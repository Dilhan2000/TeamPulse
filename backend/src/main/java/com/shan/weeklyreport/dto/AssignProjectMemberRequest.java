package com.shan.weeklyreport.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for assigning a team member to a project (C5-B03).
 */
public record AssignProjectMemberRequest(
        @NotNull(message = "User ID is required")
        Long userId
) {
}
