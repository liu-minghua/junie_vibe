package edu.minghualiu.oahspe.entities;

/**
 * Represents the execution status of a workflow.
 */
public enum WorkflowStatus {
    NOT_STARTED("Not started"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed successfully"),
    FAILED("Failed with errors");
    
    private final String description;
    
    WorkflowStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
