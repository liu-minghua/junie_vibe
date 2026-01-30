# Phase 4 API Reference: OahspeDataValidator

**Date:** 2026-01-30  
**Version:** 1.0  
**Target Audience:** Phase 5+ developers consuming OahspeDataValidator

---

## Overview

This document defines the complete API contract for OahspeDataValidator. Use this as reference when integrating Phase 4 results into Phase 5 or using validator results in REST endpoints or reports.

**Key Contracts:**
- All public methods are documented with signatures
- Parameter constraints explicitly stated
- Return types and value meanings explained
- Exception types and triggers documented
- Null handling strategy defined
- Working code examples provided

---

## Public Methods

### OahspeDataValidator

#### `validateAll(ValidationProgressCallback callback)`

**Signature:**
```java
public ValidationResult validateAll(ValidationProgressCallback callback)
    throws ValidationException
```

**Parameters:**
- `callback` (ValidationProgressCallback): Optional progress tracking callback
  - **Nullable:** Yes
  - **Default:** Pass null to skip progress updates
  - **Usage:** Implement to track progress in UI or logs

**Returns:**
- `ValidationResult`: Aggregated validation results with all issues found
  - Never null
  - Contains summary metrics and detailed issue list
  - Check `isValid()` to determine pass/fail

**Throws:**
- `ValidationException`: If database connection fails or repository query fails
  - **When:** Critical system errors (DB unreachable, JPA errors)
  - **Not thrown for:** Data inconsistencies (those go in ValidationResult)

**Example: Basic Usage**
```java
@Service
public class ValidationService {
    private final OahspeDataValidator validator;
    
    public ValidationResult validateDatabase() throws ValidationException {
        ValidationResult result = validator.validateAll(null);
        
        if (result.isValid()) {
            System.out.println("✅ All data is valid!");
        } else {
            System.out.println("❌ Found " + result.getTotalIssuesFound() + " issues");
            System.out.println(result.getDetailedReport());
        }
        
        return result;
    }
}
```

**Example: With Progress Tracking**
```java
public ValidationResult validateWithProgress() throws ValidationException {
    ValidationProgressCallback callback = new ValidationProgressCallback() {
        @Override
        public void onValidationStart(int totalEntities) {
            System.out.println("Starting validation of " + totalEntities + " entities");
        }
        
        @Override
        public void onEntityValidated(String entityType, int count) {
            System.out.println("Validated " + count + " " + entityType + "s");
        }
        
        @Override
        public void onValidationComplete(ValidationResult result) {
            System.out.println("Validation complete: " + result.getIssuesSummary());
        }
    };
    
    return validator.validateAll(callback);
}
```

**Example: Using Results for REST Response**
```java
@RestController
@RequestMapping("/api/validation")
public class ValidationController {
    private final OahspeDataValidator validator;
    
    @GetMapping("/status")
    public ResponseEntity<?> getValidationStatus() {
        try {
            ValidationResult result = validator.validateAll(null);
            return ResponseEntity.ok(new ValidationStatusResponse(
                result.isValid(),
                result.getTotalEntitiesChecked(),
                result.getTotalIssuesFound(),
                result.getIssuesSummary()
            ));
        } catch (ValidationException e) {
            return ResponseEntity.status(500).body(
                new ErrorResponse("Validation failed: " + e.getMessage())
            );
        }
    }
}
```

---

#### `validateEntities(EntityType entityType, ValidationProgressCallback callback)`

**Signature:**
```java
public ValidationResult validateEntities(
    String entityType,
    ValidationProgressCallback callback
) throws ValidationException
```

**Parameters:**
- `entityType` (String): Type of entity to validate
  - **Nullable:** No - required parameter
  - **Valid values:** "BOOK", "CHAPTER", "VERSE", "NOTE", "IMAGE"
  - **Case-sensitive:** Yes
  - **Other values:** Will throw IllegalArgumentException

- `callback` (ValidationProgressCallback): Optional progress tracking
  - **Nullable:** Yes
  - **Usage:** Same as validateAll()

**Returns:**
- `ValidationResult`: Results for specified entity type only
  - Contains only issues related to that entity type
  - Other entity types excluded
  - Summary metrics reflect only this entity type

**Throws:**
- `ValidationException`: If database error occurs
- `IllegalArgumentException`: If entityType is invalid

