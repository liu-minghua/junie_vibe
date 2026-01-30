package edu.minghualiu.oahspe.ingestion.validator;

/**
 * ValidationProgressCallback - Observer interface for progress tracking
 * 
 * Implement to track validation progress in UI or logs.
 * Optional - can pass null to validator if progress tracking not needed.
 * 
 * Use Cases:
 * - Progress bar in UI showing current entity type and count
 * - Log messages for async validation jobs
 * - Real-time progress updates in REST endpoints
 * - Collecting validation metrics
 * 
 * Design Principle: Observer pattern enables progress tracking without coupling
 * validator logic to UI/logging concerns.
 * 
 * Null Safety: Validator checks for null callback before invoking methods.
 * 
 * Example Usage:
 * {@code
 * ValidationProgressCallback callback = new ValidationProgressCallback() {
 *     public void onValidationStart(int totalEntities) {
 *         System.out.println("Starting validation of " + totalEntities + " entities");
 *     }
 *     public void onEntityValidated(String entityType, int count) {
 *         progressBar.update(count);
 *     }
 *     public void onValidationComplete(ValidationResult result) {
 *         showResults(result);
 *     }
 * };
 * 
 * ValidationResult result = validator.validateAll(callback);
 * }
 * 
 * @author Development Team
 * @version 1.0
 * @since 2026-01-30
 */
public interface ValidationProgressCallback {
    
    /**
     * Called when validation starts
     * 
     * Use to:
     * - Show progress bar with maximum value
     * - Start progress spinner
     * - Log validation start
     * 
     * @param totalEntities Total number of entities to validate
     */
    void onValidationStart(int totalEntities);
    
    /**
     * Called after batch of entities is validated
     * 
     * Use to:
     * - Update progress bar position
     * - Log progress periodically
     * - Send progress events to UI
     * 
     * @param entityType Type just validated (BOOK, CHAPTER, VERSE, NOTE, IMAGE)
     * @param count Cumulative count of this entity type validated
     */
    void onEntityValidated(String entityType, int count);
    
    /**
     * Called when validation completes
     * 
     * Use to:
     * - Display final results
     * - Enable action buttons
     * - Log completion
     * 
     * @param result Final ValidationResult with all findings
     */
    void onValidationComplete(ValidationResult result);
}
