package edu.minghualiu.oahspe.ingestion.validator;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * ValidationResult - Aggregated validation findings and metrics
 * 
 * Collects all validation issues and provides:
 * - Metrics (total entities checked, total issues, breakdown by severity/type)
 * - Filtering (issues by severity or entity type)
 * - Status queries (isValid, hasCriticalIssues, etc.)
 * - Reporting (summary or detailed reports)
 * 
 * Design Principle: Result container that organizes issues for user action
 * Issues are organized in two ways:
 * - By severity (CRITICAL, ERROR, WARNING, INFO)
 * - By entity type (BOOK, CHAPTER, VERSE, NOTE, IMAGE)
 * 
 * This makes it easy for users to:
 * - Fix critical issues first
 * - Generate reports by entity type
 * - Filter issues by severity level
 * - Determine if validation passed (isValid())
 * 
 * @author Development Team
 * @version 1.0
 * @since 2026-01-30
 */
@Getter
public class ValidationResult {
    
    private int totalEntitiesChecked = 0;
    private int totalIssuesFound = 0;
    
    @Setter
    private long elapsedTimeMs = 0;
    
    private final Map<Severity, List<ValidationIssue>> issuesBySeverity = new HashMap<>();
    private final Map<String, List<ValidationIssue>> issuesByEntityType = new HashMap<>();
    private final Map<String, Integer> successfulValidations = new HashMap<>();
    
    private final List<ValidationIssue> allIssues = new ArrayList<>();
    
    /**
     * Initialize severity map with empty lists
     */
    public ValidationResult() {
        for (Severity severity : Severity.values()) {
            issuesBySeverity.put(severity, new ArrayList<>());
        }
    }
    
    /**
     * Add a validation issue to results
     * 
     * @param issue Issue to add
     */
    public void addIssue(ValidationIssue issue) {
        if (issue == null) return;
        
        allIssues.add(issue);
        totalIssuesFound++;
        
        // Organize by severity
        issuesBySeverity.get(issue.getSeverity()).add(issue);
        
        // Organize by entity type
        issuesByEntityType.computeIfAbsent(issue.getEntityType(), k -> new ArrayList<>())
            .add(issue);
    }
    
    /**
     * Record that N entities of type were validated
     * 
     * @param entityType Type that was validated
     * @param count Number of entities of that type
     */
    public void recordValidation(String entityType, int count) {
        totalEntitiesChecked += count;
        successfulValidations.put(entityType, count);
    }
    
    /**
     * Get all issues (in order added)
     * 
     * @return List of all ValidationIssue objects
     */
    public List<ValidationIssue> getAllIssues() {
        return new ArrayList<>(allIssues);
    }
    
    /**
     * Get issues filtered by severity
     * 
     * @param severity Severity level to filter by
     * @return List of issues with that severity (empty if none)
     */
    public List<ValidationIssue> getIssuesBySeverity(Severity severity) {
        return new ArrayList<>(issuesBySeverity.getOrDefault(severity, new ArrayList<>()));
    }
    
    /**
     * Get issues filtered by entity type
     * 
     * @param entityType Type to filter by (BOOK, CHAPTER, VERSE, NOTE, IMAGE)
     * @return List of issues for that entity type (empty if none)
     */
    public List<ValidationIssue> getIssuesByEntityType(String entityType) {
        return new ArrayList<>(issuesByEntityType.getOrDefault(entityType, new ArrayList<>()));
    }
    
    /**
     * Get critical issues only
     * 
     * @return List of CRITICAL severity issues
     */
    public List<ValidationIssue> getCriticalIssues() {
        return getIssuesBySeverity(Severity.CRITICAL);
    }
    
    /**
     * Get error issues only
     * 
     * @return List of ERROR severity issues
     */
    public List<ValidationIssue> getErrorIssues() {
        return getIssuesBySeverity(Severity.ERROR);
    }
    
    /**
     * Get warning issues only
     * 
     * @return List of WARNING severity issues
     */
    public List<ValidationIssue> getWarningIssues() {
        return getIssuesBySeverity(Severity.WARNING);
    }
    
    /**
     * Check if validation passed (no CRITICAL or ERROR issues)
     * 
     * @return true if no CRITICAL or ERROR issues found
     */
    public boolean isValid() {
        return getCriticalIssues().isEmpty() && getErrorIssues().isEmpty();
    }
    
    /**
     * Check if any CRITICAL issues exist
     * 
     * @return true if CRITICAL issues found
     */
    public boolean hasCriticalIssues() {
        return !getCriticalIssues().isEmpty();
    }
    
    /**
     * Check if any ERROR issues exist (or CRITICAL)
     * 
     * @return true if ERROR or CRITICAL issues found
     */
    public boolean hasErrors() {
        return !getErrorIssues().isEmpty() || hasCriticalIssues();
    }
    
