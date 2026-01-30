# **Oahspe PDF Ingestion - Best Order of Action**

This document defines the optimal **strategic roadmap** for implementing the Oahspe PDF ingestion pipeline, based on comprehensive analysis of all documentation files in this folder.

The plan is organized into **5 phases**, progressing from foundational components to end-to-end testing.

---

## **Phase 1: Core Parser Implementation** (Foundation)

### **1.1 Implement `OahspeParser`**

**Priority:** CRITICAL  
**Dependencies:** None

The parser is the foundation of the entire pipeline. It converts raw PDF text lines into strongly-typed events.

**Key Responsibilities:**
- Implement the state machine with states: `OUTSIDE_BOOK`, `IN_BOOK`, `IN_CHAPTER`, `IN_VERSE`, `IN_NOTE`
- Apply regex patterns to detect:
  - Book titles (English & Chinese)
  - Chapter headers
  - Verse markers (`14/7.1` format)
  - Note markers (`(1)` format)
  - Image references (`i002` format)
- Emit strongly-typed events:
  - `BookStart(String title)`
  - `ChapterStart(String title)`
  - `Verse(String verseKey, String text)`
  - `Note(String noteKey, String text)`
  - `ImageRef(String imageKey, String caption)`
  - `PageBreak(int pageNumber)`
- Handle continuation lines correctly

