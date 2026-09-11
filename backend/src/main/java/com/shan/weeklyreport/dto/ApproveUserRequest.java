package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.Role;

/**
 * Request DTO for approving a pending user (C1-T10).
 * role override is optional.
 */
public record ApproveUserRequest(
        Role role
) {}