    /**
     * Check if any WARNING issues exist (or worse)
     * 
     * @return true if WARNING, ERROR, or CRITICAL issues found
     */
    public boolean hasWarnings() {
        return !getWarningIssues().isEmpty() || hasErrors();
    }
    
    /**
     * Check if any issues exist at all
     * 
     * @return true if any issues found
     */
    public boolean hasIssues() {
        return totalIssuesFound > 0;
    }
    
    /**
     * Get concise summary (3-5 lines)
     * 
     * Example output:
     * Validation Results:
     *   - Total entities checked: 5,432
     *   - Total issues found: 12
     *   - Critical: 0 | Errors: 3 | Warnings: 9
     *   - Status: ✅ VALID (no critical/error issues)
     * 
     * @return String summary suitable for console/log output
     */
    public String getIssuesSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation Results:\n");
        sb.append(String.format("  - Total entities checked: %,d\n", totalEntitiesChecked));
        sb.append(String.format("  - Total issues found: %,d\n", totalIssuesFound));
        
        int critical = getCriticalIssues().size();
        int errors = getErrorIssues().size();
        int warnings = getWarningIssues().size();
        sb.append(String.format("  - Critical: %d | Errors: %d | Warnings: %d\n", 
                               critical, errors, warnings));
        
        String status = isValid() ? "✅ VALID (no critical/error issues)" : 
                       "❌ INVALID (has critical or error issues)";
        sb.append(String.format("  - Status: %s", status));
        
        return sb.toString();
    }
    
    /**
     * Get detailed report with all issues organized by severity/entity
     * 
     * Example output:
     * === VALIDATION DETAILED REPORT ===
     * 
     * CRITICAL ISSUES (0):
     * [None]
     * 
     * ERROR ISSUES (3):
     * 1. Verse #5 in Chapter 2: Verse number gap (expected 5, found 6)
     * 2. Note #142: References non-existent Verse #9999
     * 3. Image #8: References non-existent Note
     * 
     * WARNING ISSUES (9):
     * 1. Chapter 5: Contains only 1 verse (typically >5)
     * ...
     * 
     * @return String detailed report
     */
    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== VALIDATION DETAILED REPORT ===\n\n");
        
        // Critical issues
        reportBySeverity(sb, Severity.CRITICAL);
        sb.append("\n");
        
        // Error issues
        reportBySeverity(sb, Severity.ERROR);
        sb.append("\n");
        
        // Warning issues
        reportBySeverity(sb, Severity.WARNING);
        sb.append("\n");
        
        // Info issues
        reportBySeverity(sb, Severity.INFO);
        
        return sb.toString();
    }
    
    /**
     * Report issues by severity
     */
    private void reportBySeverity(StringBuilder sb, Severity severity) {
        List<ValidationIssue> issues = getIssuesBySeverity(severity);
        
        sb.append(severity.getDisplayName().toUpperCase()).append(" ISSUES (")
            .append(issues.size()).append("):\n");
        
        if (issues.isEmpty()) {
            sb.append("[None]\n");
        } else {
            for (int i = 0; i < issues.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, issues.get(i).toString()));
            }
        }
    }
    
    /**
     * Get metrics as JSON-formatted string
     * 
     * Example output:
     * {
     *   "totalEntitiesChecked": 5432,
     *   "totalIssuesFound": 12,
     *   "issuesBySeverity": {
     *     "CRITICAL": 0,
     *     "ERROR": 3,
     *     "WARNING": 9,
     *     "INFO": 0
     *   },
     *   "issuesByEntity": {
     *     "VERSE": 5,
     *     "NOTE": 2,
     *     "IMAGE": 1,
     *     "CHAPTER": 4
     *   },
     *   "elapsedTimeMs": 2450,
     *   "isValid": true
     * }
     * 
     * @return JSON-formatted metrics
     */
    public String getMetricsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(String.format("  \"totalEntitiesChecked\": %d,\n", totalEntitiesChecked));
        sb.append(String.format("  \"totalIssuesFound\": %d,\n", totalIssuesFound));
        
        sb.append("  \"issuesBySeverity\": {\n");
        for (Severity severity : Severity.values()) {
            int count = getIssuesBySeverity(severity).size();
            sb.append(String.format("    \"%s\": %d", severity, count));
            if (severity != Severity.INFO) sb.append(",");
            sb.append("\n");
        }
        sb.append("  },\n");
        
        sb.append("  \"issuesByEntity\": {\n");
        String[] entities = {"BOOK", "CHAPTER", "VERSE", "NOTE", "IMAGE"};
        for (int i = 0; i < entities.length; i++) {
            int count = getIssuesByEntityType(entities[i]).size();
            sb.append(String.format("    \"%s\": %d", entities[i], count));
            if (i < entities.length - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  },\n");
        
        sb.append(String.format("  \"elapsedTimeMs\": %d,\n", elapsedTimeMs));
        sb.append(String.format("  \"isValid\": %s\n", isValid()));
        sb.append("}");
        
        return sb.toString();
    }
}
