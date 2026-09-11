package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.ReviewAction;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        int versionNumber,
        ReviewAction action,
        String comment,
        String reviewerName,
        LocalDateTime reviewedAt
) {}
