package com.shan.weeklyreport.dto;

import java.time.LocalDateTime;

public record ReportVersionSummaryResponse(
        Long id,
        int versionNumber,
        LocalDateTime submittedAt
) {}
