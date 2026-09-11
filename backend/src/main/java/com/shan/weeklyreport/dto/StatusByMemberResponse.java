package com.shan.weeklyreport.dto;

/**
 * Report distribution by status per team member (C6-T05).
 */
public record StatusByMemberResponse(
        Long userId,
        String fullName,
        long draft,
        long submitted,
        long needsCorrection,
        long approved
) {
}
