package com.shan.weeklyreport.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateReportRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        @NotNull(message = "Week start date is required")
        LocalDate weekStartDate
) {}
