package com.shan.weeklyreport.dto;

public record ProjectSummaryResponse(
        Long id,
        String name,
        String description,
        boolean active
) {}
