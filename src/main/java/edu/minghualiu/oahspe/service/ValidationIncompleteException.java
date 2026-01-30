package edu.minghualiu.oahspe.service;

/**
 * Exception thrown when validation is still in progress.
 */
public class ValidationIncompleteException extends RuntimeException {
    public ValidationIncompleteException(String message) {
        super(message);
    }

    public ValidationIncompleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
