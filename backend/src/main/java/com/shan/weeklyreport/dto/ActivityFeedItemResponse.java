package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.ReviewAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Item in the manager dashboard activity feed (C6-T08).
 */
public record ActivityFeedItemResponse(
        String type,
        String actorName,
        String targetUserName,
        ReviewAction action,
        LocalDate weekStartDate,
        LocalDateTime timestamp,
        Long reportId
) {
}
