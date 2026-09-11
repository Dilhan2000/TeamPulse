package com.shan.weeklyreport.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for toggling project active status (C5-T01).
 */
public record UpdateProjectStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean active
) {
}
