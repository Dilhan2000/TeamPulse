package com.shan.weeklyreport.exception;

/**
 * Thrown on invalid client requests (400).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