**Example: Validate Only Verses**
```java
public void validateVersesOnly() throws ValidationException {
    ValidationResult result = validator.validateEntities("VERSE", null);
    
    System.out.println("Verses validated: " + result.getTotalEntitiesChecked());
    System.out.println("Issues found: " + result.getTotalIssuesFound());
    
    // Get only verse-related issues
    result.getIssuesBySeverity(Severity.ERROR)
        .stream()
        .filter(issue -> issue.getEntityType().equals("VERSE"))
        .forEach(issue -> System.out.println(issue.getMessage()));
}
```

**Example: Check Specific Entity Type**
```java
public void checkImageReferences() throws ValidationException {
    ValidationResult imageValidation = validator.validateEntities("IMAGE", null);
    
    if (imageValidation.hasCriticalIssues()) {
        System.out.println("⚠️  Critical image issues found:");
        imageValidation.getCriticalIssues().forEach(issue -> 
            System.out.println("  - " + issue.getMessage())
        );
    }
}
```

---

#### `validateRelationships()`

**Signature:**
```java
public ValidationResult validateRelationships()
    throws ValidationException
```

**Parameters:**
- None

**Returns:**
- `ValidationResult`: Results for all cross-entity relationships
  - Issues from: verse sequencing, reference integrity, completeness checks
  - Individual entity validation issues NOT included
  - Designed to run AFTER validateAll() for detailed analysis

**Throws:**
- `ValidationException`: If database error occurs

**Example: Check Only Relationships**
```java
public void checkReferentialIntegrity() throws ValidationException {
    ValidationResult relationshipResult = validator.validateRelationships();
    
    if (relationshipResult.isValid()) {
        System.out.println("✅ All relationships are valid");
    } else {
        System.out.println("Relationship issues:");
        relationshipResult.getDetailedReport();
    }
}
```

---

### ValidationResult

#### Data Access Methods

**`getTotalEntitiesChecked()`**
```java
public int getTotalEntitiesChecked()
```
Returns total number of entities validated (sum of all types).

**`getTotalIssuesFound()`**
```java
public int getTotalIssuesFound()
```
Returns total count of all issues (all severities combined).

**`getIssuesBySeverity(Severity severity)`**
```java
public List<ValidationIssue> getIssuesBySeverity(Severity severity)
```
Returns filtered list of issues by severity level.

**`getIssuesByEntityType(String entityType)`**
```java
public List<ValidationIssue> getIssuesByEntityType(String entityType)
```
Returns filtered list of issues for specific entity type.

---

#### Status Query Methods

**`isValid()`**
```java
public boolean isValid()
```
Returns true if no CRITICAL or ERROR issues found (WARNING/INFO ignored).

**`hasCriticalIssues()`**
```java
public boolean hasCriticalIssues()
```
Returns true if any CRITICAL severity issues exist.

**`hasErrors()`**
```java
public boolean hasErrors()
```
Returns true if any ERROR or CRITICAL issues exist.

**`hasWarnings()`**
```java
public boolean hasWarnings()
```
Returns true if any WARNING issues exist (including ERROR/CRITICAL).

---

#### Reporting Methods

**`getIssuesSummary()`**
```java
public String getIssuesSummary()
```
Returns concise human-readable summary (1-5 lines).

**Format:**
```
Validation Results:
  - Total entities checked: 5,432
  - Total issues found: 12
  - Critical: 0 | Errors: 3 | Warnings: 9
  - Status: ✅ VALID (no critical/error issues)
```

**`getDetailedReport()`**
```java
public String getDetailedReport()
```
Returns comprehensive report with all issues organized by entity type and severity.

**Format:**
```
=== VALIDATION DETAILED REPORT ===

CRITICAL ISSUES (0):
[None]

ERROR ISSUES (3):
1. Verse #5 in Chapter 2: Verse number gap (expected 5, found 6)
2. Note #142: References non-existent Verse #9999
3. Image #8: References non-existent Note

WARNING ISSUES (9):
1. Chapter 5: Contains only 1 verse (typically >5)
...
```

**`getMetricsSummary()`**
```java
public String getMetricsSummary()
```
Returns JSON-formatted metrics for programmatic use.

**Format:**
```json
{
  "totalEntitiesChecked": 5432,
  "totalIssuesFound": 12,
  "issuesBySeverity": {
    "CRITICAL": 0,
    "ERROR": 3,
    "WARNING": 9,
    "INFO": 0
  },
  "issuesByEntity": {
    "VERSE": 5,
    "NOTE": 2,
    "IMAGE": 1,
    "CHAPTER": 4
  },
  "elapsedTimeMs": 2450,
  "isValid": true
}
```

