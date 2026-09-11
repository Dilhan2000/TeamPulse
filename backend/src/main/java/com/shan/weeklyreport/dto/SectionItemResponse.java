package com.shan.weeklyreport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Item in the side-by-side section view (C4-T05).
 * Represents either a blocker (with resolved status) or an achievement.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SectionItemResponse(
        String description,
        boolean flagged,
        Boolean resolved
) {
}
