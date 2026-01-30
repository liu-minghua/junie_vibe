# Phase 4 Design Document: OahspeDataValidator

**Date:** 2026-01-30  
**Status:** In Progress  
**Version:** 1.0  

---

## Overview

OahspeDataValidator is a post-ingestion validation framework that ensures data quality of the Oahspe sacred text database. After Phase 3 completes the PDF ingestion pipeline (text extraction → parsing → database storage), Phase 4 validates that:

1. **Individual entities** conform to business rules (e.g., verse numbering, note content)
2. **Relationships between entities** are consistent (e.g., verses belong to valid chapters)
3. **Referential integrity** is maintained (e.g., images reference existing notes)
4. **Data completeness** across the ingestion (e.g., no missing verses in chapters)

**Integration point:** Consumes persisted entities from Phase 3 via Spring Data repositories. No external files or APIs required.

---

## Architecture Diagram

### High-Level Component Interaction

```
┌─────────────────────────────────────────────────────────────────┐
│                    OahspeDataValidator                           │
│                   (Main orchestrator)                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ EntityValidator  │  │CrossEntityValidator│ │ValidationCtx │  │
│  │  - Book rules    │  │ - Chapter refs    │  │ - Results   │  │
│  │  - Chapter rules │  │ - Verse refs      │  │ - Metrics   │  │
│  │  - Verse rules   │  │ - Image refs      │  │ - Progress  │  │
│  │  - Note rules    │  │ - Note refs       │  │             │  │
│  │  - Image rules   │  │ - Sequential checks │  │             │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
│           ↑                      ↑                    ↑           │
│           └──────────────────────┼────────────────────┘           │
│                                  │                                │
└──────────────────────────────────┼────────────────────────────────┘
                                   │
                    ┌──────────────┴────────────────┐
                    ↓                               ↓
            Spring Data Repositories        ProgressCallback
          (BookRepository, etc.)           (Optional observer)
                    │
                    ↓
            H2 Database (Phase 3)
```

### Component Responsibilities

#### OahspeDataValidator (Main Orchestrator)
- **Responsibility:** Coordinate validation workflow across all entity types
- **Input:** Takes optional ProgressCallback for external progress tracking
- **Output:** Returns ValidationResult with comprehensive error metrics
- **Methods:**
  - `validateAll(ProgressCallback?)` → ValidationResult
  - `validateEntities(EntityType, ProgressCallback?)` → ValidationResult
  - `validateRelationships()` → ValidationResult
  - `validateIntegrity()` → ValidationResult

**Why separate:** Single responsibility - orchestration only, delegates specific validation logic.

#### EntityValidator (Single Responsibility)
- **Responsibility:** Validate individual entity instances for business rule compliance
- **Input:** Single entity or collection of entities
- **Output:** List of ValidationIssue objects
- **Methods:**
  - `validateBook(Book)` → List<ValidationIssue>
  - `validateChapter(Chapter)` → List<ValidationIssue>
  - `validateVerse(Verse)` → List<ValidationIssue>
  - `validateNote(Note)` → List<ValidationIssue>
  - `validateImage(Image)` → List<ValidationIssue>

**Why separate:** Makes testing individual entity rules easier, reusable across different validation scenarios.

#### CrossEntityValidator (Relationship Validation)
- **Responsibility:** Validate relationships and consistency across entities
- **Input:** Entities from multiple repositories
- **Output:** List of ValidationIssue objects
- **Methods:**
  - `validateVerseSequencing()` → List<ValidationIssue> (are verses numbered 1, 2, 3...?)
  - `validateChapterReferences()` → List<ValidationIssue> (do all verses belong to real chapters?)
  - `validateNoteReferences()` → List<ValidationIssue> (do note verse refs exist?)
  - `validateImageReferences()` → List<ValidationIssue> (do images reference real notes?)
  - `validateCrossReferences()` → List<ValidationIssue> (comprehensive referential integrity)

**Why separate:** Requires accessing multiple repositories, more complex validation logic.