---

### ValidationIssue

#### Constructor

**`ValidationIssue(Severity severity, String entityType, Long entityId, String rule, String message)`**

**Parameters:**
- `severity` (Severity): CRITICAL | ERROR | WARNING | INFO
- `entityType` (String): "BOOK" | "CHAPTER" | "VERSE" | "NOTE" | "IMAGE"
- `entityId` (Long): ID of affected entity
- `rule` (String): Name of violated rule (e.g., "VerseSequencing")
- `message` (String): Human-readable description

**Example:**
```java
ValidationIssue issue = new ValidationIssue(
    Severity.ERROR,
    "VERSE",
    123L,
    "VerseSequencing",
    "Verse number gap in Chapter 5: expected 5, found 6"
);
```

#### Access Methods

**`getSeverity()` → Severity**  
**`getEntityType()` → String**  
**`getEntityId()` → Long**  
**`getRule()` → String**  
**`getMessage()` → String**  
**`getSuggestedFix()` → String` (optional)** 

**Example:**
```java
validationResult.getIssuesBySeverity(Severity.ERROR)
    .forEach(issue -> {
        System.out.println("Entity: " + issue.getEntityType() + 
                          " #" + issue.getEntityId());
        System.out.println("Issue: " + issue.getMessage());
        if (issue.getSuggestedFix() != null) {
            System.out.println("Fix: " + issue.getSuggestedFix());
        }
    });
