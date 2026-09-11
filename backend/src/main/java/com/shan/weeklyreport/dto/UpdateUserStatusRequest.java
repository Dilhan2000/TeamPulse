package com.shan.weeklyreport.dto;

import com.shan.weeklyreport.common.AccountStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating user status (C1-T10).
 */
public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required")
        AccountStatus status
) {}
