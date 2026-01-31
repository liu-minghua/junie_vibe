package edu.minghualiu.oahspe.entities;

/**
 * Represents the current phase in the workflow execution.
 */
public enum WorkflowPhase {
    PAGE_LOADING("Loading pages from PDF"),
    CLEANUP("Cleaning up old data"),
    CONTENT_INGESTION("Ingesting content from pages"),
    COMPLETED("Workflow completed");
    
    private final String description;
    
    WorkflowPhase(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
