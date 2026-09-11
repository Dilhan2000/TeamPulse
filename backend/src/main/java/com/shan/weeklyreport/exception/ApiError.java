package com.shan.weeklyreport.exception;

import java.time.Instant;

/**
 * Standard error response shape returned by {@link GlobalExceptionHandler}
 * for every non-2xx response from the API.
 *
 * Shape: { timestamp, status, error, message, path }
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}
