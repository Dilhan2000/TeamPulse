package com.shan.weeklyreport.exception;

/**
 * Thrown when a request conflicts with current state of resource (409).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
