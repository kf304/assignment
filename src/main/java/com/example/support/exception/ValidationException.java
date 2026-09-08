package com.example.support.exception;

/** Thrown when caller-supplied data fails validation. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