#### ValidationResult (Data Container)
- **Responsibility:** Aggregate and report validation findings
- **Fields:**
  - totalEntitiesChecked: int
  - totalIssuesFound: int
  - issuesBySeverity: Map<Severity, List<ValidationIssue>>
  - issuesByEntity: Map<EntityType, List<ValidationIssue>>
  - elapsedTimeMs: long
  - successfulValidations: Map<String, Integer> (counts per validator)
  - isSuccessful: boolean (no critical issues)
  
- **Methods:**
  - `addIssue(ValidationIssue)`
  - `getIssuesSummary()` → String (human-readable summary)
  - `getDetailedReport()` → String (full report with all issues)
  - `hasIssues()`, `hasCriticalIssues()`, `isValid()` → boolean

**Why separate:** Provides clean data structure for results, enables flexible reporting.

#### ValidationIssue (Data Container)
- **Responsibility:** Represent a single validation problem
- **Fields:**
  - severity: Severity enum (CRITICAL, ERROR, WARNING, INFO)
  - entityType: String (Book, Chapter, etc.)
  - entityId: Long
  - rule: String (what was violated, e.g., "VerseNumbering")
  - message: String (human-readable explanation)
  - suggestedFix: String (optional, how to fix)

**Why separate:** Allows detailed issue reporting, supports filtering by severity/type.

#### ValidationProgressCallback (Observer Interface)
- **Responsibility:** Enable external progress tracking without coupling
- **Methods:**
  - `onValidationStart(int totalEntities)` → void
  - `onEntityValidated(String entityType, int count)` → void
  - `onValidationComplete(ValidationResult result)` → void

**Why separate:** Optional progress tracking, doesn't interfere with main validation logic, enables async progress UI.

---

## Data Flow: Happy Path

```
1. User calls validator.validateAll()
   ↓
2. OahspeDataValidator loads all entities from repositories
   ├─ Books: bookRepository.findAll()
   ├─ Chapters: chapterRepository.findAll()
   ├─ Verses: verseRepository.findAll()
   ├─ Notes: noteRepository.findAll()
   └─ Images: imageRepository.findAll()
   ↓
3. EntityValidator checks each entity individually
   ├─ validateBook: Check book has title, year
   ├─ validateChapter: Check chapter has number, book ref
   ├─ validateVerse: Check verse has number, chapter ref, text
   ├─ validateNote: Check note has content, verse ref
   └─ validateImage: Check image has path, exists on disk
   ↓
4. CrossEntityValidator checks relationships
   ├─ validateVerseSequencing: Verses 1,2,3... in each chapter
   ├─ validateChapterReferences: All verses have valid chapter
   ├─ validateNoteReferences: All notes reference valid verses
   └─ validateImageReferences: All images reference valid notes
   ↓
5. All issues aggregated into ValidationResult
   ↓
6. Return ValidationResult with metrics
   ├─ Total entities checked: 5000
   ├─ Total issues found: 3
   ├─ Critical issues: 0
   ├─ Warnings: 3
   └─ Validation successful: true
```

---

## Validation Rules by Entity

### Book Validation
- **Required fields:** title (non-blank), year (positive if present)
- **Format:** Title length < 500 characters
- **Referential:** Has at least one chapter
- **Severity:** CRITICAL if missing required fields

### Chapter Validation
- **Required fields:** chapterNumber (positive), book (non-null)
- **Format:** Chapter number < 100 (practical constraint for sacred texts)
- **Referential:** Book with bookId exists
- **Sequence:** Chapter numbers are unique within book
- **Content:** Has at least one verse
- **Severity:** ERROR if missing refs, WARNING if sequence issues

### Verse Validation
- **Required fields:** verseNumber (positive), chapter (non-null), text (non-blank)
- **Format:** Text < 5000 characters, verse number < 1000
- **Referential:** Chapter with chapterId exists
- **Sequence:** Verse numbers are unique within chapter
- **Content:** Text is non-empty
- **Severity:** ERROR if missing fields, WARNING if sequence gaps

