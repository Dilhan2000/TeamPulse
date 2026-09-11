package com.shan.weeklyreport.exception;

/**
 * Thrown when attempting to register an email that is already in use (409).
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
