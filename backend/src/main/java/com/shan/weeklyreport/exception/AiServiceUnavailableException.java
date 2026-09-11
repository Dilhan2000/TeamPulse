package com.shan.weeklyreport.exception;

/**
 * Thrown when an upstream Anthropic Claude API failure occurs (SRS08 C8-T07).
 * Translates to HTTP 502 Bad Gateway.
 */
public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
