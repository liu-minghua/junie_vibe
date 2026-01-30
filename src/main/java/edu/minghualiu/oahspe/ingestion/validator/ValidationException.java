package edu.minghualiu.oahspe.ingestion.validator;

/**
 * ValidationException - Validation system error
 * 
 * Thrown when the validation system itself fails (e.g., database error).
 * NOT thrown for data validation failures (those are recorded in ValidationResult).
 * 
 * Distinguishes between:
 * - System errors (DB unreachable) → throw ValidationException
 * - Data errors (invalid verse number) → add to ValidationResult
 * 
 * @author Development Team
 * @version 1.0
 * @since 2026-01-30
 */
public class ValidationException extends RuntimeException {
    
    /**
     * Create validation exception with message
     * 
     * @param message Description of what went wrong
     */
    public ValidationException(String message) {
        super(message);
    }
    
    /**
     * Create validation exception with message and cause
     * 
     * @param message Description of what went wrong
     * @param cause Root cause exception (preserves stack trace)
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
