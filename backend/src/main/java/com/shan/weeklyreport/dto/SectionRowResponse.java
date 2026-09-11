package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.ReportStatus;

import java.util.List;

/**
 * Row in the side-by-side section view (C4-T05).
 * Represents a qualifying team member's report and their items for that section.
 */
public record SectionRowResponse(
        Long userId,
        String fullName,
        String projectName,
        Long reportId,
        ReportStatus reportStatus,
        List<SectionItemResponse> items
) {
}
