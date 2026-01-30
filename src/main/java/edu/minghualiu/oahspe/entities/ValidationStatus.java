package edu.minghualiu.oahspe.entities;

/**
 * Enum representing the status of a validation request.
 */
public enum ValidationStatus {
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    ValidationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
