package com.shan.weeklyreport.exception;

/**
 * Thrown when AI chat assistant endpoints are accessed while the feature is disabled (SRS08 C8-T06).
 * Translates to HTTP 503 Service Unavailable.
 */
public class AiServiceDisabledException extends RuntimeException {
    public AiServiceDisabledException(String message) {
        super(message);
    }
}
