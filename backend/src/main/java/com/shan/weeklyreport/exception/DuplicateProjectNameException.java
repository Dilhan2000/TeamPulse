package com.shan.weeklyreport.exception;

/**
 * Thrown when a project with the given name already exists (409).
 */
public class DuplicateProjectNameException extends ConflictException {
    public DuplicateProjectNameException(String message) {
        super(message);
    }
}
