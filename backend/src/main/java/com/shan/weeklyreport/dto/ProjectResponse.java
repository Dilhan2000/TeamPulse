package com.shan.weeklyreport.dto;

import java.time.LocalDateTime;

/**
 * Full project details response (C5-T01).
 */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
