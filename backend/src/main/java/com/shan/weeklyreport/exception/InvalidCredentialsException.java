package com.shan.weeklyreport.exception;

/**
 * Thrown when credentials fail verification (401).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
