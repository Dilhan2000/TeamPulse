package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.ReportStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportSummaryResponse(
        Long id,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        Long projectId,
        String projectName,
        ReportStatus status,
        LocalDateTime submittedAt
) {}
