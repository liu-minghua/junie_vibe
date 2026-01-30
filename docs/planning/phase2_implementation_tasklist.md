# Phase 2 Implementation Tasklist: Oahspe Ingestion Service

**Status:** In Progress  
**Version:** 1.0  
**Last Updated:** 2026-01-30  
**Estimated Total Duration:** 7.2 hours (430 minutes)

---

## Overview

Phase 2 focuses on building the **OahspeIngestionService** - the core orchestrator that consumes OahspeEvent objects from Phase 1 and builds the entity hierarchy (Book → Chapter → Verse → Note ↔ Image).

**Key Objectives:**
- ✅ Service skeleton and event handlers
- ✅ Integration with repositories and ImageNoteLinker
- Test suite covering parser→service integration
- Service documentation and usage guide
- Full Phase 2 validation

**Completed in Previous Session:**
- ✅ Task 2.1.1: Package structure created
- ✅ Task 2.2.1: OahspeIngestionService implemented (300+ lines)
- ✅ Task 2.2.2: Service compilation verified (mvn clean compile)
- ✅ Task 2.4.1-2.4.2: Integration test skeleton with 2 test cases

---

## Task Groups

### Task Group 2.1: Component Review (Time: 30 min)

#### Task 2.1.1: Package Structure
- **Status:** ✅ COMPLETE
- **Details:** Created directory structure
  ```
  src/test/java/edu/minghualiu/oahspe/ingestion/service/
  ```
- **Time:** 5 min

#### Task 2.1.2: Review Existing Components
- **Status:** ⏳ IN PROGRESS
- **Acceptance Criteria:**
  - [ ] Verified Book.java entity with chapters collection
  - [ ] Verified Chapter.java with book reference and verses
  - [ ] Verified Verse.java with chapter reference and notes
  - [ ] Verified Note.java with verse reference and images (M2M)
  - [ ] Verified Image.java with notes bidirectional link
  - [ ] Verified ImageNoteLinker.java implementation
  - [ ] All entities have proper cascade rules
- **Time:** 25 min

---

### Task Group 2.2: Service Implementation (Time: 2.0 hours)

#### Task 2.2.1: OahspeIngestionService Class
- **Status:** ✅ COMPLETE
- **Details:** Implemented 77-line service with:
  - Dependency injection (6 repositories + ImageNoteLinker)
  - Context management (currentBook, currentChapter, currentVerse, currentNote, currentPageNumber)
  - `ingestEvents()` method with pattern matching event dispatch
  - Event handlers: handleBookStart, handleChapterStart, handleVerse, handleNote, handleImageRef
  - Persistence methods: saveCurrentBook(), finishIngestion()
- **Test Status:** Compilation verified ✅
- **Time:** 1.5 hours (complete)

#### Task 2.2.2: Service Compilation Verification
- **Status:** ✅ COMPLETE
- **Command:** `mvn clean compile -q`
- **Result:** SUCCESS (0 compilation errors)
- **Time:** 5 min

---

### Task Group 2.4: Integration Tests (Time: 2.5 hours)

#### Task 2.4.1: Parser-to-Service Integration Test
- **Status:** ✅ IN PROGRESS (1/6 tests implemented)
- **Test Name:** `testParserToServiceIntegration_SingleVerse()`
- **Details:**
  - Creates sequence: BookStart → ChapterStart → Verse
  - Calls ingestEvents() with page number
  - Asserts book persisted correctly
- **Expected Result:** PASS
- **Time:** 20 min

#### Task 2.4.2: Continuation Line Handling Test
- **Status:** ✅ IN PROGRESS (1/6 tests implemented)
- **Test Name:** `testContinuationLineHandling()`
- **Details:**
  - Tests multi-part verse handling
  - Creates verse with null verseKey for continuation
  - Verifies text concatenation with space separator
- **Expected Result:** PASS
- **Time:** 20 min

#### Task 2.4.3: Image Linking Integration Test
- **Status:** ❌ NOT STARTED
- **Test Name:** `testImageLinkingIntegration()`
- **Specification:**
  ```
  - Create sequence: BookStart → ChapterStart → Verse → Note → ImageRef
  - Call ingestEvents()
  - Assert image persisted
  - Assert image linked to note
  - Assert ImageNoteLinker called correctly
  ```
- **Acceptance Criteria:**
  - [ ] Image created with correct fields (imageKey, title, description, sourcePage)
  - [ ] Image linked to note bidirectionally
  - [ ] Image lookup by imageKey works
- **Time:** 30 min

#### Task 2.4.4: Full Page Integration Test
- **Status:** ❌ NOT STARTED
- **Test Name:** `testFullPageIngestion()`
- **Specification:**
  ```
  - Create realistic page with:
    * 1 book, 2 chapters, 4 verses, 3 notes (mixed across verses), 2 images
  - Call ingestEvents() multiple times with pageNumber increments
  - Assert all entities persisted correctly
  - Verify relationships intact
  ```
