package edu.minghualiu.oahspe.service;

/**
 * Exception thrown when a validation request is not found.
 */
public class ValidationRequestNotFoundException extends RuntimeException {
    public ValidationRequestNotFoundException(String message) {
        super(message);
    }

    public ValidationRequestNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
