package com.shan.weeklyreport.dto;

import jakarta.validation.constraints.Size;

public record ApproveReportRequest(
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        String comment
) {}