### Note Validation
- **Required fields:** content (non-blank), verse (non-null)
- **Format:** Content < 10000 characters
- **Referential:** Verse with verseId exists
- **Content:** Text is non-empty
- **Severity:** ERROR if missing fields

### Image Validation
- **Required fields:** filePath (non-blank), note (non-null) [if linked]
- **Format:** File path < 500 characters
- **File existence:** [Optional] Check if file exists on disk
- **Referential:** Note (if linked) exists
- **Severity:** WARNING if file missing, ERROR if note ref missing

---

## Cross-Entity Validation Rules

### Verse Sequencing
**Rule:** Within each chapter, verses must be numbered consecutively starting from 1.

**Validation Logic:**
```
For each chapter:
  verses = chapter.getVerses().sorted(byVerseNumber)
  for i in 0..verses.length:
    if verses[i].verseNumber != i+1:
      addIssue(ERROR, "VerseSequencing", "Chapter has gap in verse numbering")
```

**Severity:** WARNING (indicates data issue but doesn't block usage)

### Chapter Completeness
**Rule:** Each chapter should have verses; no chapters should be empty.

**Validation Logic:**
```
For each chapter:
  if chapter.getVerses().isEmpty():
    addIssue(WARNING, "ChapterCompleteness", "Chapter has no verses")
```

**Severity:** WARNING

### Reference Integrity
**Rule:** All foreign key references point to existing entities.

**Validation Logic:**
```
For each verse:
  if chapterRepository.findById(verse.chapterId) == null:
    addIssue(CRITICAL, "ReferenceIntegrity", "Verse references non-existent chapter")

For each note:
  if verseRepository.findById(note.verseId) == null:
    addIssue(CRITICAL, "ReferenceIntegrity", "Note references non-existent verse")

For each image:
  if image.getNoteId() != null && noteRepository.findById(image.noteId) == null:
    addIssue(ERROR, "ReferenceIntegrity", "Image references non-existent note")
```

**Severity:** CRITICAL for missing required refs, ERROR for missing optional refs

---

## Error Handling Strategy

### File-Level Errors (Fatal - Stop)
- Database connection failure
- Repository query exceptions
- Spring bean initialization failure

**Handling:** Throw `ValidationException` immediately, fail fast.

**Code Pattern:**
```java
try {
    List<Book> books = bookRepository.findAll();
} catch (DataAccessException e) {
    throw new ValidationException("Failed to load books", e);
}
```

### Entity-Level Errors (Recoverable - Continue)
- Individual entity validation failures
- Missing non-critical fields
- Format violations

**Handling:** Catch, record as ValidationIssue, continue validation.

**Code Pattern:**
```java
try {
    validateBook(book);
} catch (ValidationException e) {
    result.addIssue(new ValidationIssue(ERROR, "Book", book.getId(), "ValidationFailed", e.getMessage()));
    // Continue to next book
}
```

### Relationship-Level Errors (Recoverable - Continue)
- Cross-entity reference issues
- Sequence gaps
- Missing optional relationships

**Handling:** Detect and record, don't throw exceptions.

**Code Pattern:**
```java
if (!verseRepository.existsById(note.getVerseId())) {
    result.addIssue(new ValidationIssue(ERROR, "Note", note.getId(), "ReferenceIntegrity", ...));
}
```

### Never Silently Fail
- All issues (CRITICAL, ERROR, WARNING, INFO) must be recorded
- User can decide which issues to address based on severity
- No validation failures should be hidden

---

## Integration with Phase 3

### Input Contract
**What Phase 4 receives from Phase 3:**
- Populated database with Book, Chapter, Verse, Note, Image entities
- All entities have primary keys and basic relationships set
- Data may have inconsistencies that validation will uncover

### Integration Points
```
Phase 3: OahspeIngestionRunner
  ├─ Calls bookRepository.saveAndFlush()
  ├─ Calls chapterRepository.saveAndFlush()
  ├─ Calls verseRepository.saveAndFlush()
  ├─ Calls noteRepository.saveAndFlush()
  └─ Calls imageRepository.saveAndFlush()
    ↓
    [Database now populated]
    ↓
Phase 4: OahspeDataValidator
  ├─ Reads from bookRepository.findAll()
  ├─ Reads from chapterRepository.findAll()
  ├─ Reads from verseRepository.findAll()
  ├─ Reads from noteRepository.findAll()
  └─ Reads from imageRepository.findAll()
  ↓
  [Validation Report]
```

### No Direct Coupling
- Phase 4 doesn't call Phase 3 code
- Only interacts through shared repositories
- Could run independently if needed
- Could run repeatedly to check data consistency

---

## Design Decisions & Rationale

### Decision 1: Separate EntityValidator and CrossEntityValidator
**Chosen approach:** Two separate validator classes  
**Rationale:**
- Entity validation has different complexity than relationship validation
- Easier to test entities in isolation
- Easier to extend with new entity types without affecting cross-entity logic
- Follows Single Responsibility Principle

**Alternative:** One monolithic Validator class
- Simpler initially but harder to maintain
- Tests would be more complex

---

### Decision 2: ValidationIssue Objects vs Throwing Exceptions
**Chosen approach:** Collect ValidationIssue objects in ValidationResult  
**Rationale:**
- Don't stop on first error; collect ALL issues
- User sees complete picture of data problems
- Allows filtering/prioritizing issues by severity
- Can export issues to report files
- Better user experience (fix all issues at once vs iteratively)

**Alternative:** Throw exception on first issue
- Would stop validation early
- User wouldn't know about other issues

---

### Decision 3: Optional ProgressCallback for Large Datasets
**Chosen approach:** Optional ProgressCallback interface  
**Rationale:**
- Validation could take minutes for large datasets (100k+ verses)
- Provide progress updates without coupling to UI
- Async progress tracking possible
- Can ignore if not needed

**Alternative:** No progress tracking
- Would leave users guessing about progress
- No way to implement progress bars in UI

---

### Decision 4: No Automatic Fixing
**Chosen approach:** Report issues only, don't auto-fix  
**Rationale:**
- Validation shouldn't modify data automatically
- User should review and approve fixes
- Prevents unintended data loss
- Gives users control over their data
- Easier to implement and test

**Alternative:** Auto-fix common issues
- Risky with sacred text data
- User loses visibility into what changed
- Hard to verify fixes are correct

---

### Decision 5: Severity Levels (CRITICAL, ERROR, WARNING, INFO)
**Chosen approach:** Four-level severity system  
**Rationale:**
- CRITICAL: Data invalid, must fix before use
- ERROR: Data inconsistent, should fix
- WARNING: Data suboptimal, nice to fix
- INFO: Informational, no action needed

Allows users to:
- Fix critical issues first
- Plan other fixes later
- Generate reports by severity
- Set validation pass/fail thresholds

---

## Dependencies

### Internal Dependencies
- **Repositories:** BookRepository, ChapterRepository, VerseRepository, NoteRepository, ImageRepository
  - Already exist from Phase 2
  - Used for read-only queries

- **Entities:** Book, Chapter, Verse, Note, Image
  - Already exist from Phase 1-2
  - No modifications needed

- **Spring:** @Component, @RequiredArgsConstructor, @Transactional
  - Standard Spring annotations for component and DI
  - Already in pom.xml

### External Dependencies
- **None required** - Uses only existing Spring Data and Java standard library
- No new Maven dependencies needed

---

## Performance Considerations

### Algorithm Complexity
- **Loading entities:** O(n) where n = total entities (unavoidable)
- **Entity validation:** O(n) - single pass through each entity type
- **Cross-entity validation:** O(n) - single pass through relationships
- **Overall:** O(n) - linear time, not exponential

### Memory Usage
- **Entities loaded:** ~100k entities → ~1GB memory (estimate)
- **ValidationIssue objects:** ~1000 issues → ~5MB (estimate)
- **Could optimize:** Stream entities instead of loading all at once (for Phase 5)

### Expected Performance
- 1000 verses: <1 second
- 100,000 verses: ~5-10 seconds
- 1,000,000 verses: ~30-60 seconds

### Optimization Opportunities (Phase 5)
- Stream entities instead of List.findAll()
- Parallel validation with ExecutorService
- Batch processing by entity type
- Database-level validation queries (SQL checks)

---

## Testing Strategy

### Unit Tests (EntityValidator, CrossEntityValidator)
- Test each validation rule independently
- Use test builders for entity creation
- Mock repositories not needed (testing logic, not data)
- Target: 90%+ coverage

### Integration Tests
- Use @DataJpaTest with test H2 database
- Insert test data, validate, check results
- Test interaction between validators
- Test ValidationResult aggregation
- Test ProgressCallback invocation
- Target: 6-8 integration tests

### Test Scenarios
- **Happy path:** Valid data passes validation
- **Error cases:** Invalid data caught and reported
- **Edge cases:** Empty database, single entity, boundary values
- **Relationship cases:** Missing references, circular refs, orphaned entities

---

## Future Enhancements

### Phase 5 Candidate: Auto-Repair
- Implement fixes for common issues
- Add dry-run mode (show what would be fixed)
- Log all fixes for audit trail

### Phase 5 Candidate: Advanced Reporting
- Generate PDF reports
- Export validation results to CSV
- Create dashboard with validation metrics
- Historical tracking of validation issues

### Phase 5 Candidate: Performance Optimization
- Stream entities instead of loading all
- Parallel validation with thread pools
- Database-level validation queries
- Caching of frequently accessed entities

### Phase 5 Candidate: ML-Based Validation
- Detect anomalies in text content
- Flag suspicious patterns
- Suggest corrections based on patterns

---

## Class Structure Summary

```java
// Main orchestrator
public class OahspeDataValidator {
    - validateAll(ProgressCallback?) → ValidationResult
    - validateEntities(EntityType) → ValidationResult
    - validateRelationships() → ValidationResult
}

// Validator implementations
public class EntityValidator {
    - validateBook(Book) → List<ValidationIssue>
    - validateChapter(Chapter) → List<ValidationIssue>
    - validateVerse(Verse) → List<ValidationIssue>
    - validateNote(Note) → List<ValidationIssue>
    - validateImage(Image) → List<ValidationIssue>
}

public class CrossEntityValidator {
    - validateVerseSequencing() → List<ValidationIssue>
    - validateChapterReferences() → List<ValidationIssue>
    - validateNoteReferences() → List<ValidationIssue>
    - validateImageReferences() → List<ValidationIssue>
}

// Result containers
public class ValidationResult {
    - totalEntitiesChecked: int
    - totalIssuesFound: int
    - issuesBySeverity: Map<Severity, List<ValidationIssue>>
    - addIssue(ValidationIssue)
    - getIssuesSummary() → String
    - isValid() → boolean
}

public class ValidationIssue {
    - severity: Severity
    - entityType: String
    - entityId: Long
    - rule: String
    - message: String
    - suggestedFix: String
}

// Observer for progress
public interface ValidationProgressCallback {
    - onValidationStart(int totalEntities)
    - onEntityValidated(String entityType, int count)
    - onValidationComplete(ValidationResult)
}

// Exception
public class ValidationException extends Exception {
    - constructor(String message)
    - constructor(String message, Throwable cause)
}
```

---

## Summary

OahspeDataValidator provides a comprehensive validation framework that:

1. ✅ **Validates all entities** against business rules
2. ✅ **Checks relationships** between entities
3. ✅ **Aggregates issues** by severity for prioritization
4. ✅ **Integrates cleanly** with Phase 3 without coupling
5. ✅ **Enables progress tracking** for long-running validation
6. ✅ **Reports comprehensive results** for user decision-making
7. ✅ **Is easily testable** with clear component boundaries
8. ✅ **Follows design patterns** from Phase 1-3

---

*Document Status: READY FOR IMPLEMENTATION*  
*Next Step: Create API Reference Document (Task 4.1.2)*