**Reference:** [oahspe_ingestion_workflow.md](oahspe_ingestion_workflow.md#5-oahspeparser-implementation)

**Location:** `src/main/java/edu/minghualiu/oahspe/ingestion/OahspeParser.java`

---

### **1.2 Unit Tests for `OahspeParser`**

**Priority:** HIGH  
**Dependencies:** OahspeParser implementation

**Test Cases (from test plan):**
- **P1:** Detect book titles correctly
- **P2:** Detect chapter titles correctly
- **P3:** Detect verse lines with keys and text
- **P4:** Detect note lines with keys and text
- **P5:** Detect image references with keys and captions
- **P6:** Handle continuation lines (lines without markers)

**Reference:** [oahspe_ingestion_testplan.md](oahspe_ingestion_testplan.md#31-oahspeparser-tests)

**Location:** `src/test/java/edu/minghualiu/oahspe/ingestion/OahspeParserTest.java`

---

## **Phase 2: Ingestion Service Layer**

### **2.1 Implement `OahspeIngestionService`**

**Priority:** CRITICAL  
**Dependencies:** OahspeParser, all repositories, ImageNoteLinker

The ingestion service is the **central coordinator** that:
- Consumes parser events
- Maintains context (currentBook, currentChapter, currentVerse, currentNote)
- Builds the complete JPA entity graph
- Persists the hierarchy via cascade rules

**Key Responsibilities:**
- Process `BookStart` events → create new `Book`
- Process `ChapterStart` events → create new `Chapter`, link to current book
- Process `Verse` events → create or append to current verse
- Process `Note` events → create or append to current note
- Process `ImageRef` events → create image and link via `ImageNoteLinker`
- Call `bookRepository.save(book)` to persist entire hierarchy

**Reference:** [oahspe_ingestion_service.md](oahspe_ingestion_service.md#4-ingestion-service-class)

**Location:** `src/main/java/edu/minghualiu/oahspe/ingestion/OahspeIngestionService.java`

---

### **2.2 Verify & Enhance `ImageNoteLinker`**

**Priority:** HIGH  
**Dependencies:** ImageRepository, NoteRepository

This component already exists in the codebase but should be verified to:
- Persist images before linking (avoid transient-entity exceptions)
- Handle idempotent linking (no duplicate join table entries)
- Use `@Transactional` for consistency

**Reference:** [oahspe_ingestion_workflow.md](oahspe_ingestion_workflow.md#6-imagenotelinker-helper)

**Current Location:** `src/main/java/edu/minghualiu/oahspe/ingestion/ImageNoteLinker.java`

---

### **2.3 Integration Tests**

**Priority:** HIGH  
**Dependencies:** OahspeParser, OahspeIngestionService, all repositories

**Test Cases:**
- **I1:** Parser → IngestionService integration
  - Parse sample chapter with verse, note, and image
  - Verify all entities created and linked in database
  
- **I2:** Image extraction from PDF page
  - Extract image from synthetic PDF
  - Verify correct metadata (imageKey, sourcePage, contentType, data)
  
- **I3:** Full page integration
  - Parse + ingest + extract images from one PDF page
  - Validate complete entity graph

**Reference:** [oahspe_ingestion_testplan.md](oahspe_ingestion_testplan.md#4-integration-test-plan)

**Location:** `src/test/java/edu/minghualiu/oahspe/ingestion/OahspeIngestionIntegrationTest.java`

---

## **Phase 3: Orchestration Layer**

### **3.1 Implement `OahspeIngestionRunner`**

**Priority:** CRITICAL  
**Dependencies:** OahspeParser, OahspeIngestionService, ImageRepository, PDFBox library

The runner is the **top-level orchestrator** that drives the entire pipeline end-to-end.

**Key Responsibilities:**
- Load PDF using PDFBox (`PDDocument`)
- Iterate through all pages
- For each page:
  - Extract text using `PDFTextStripper`
  - Extract images using `PDResources` and `PDImageXObject`
  - Parse text into events via `OahspeParser`
  - Ingest events via `OahspeIngestionService`
  - Extract and persist images
  - Log progress (page number, book/chapter transitions)
- Handle errors gracefully (log and continue)
- Support restart-safe ingestion (idempotent operations)

**Configuration:**
- Add to `application.properties`: `oahspe.pdf.path=/path/to/oahspe.pdf`

**Reference:** [oahspe_ingestion_runner.md](oahspe_ingestion_runner.md)

**Location:** `src/main/java/edu/minghualiu/oahspe/ingestion/OahspeIngestionRunner.java`

**Implementation Style:**
- Implement as Spring `@Component` with `CommandLineRunner` or `@Scheduled` job
- Use dependency injection for parser, service, and repositories
- Use try-with-resources for PDDocument management

---

## **Phase 4: Testing Infrastructure & Validation**

### **4.1 Create Test Data Builders**

**Priority:** MEDIUM  
**Dependencies:** Entity classes

Build fluent, readable test builders for clean test setup.

**Classes to Create:**
- `BookBuilder` - fluent builder for Book entities
- `ChapterBuilder` - fluent builder for Chapter entities
- `VerseBuilder` - fluent builder for Verse entities
- `NoteBuilder` - fluent builder for Note entities
- `ImageBuilder` - fluent builder for Image entities

**Reference:** [oahspe_ingestion_test_data.md](oahspe_ingestion_test_data.md#1-test-data-builder-classes-recommended-for-unit--integration-tests)

**Location:** `src/test/java/edu/minghualiu/oahspe/testdata/`

---

### **4.2 Data Integrity & Idempotency Tests**

**Priority:** HIGH  
**Dependencies:** All ingestion components, test data builders

**Test Cases:**
- **E1:** Cascade save validation
  - Build Book → Chapter → Verse → Note hierarchy
  - Save Book
  - Verify all children persisted

- **E2:** Unique constraint validation
  - Attempt to insert duplicate `verseKey`, `noteKey`, `imageKey`
  - Verify constraint violations thrown

- **L1:** Image-to-note linking
  - Create note and image
  - Link via `ImageNoteLinker`
  - Verify join table populated

- **L2:** Idempotent linking
  - Link same image to note twice
  - Verify no duplicate join table entries

**Reference:** [oahspe_ingestion_testplan.md](oahspe_ingestion_testplan.md#32-imagenotelinker-tests)

**Location:** `src/test/java/edu/minghualiu/oahspe/ingestion/OahspeIngestionIdempotencyTest.java`

---

### **4.3 End-to-End System Test**

**Priority:** HIGH  
**Dependencies:** Complete ingestion pipeline, synthetic test PDF

**Test Scenario:**
- Create or obtain a small synthetic PDF with:
  ```
  Book of Osiris
  Chapter 1
  18/1.1 Osiris said...
  (1) A note about this verse
  i003 A plate image
  ```

- Run full ingestion pipeline
- Validate database state:
  - 1 Book record
  - 1 Chapter record
  - 1 Verse record
  - 1 Note record
  - 1 Image record
  - 1 join table entry linking note and image

**Reference:** [oahspe_ingestion_testplan.md](oahspe_ingestion_testplan.md#5-system-test-plan)

**Location:** `src/test/java/edu/minghualiu/oahspe/OahspeIngestionSystemTest.java`

---

## **Phase 5: Configuration & Execution**

### **5.1 Configure Application Properties**

**Priority:** MEDIUM  
**Dependencies:** None

Add to `src/main/resources/application.properties`:

```properties
# Oahspe PDF Configuration
oahspe.pdf.path=C:/path/to/oahspe.pdf

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/oahspe
spring.datasource.username=root
spring.datasource.password=password

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Logging
logging.level.edu.minghualiu.oahspe=INFO
logging.level.org.hibernate.SQL=DEBUG
```

---

### **5.2 Add Required Dependencies**

**Priority:** HIGH  
**Dependencies:** pom.xml configured

Ensure `pom.xml` includes:
- PDFBox: `org.apache.pdfbox:pdfbox`
- Spring Data JPA
- MySQL/H2 (testing)
- Lombok (optional, for entity builders)

---

### **5.3 Test with Actual or Synthetic PDF**

**Priority:** HIGH  
**Dependencies:** All previous phases complete

1. Prepare a test PDF (synthetic or sample from actual Oahspe)
2. Update `oahspe.pdf.path` to point to test file
3. Run `OahspeIngestionRunner`
4. Verify all entities created in database
5. Inspect book/chapter/verse/note/image counts
6. Validate image data integrity (file size, format)

---

### **5.4 Final Validation**

**Priority:** MEDIUM

- Run full test suite: `mvn clean test`
- Run system test with actual PDF
- Verify no memory leaks (large PDF processing)
- Check restart idempotency (run ingestion twice, verify no duplicates)
- Validate data quality (all verses correctly parsed, images linked)

---

## **Critical Path Summary**

```
Phase 1: OahspeParser Implementation
        ↓
        → OahspeParser Unit Tests
        ↓
Phase 2: OahspeIngestionService Implementation
        ↓
        → Integration Tests (Parser + Service)
        ↓
Phase 3: OahspeIngestionRunner Implementation
        ↓
Phase 4: Test Data Builders
        ↓
        → Idempotency & Integrity Tests
        ↓
        → End-to-End System Test
        ↓
Phase 5: Configuration & Execution
        ↓
        → Final Validation with Real PDF
```

---

## **Current Status**

### ✅ Already Implemented
- All 5 entity classes (Book, Chapter, Verse, Note, Image)
- All 5 repository interfaces
- `ImageNoteLinker` component

### ❌ Must Build (In Order)
1. `OahspeParser` + unit tests
2. `OahspeIngestionService` + integration tests
3. `OahspeIngestionRunner`
4. Test data builders
5. System tests and idempotency tests

---

## **Key Design Principles**

1. **Deterministic**: Same PDF → Same database structure every time
2. **Idempotent**: Running ingestion twice = no duplicates
3. **Modular**: Each layer (parser, service, runner) is independently testable
4. **Traceable**: Every entity linked to source page number
5. **Restart-Safe**: Unique keys (`verseKey`, `noteKey`, `imageKey`) enable safe re-runs

---

## **References**

All documentation files in this folder:
- [oahspe_ingestion_workflow.md](oahspe_ingestion_workflow.md) - Architecture & event model
- [oahspe_ingestion_service.md](oahspe_ingestion_service.md) - Service layer details
- [oahspe_ingestion_runner.md](oahspe_ingestion_runner.md) - Runner orchestration
- [oahspe_ingestion_testplan.md](oahspe_ingestion_testplan.md) - Complete test strategy
- [oahspe_ingestion_test_data.md](oahspe_ingestion_test_data.md) - Test data builders

---

**Document Version:** 1.0  
**Last Updated:** January 30, 2026  
**Status:** Ready for Implementation
