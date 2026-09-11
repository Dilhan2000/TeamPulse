package com.shan.weeklyreport.dto;

/**
 * Request DTO for rejecting a pending user (C1-T10).
 */
public record RejectUserRequest(
        String reason
) {}