- **Acceptance Criteria:**
  - [ ] Book with correct chapter count
  - [ ] Chapters with correct verse count
  - [ ] Verses with correct note count
  - [ ] Notes linked to images bidirectionally
  - [ ] Page numbers tracked correctly
- **Time:** 45 min

#### Task 2.4.5: Idempotency Validation Test
- **Status:** ❌ NOT STARTED
- **Test Name:** `testIdempotentImageLinking()`
- **Specification:**
  ```
  - Create image with specific imageKey
  - Call linkImageToNote() twice with same image
  - Assert image not duplicated (findByImageKey() returns existing)
  - Assert note link added correctly both times
  - Verify no constraint violations
  ```
- **Acceptance Criteria:**
  - [ ] findByImageKey() returns existing image
  - [ ] No DataIntegrityViolationException thrown
  - [ ] Image count = 1 after two calls
  - [ ] Note-image link established correctly
- **Time:** 30 min

#### Task 2.4.6: Multiple Books Ingestion Test
- **Status:** ❌ NOT STARTED
- **Test Name:** `testMultipleBooksIngestion()`
- **Specification:**
  ```
  - Create two complete book sequences:
    * Book A: ChapterStart → 2 verses
    * Book B: ChapterStart → 2 verses
  - Call finishIngestion() between books
  - Assert both books persisted independently
  - Verify chapters/verses belong to correct book
  - Verify state properly reset
  ```
- **Acceptance Criteria:**
  - [ ] Book count = 2
  - [ ] Book A has correct chapter/verse count
  - [ ] Book B has correct chapter/verse count
  - [ ] No cross-contamination between books
  - [ ] currentBook null after finishIngestion()
- **Time:** 45 min

---

### Task Group 2.5: Documentation (Time: 1.5 hours)

#### Task 2.5.1: Service Usage Guide
- **Status:** ❌ NOT STARTED
- **File:** `docs/INGESTION_SERVICE_USAGE.md`
- **Content Specification:**
  ```markdown
  # OahspeIngestionService Usage Guide
  
  ## Overview
  - Purpose: Event→Entity orchestration
  - Input: List<OahspeEvent> from OahspeParser
  - Output: Persisted entity hierarchy
  
  ## Basic Usage
  - Example: Single verse ingestion
  - Example: Multi-chapter book ingestion
  - Example: Notes with images
  
  ## State Management
  - Context fields and lifecycle
  - finishIngestion() behavior
  - Page number tracking
  
  ## Best Practices
  - Transaction boundaries
  - Error handling
  - Continuation line formatting
  - Image linking patterns
  ```
- **Time:** 45 min

#### Task 2.5.2: Javadoc Enhancement
- **Status:** ❌ NOT STARTED
- **Target:** OahspeIngestionService.java
- **Enhancements:**
  - Class-level: 3-4 line description with architecture context
  - Method-level: Parameter descriptions, return values, exceptions
  - Example code in event handlers
  - Link to OahspeEvent types
- **Time:** 30 min

---

### Task Group 2.6: Validation & Delivery (Time: 1.2 hours)

#### Task 2.6.1: Full Test Execution
- **Status:** ❌ NOT STARTED
- **Commands:**
  ```bash
  mvn clean compile          # Verify compilation
  mvn clean test            # Run all tests including Phase 1 + Phase 2
  mvn test -Dtest=OahspeIngestionServiceIT  # Integration tests only
  ```
- **Acceptance Criteria:**
  - [ ] All Phase 1 tests pass (34 existing tests)
  - [ ] All Phase 2 integration tests pass (6 tests)
  - [ ] Total: 40+ tests passing
  - [ ] No compilation errors
  - [ ] No warnings (except expected Spring warnings)
- **Time:** 30 min

#### Task 2.6.2: Code Review Checklist
- **Status:** ❌ NOT STARTED
- **Review Points:**
  - [ ] Service dependency injection complete
  - [ ] All event types handled in switch statement
  - [ ] State transitions correct
  - [ ] Null checks for optional fields
  - [ ] Transaction boundaries (@Transactional placement)
  - [ ] Logging at appropriate levels
  - [ ] No hardcoded values
  - [ ] Consistent naming conventions
  - [ ] Javadoc complete
- **Time:** 20 min

#### Task 2.6.3: Git Commit & Documentation
- **Status:** ❌ NOT STARTED
- **Commit Message Template:**
  ```
  Phase 2: OahspeIngestionService Implementation
  
  - Implement OahspeIngestionService orchestrator
  - Add 6 integration tests covering:
    * Parser-to-service integration
    * Continuation line handling
    * Image linking
    * Full page ingestion
    * Idempotency validation
    * Multiple books
  - Enhance Javadoc with architecture examples
  - Create service usage guide
  - All Phase 2 deliverables complete
  
  Tests: 40/40 passing
  Coverage: Service layer coverage >80%
  ```
