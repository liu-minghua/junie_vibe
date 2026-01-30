package edu.minghualiu.oahspe.ingestion.validator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * ValidationIssue - Represents a single validation problem
 * 
 * Each issue records:
 * - Severity (CRITICAL, ERROR, WARNING, INFO)
 * - What entity had the problem
 * - What rule was violated
 * - Human-readable description
 * - Optional suggestion for fixing
 * 
 * Example issues:
 * - Verse #5 in Chapter 2 missing text content
 * - Note #142 references non-existent verse
 * - Image #8 file path exceeds 500 characters
 * 
 * Design: Immutable data class for safe passing between components
 * 
 * @author Development Team
 * @version 1.0
 * @since 2026-01-30
 */
@Getter
@AllArgsConstructor
@ToString
public class ValidationIssue {
    
    /** Severity level (CRITICAL, ERROR, WARNING, INFO) */
    private final Severity severity;
    
    /** Entity type affected (BOOK, CHAPTER, VERSE, NOTE, IMAGE) */
    private final String entityType;
    
    /** ID of the affected entity */
    private final Long entityId;
    
    /** Name of the rule that was violated (e.g., "VerseSequencing") */
    private final String rule;
    
    /** Human-readable description of the issue */
    private final String message;
    
    /** Optional suggestion for how to fix (null if none) */
    private final String suggestedFix;
    
    /**
     * Create issue without suggested fix
     */
    public ValidationIssue(Severity severity, String entityType, Long entityId, 
                          String rule, String message) {
        this(severity, entityType, entityId, rule, message, null);
    }
    
    /**
     * Format issue as string for display
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity.getDisplayName()).append("] ");
        sb.append(entityType).append("#").append(entityId).append(" ");
        sb.append("(").append(rule).append("): ");
        sb.append(message);
        if (suggestedFix != null) {
            sb.append(" [Fix: ").append(suggestedFix).append("]");
        }
        return sb.toString();
    }
}
