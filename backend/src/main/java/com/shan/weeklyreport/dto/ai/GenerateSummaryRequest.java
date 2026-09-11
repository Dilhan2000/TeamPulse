package com.shan.weeklyreport.dto.ai;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request payload for generating a team summary for a specific week (SRS08 C8-T06).
 */
public record GenerateSummaryRequest(
        @NotNull(message = "weekStartDate is required")
        LocalDate weekStartDate,

        Long projectId
) {
}
