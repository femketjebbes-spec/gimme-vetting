package com.gimmevettingsolution.intake.service;

/**
 * Exception thrown when request validation fails.
 */
public class ValidationException extends Exception {

    private final String field;

    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    public ValidationException(String field, String message) {
        super(field + ": " + message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