- **Commands:**
  ```bash
  git add -A
  git commit -m "Phase 2: OahspeIngestionService Implementation"
  git push origin pdf-ingestion-workflow
  ```
- **Acceptance Criteria:**
  - [ ] Commit message clear and descriptive
  - [ ] All new files staged
  - [ ] Push successful to remote
  - [ ] GitHub shows new commits
  - [ ] Phase 2 completion report in docs/
- **Time:** 20 min

#### Task 2.6.4: Phase 2 Completion Report
- **Status:** ❌ NOT STARTED
- **File:** `docs/PHASE2_COMPLETION_REPORT.md`
- **Contents:**
  - Overview: Objectives achieved
  - Deliverables summary (service + tests + docs)
  - Code metrics (lines of code, test coverage)
  - Git commit hashes
  - Phase 3 preparation status
  - Known limitations / future improvements
- **Time:** 20 min

---

## Progress Summary

| Task Group | Total Time | Status | Notes |
|-----------|-----------|--------|-------|
| 2.1 Component Review | 30 min | 🟡 IN PROGRESS | 1/2 tasks complete |
| 2.2 Service Implementation | 2.0 hrs | ✅ COMPLETE | Service + compilation verified |
| 2.4 Integration Tests | 2.5 hrs | 🟡 IN PROGRESS | 2/6 tests, tests passing |
| 2.5 Documentation | 1.5 hrs | ❌ NOT STARTED | Usage guide + Javadoc |
| 2.6 Validation & Delivery | 1.2 hrs | ❌ NOT STARTED | Full test run + git commit |
| **TOTAL** | **~7.2 hrs** | 🟡 **35% COMPLETE** | **150/430 min done** |

---

## Current Session Progress

### Completed (2.5 hours)
✅ Task 2.1.1 - Package structure  
✅ Task 2.2.1 - OahspeIngestionService (service class)  
✅ Task 2.2.2 - Compilation verification  
✅ Task 2.4.1 - Single verse integration test  
✅ Task 2.4.2 - Continuation line test  

### Next Immediate Actions
1. ⏳ Review existing components (Task 2.1.2) - 25 min
2. ❌ Create remaining 4 integration tests (Tasks 2.4.3-2.4.6) - 2.5 hrs
3. ❌ Documentation (Tasks 2.5.1-2.5.2) - 1.5 hrs
4. ❌ Final validation & git commit (Tasks 2.6.1-2.6.4) - 1.2 hrs

---

## Technical Details

### OahspeIngestionService Architecture

**Dependency Injection (6 repositories):**
```java
@Service
@RequiredArgsConstructor
public class OahspeIngestionService {
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final VerseRepository verseRepository;
    private final NoteRepository noteRepository;
    private final ImageRepository imageRepository;
    private final ImageNoteLinker imageNoteLinker;
```

**Context State Management:**
```java
    private Book currentBook;
    private Chapter currentChapter;
    private Verse currentVerse;
    private Note currentNote;
    private int currentPageNumber;
```

**Event Dispatch Pattern:**
```java
public void ingestEvents(List<OahspeEvent> events, int pageNumber) {
    this.currentPageNumber = pageNumber;
    for (OahspeEvent event : events) {
        switch (event) {
            case OahspeEvent.BookStart book -> handleBookStart(book);
            case OahspeEvent.ChapterStart chapter -> handleChapterStart(chapter);
            // ... other cases
        }
    }
}
```

**Persistence Methods:**
```java
@Transactional
public void saveCurrentBook() { /* ... */ }

@Transactional
public void finishIngestion() { /* ... */ }
```

### Test Configuration

**DataJpaTest with Custom Beans:**
```java
@DataJpaTest
public class OahspeIngestionServiceIT {
    @TestConfiguration
    static class TestConfig {
        @Bean
        public OahspeIngestionService oahspeIngestionService(...) { ... }
        
        @Bean
        public ImageNoteLinker imageNoteLinker(...) { ... }
    }
}
```

---

## Dependencies & Prerequisites

✅ Phase 1 Complete:
- OahspeParser (277 lines)
- OahspeEvent (45 lines)
- ParserState (42 lines)
- 34 parser tests (all passing)

✅ Entity Layer:
- All 5 entities (Book, Chapter, Verse, Note, Image)
- Repositories (5x)
- ImageNoteLinker component

✅ Spring Configuration:
- H2 database for testing
- Flyway migrations
- JPA with Hibernate

---

## Known Issues & Notes

1. **PowerShell File Creation:** Resolved using Set-Content cmdlet (heredoc not supported)
2. **Test Dependencies:** Using `spring-boot-data-jpa-test` (custom starter)
3. **Javadoc Style:** Match Phase 1 documentation patterns

---

## Phase 3 Preparation

After Phase 2 completion, Phase 3 will implement:
- **OahspeIngestionRunner:** PDF extraction + orchestration
- PDF text extraction with Apache PDFBox
- Page-by-page processing pipeline
- End-to-end integration tests