```

---

### ValidationProgressCallback (Interface)

#### `onValidationStart(int totalEntities)`

Called when validation begins.

**Parameters:**
- `totalEntities` (int): Total number of entities to validate

**Use Case:** Display progress bar maximum or start spinner.

---

#### `onEntityValidated(String entityType, int count)`

Called after each batch of entities is validated.

**Parameters:**
- `entityType` (String): Type just validated ("VERSE", "CHAPTER", etc.)
- `count` (int): Cumulative count of this entity type validated

**Use Case:** Update progress bar position.

**Example:**
```java
@Override
public void onEntityValidated(String entityType, int count) {
    progressBar.update(count);
}
```

---

#### `onValidationComplete(ValidationResult result)`

Called when validation finishes.

**Parameters:**
- `result` (ValidationResult): Final validation results

**Use Case:** Display completion message, enable buttons, close progress dialog.

**Example:**
```java
@Override
public void onValidationComplete(ValidationResult result) {
    if (result.isValid()) {
        showNotification("✅ Validation passed!");
    } else {
        showDetailedReport(result);
    }
}
```

---

## Enumerations

### Severity

```java
public enum Severity {
    CRITICAL,  // Data is invalid, must fix before use
    ERROR,     // Data is inconsistent, should fix
    WARNING,   // Data is suboptimal, nice to fix
    INFO       // Informational, no action needed
}
```

---

## Exceptions

### ValidationException

**Extends:** RuntimeException (or checked Exception)

**Constructors:**
```java
public ValidationException(String message)
public ValidationException(String message, Throwable cause)
```

**When Thrown:**
- Database connection failure
- Repository query error (JPA exception)
- Null pointer in validation logic (should not happen)

**Never Thrown For:**
- Data inconsistencies (recorded in ValidationResult instead)
- Invalid input parameters (throws IllegalArgumentException)

**Example:**
```java
try {
    ValidationResult result = validator.validateAll(null);
} catch (ValidationException e) {
    logger.error("Validation system error", e);
    // Inform user that validation couldn't run (not that data is invalid)
}
```

---

## Data Classes

### ValidationResult Fields

```java
public class ValidationResult {
    private int totalEntitiesChecked;              // Total count
    private int totalIssuesFound;                  // Total count
    private Map<Severity, List<ValidationIssue>> issuesBySeverity;  // Organized by severity
    private Map<String, List<ValidationIssue>> issuesByEntityType;  // Organized by entity
    private long elapsedTimeMs;                    // Execution time
    private Map<String, Integer> successfulValidations;  // Count per validator
}
```

### ValidationIssue Fields

```java
public class ValidationIssue {
    private Severity severity;            // CRITICAL, ERROR, WARNING, INFO
    private String entityType;            // BOOK, CHAPTER, VERSE, NOTE, IMAGE
    private Long entityId;                // ID of affected entity
    private String rule;                  // Rule that was violated
    private String message;               // Human-readable description
    private String suggestedFix;          // Optional fix suggestion (null if none)
}
```

---

## Common Usage Patterns

### Pattern 1: Validate and Report

```java
public void validateAndReport() throws ValidationException {
    ValidationResult result = validator.validateAll(null);
    
    System.out.println(result.getDetailedReport());
    
    if (!result.isValid()) {
        // Send alert to admin
        notificationService.notifyAdmin(result.getIssuesSummary());
    }
}
```

### Pattern 2: Validate and Export

```java
public void validateAndExport(String filename) throws ValidationException {
    ValidationResult result = validator.validateAll(null);
    
    String report = result.getDetailedReport();
    fileService.writeToFile(filename, report);
}
```

### Pattern 3: Validate Specific Type with Filtering

```java
public void validateVersesAndFixErrors() throws ValidationException {
    ValidationResult result = validator.validateEntities("VERSE", null);
    
    result.getIssuesBySeverity(Severity.ERROR)
        .forEach(issue -> {
            Verse verse = verseRepository.findById(issue.getEntityId()).get();
            // Apply fix logic
        });
}
```

### Pattern 4: REST Endpoint

```java
@GetMapping("/api/validation/report")
public ResponseEntity<Map<String, Object>> getValidationReport() {
    try {
        ValidationResult result = validator.validateAll(null);
        return ResponseEntity.ok(Map.of(
            "valid", result.isValid(),
            "totalIssues", result.getTotalIssuesFound(),
            "summary", result.getIssuesSummary(),
            "details", result.getMetricsSummary()
        ));
    } catch (ValidationException e) {
        return ResponseEntity.status(500).body(Map.of(
            "error", "Validation system unavailable: " + e.getMessage()
        ));
    }
}
```

### Pattern 5: Scheduled Validation Task

```java
@Component
public class ValidationScheduler {
    @Scheduled(cron = "0 2 * * *")  // 2 AM daily
    public void dailyValidation() {
        try {
            ValidationResult result = validator.validateAll(null);
            if (!result.isValid()) {
                logger.warn("Daily validation found issues: " + 
                           result.getIssuesSummary());
            }
        } catch (ValidationException e) {
            logger.error("Daily validation failed", e);
        }
    }
}
```

---

## Null Handling Strategy

**Method Returns:**
- `getIssuesBySeverity()` → Returns empty List if no issues (never null)
- `getDetailedReport()` → Returns String (never null, empty string if no issues)
- `ValidationResult` → Always non-null from public methods
- `ValidationProgressCallback` → May be null (optional parameter)

**Method Parameters:**
- `callback` parameter → Accepts null (progress tracking disabled)
- `entityType` parameter → Does NOT accept null (throws IllegalArgumentException)

**Entity Fields:**
- `entityId` → Never null
- `rule` → Never null
- `message` → Never null
- `suggestedFix` → May be null (optional suggestion)

---

## Performance Expectations

**For Dataset Sizes:**
- 1,000 entities: <100ms
- 10,000 entities: <500ms
- 100,000 entities: 5-10 seconds
- 1,000,000 entities: 30-60 seconds

**Note:** Actual performance depends on:
- Database query speed
- Network latency (if DB is remote)
- System CPU/memory availability

---

## Thread Safety

**OahspeDataValidator:** NOT thread-safe (don't share across threads)

**ValidationResult:** Thread-safe for reading (use in REST responses)

**Recommendation:** Create new validator instance per validation run or use synchronized access.

---

## Integration Checklist for Phase 5

When consuming OahspeDataValidator in Phase 5:

- [ ] Import ValidationResult, ValidationIssue, ValidationException
- [ ] Import ValidationProgressCallback if using progress tracking
- [ ] Inject OahspeDataValidator via constructor
- [ ] Handle ValidationException in try-catch blocks
- [ ] Use `result.isValid()` to determine pass/fail
- [ ] Check `hasCriticalIssues()` for blocking operations
- [ ] Call `getDetailedReport()` for user-facing error messages
- [ ] Use `getIssuesSummary()` for concise status output
- [ ] Implement ValidationProgressCallback for long-running UIs

---

## Backward Compatibility

**Current Version:** 1.0  
**Stability:** Stable (API locked after Phase 4 implementation)

**What won't change:**
- Public method signatures
- Return types
- Exception types
- Severity enum values

**What might evolve:**
- New validation rules (adds issues to ValidationResult)
- Performance optimizations (faster execution, same results)
- Additional helper methods (additive only)

---

*Document Status: COMPLETE*  
*Last Updated: 2026-01-30*  
*Ready for: Phase 5 Integration, Phase 4 Testing*
