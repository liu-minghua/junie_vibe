package edu.minghualiu.oahspe.ingestion.validator;

/**
 * Severity enum for validation issues
 * 
 * Four-level severity system for prioritizing fixes:
 * - CRITICAL: Data invalid, must fix before use
 * - ERROR: Data inconsistent, should fix
 * - WARNING: Data suboptimal, nice to fix
 * - INFO: Informational, no action needed
 * 
 * @author Development Team
 * @version 1.0
 * @since 2026-01-30
 */
public enum Severity {
    CRITICAL("Critical"),
    ERROR("Error"),
    WARNING("Warning"),
    INFO("Info");
    
    private final String displayName;
    
    Severity(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
