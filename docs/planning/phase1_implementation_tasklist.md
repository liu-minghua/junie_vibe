# Phase 1: Core Parser Implementation - Detailed Task List

**Version:** 1.0  
**Status:** Ready for Implementation  
**Last Updated:** January 30, 2026

This document provides granular task lists for Phase 1, breaking down OahspeParser implementation and unit tests into actionable items with clear acceptance criteria, time estimates, and success metrics.

---

## Overview

**Phase 1 Duration:** ~3-5 days (depending on team size and parallelization)  
**Phase 1 Deliverables:**
- OahspeParser class with state machine
- 12 comprehensive unit tests (P1-P6 + edge cases + performance)
- ParserState enum
- OahspeEvent interface & records
- Regex pattern definitions
- Documentation and usage guide

**Success Criteria:**
- ✅ OahspeParser compiles and runs
- ✅ All 12 unit tests pass
- ✅ Code coverage > 90%
- ✅ `mvn clean test` succeeds
- ✅ Deterministic behavior (same input → same events)
- ✅ Full Javadoc documentation
- ✅ Zero compiler warnings

---

## Task Group 1.1: Project Setup & Dependencies

### Task 1.1.1: Add PDFBox Dependency

**Status:** `[ ]` Not Started  
**Estimated Effort:** 15 minutes  
**Priority:** CRITICAL  
**Owner:** DevOps/Engineering

**Description:**
Add Apache PDFBox library to project dependencies. This library will be used later in Phase 3 for PDF extraction.

**Acceptance Criteria:**
- [ ] PDFBox dependency added to `pom.xml` (recommend version 2.0.28 or latest stable)
- [ ] Dependency block includes:
  ```xml
  <dependency>
      <groupId>org.apache.pdfbox</groupId>
      <artifactId>pdfbox</artifactId>
      <version>2.0.28</version>
  </dependency>
  ```
- [ ] No other PDF libraries conflict with PDFBox
- [ ] `mvn clean install` executes successfully
- [ ] All PDFBox classes available for import in IDE

**Related Files:**
- `pom.xml`

**Validation Command:**
```bash
mvn dependency:tree | grep pdfbox
```

---

### Task 1.1.2: Create Ingestion Package Structure

**Status:** `[ ]` Not Started  
**Estimated Effort:** 10 minutes  
**Priority:** HIGH  
**Owner:** Engineering

**Description:**
Create Maven package directories to organize ingestion-related classes.

**Acceptance Criteria:**
- [ ] Package `edu.minghualiu.oahspe.ingestion` created at:
  - `src/main/java/edu/minghualiu/oahspe/ingestion/`
- [ ] Package `edu.minghualiu.oahspe.ingestion.parser` created at:
  - `src/main/java/edu/minghualiu/oahspe/ingestion/parser/`
- [ ] Package `edu.minghualiu.oahspe.testdata` created at:
  - `src/test/java/edu/minghualiu/oahspe/testdata/`
- [ ] Directory structure visible in IDE Explorer
- [ ] All directories have `__init__` support (Maven auto-handles this)

**Related Files:**
- Directory creation only (no code files yet)

---

### Task 1.1.3: Verify Existing Entity Classes

**Status:** `[ ]` Not Started  
**Estimated Effort:** 20 minutes  
**Priority:** HIGH  
**Owner:** Engineering

**Description:**
Ensure all 5 entity classes exist, compile, and have proper JPA/Lombok annotations.

**Acceptance Criteria:**
- [ ] All 5 entity files exist:
  - `src/main/java/edu/minghualiu/oahspe/entities/Book.java`
  - `src/main/java/edu/minghualiu/oahspe/entities/Chapter.java`
  - `src/main/java/edu/minghualiu/oahspe/entities/Verse.java`
  - `src/main/java/edu/minghualiu/oahspe/entities/Note.java`
  - `src/main/java/edu/minghualiu/oahspe/entities/Image.java`
- [ ] Each entity has:
  - `@Entity` annotation
  - `@Table` annotation (with appropriate table names)
  - ID field with `@Id` and `@GeneratedValue`
  - Proper relationships:
    - Book has `@OneToMany` → Chapter
    - Chapter has `@ManyToOne` → Book and `@OneToMany` → Verse
    - Verse has `@ManyToOne` → Chapter and `@OneToMany` → Note
    - Note has `@ManyToOne` → Verse and `@ManyToMany` ↔ Image
    - Image has `@ManyToMany` ↔ Note
  - Lombok annotations (`@Builder`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- [ ] All entities compile without errors
- [ ] Run `mvn clean compile` successfully
- [ ] No warnings related to entity classes

**Related Files:**
- `src/main/java/edu/minghualiu/oahspe/entities/*.java`

**Validation Command:**
```bash
mvn clean compile
```

---

## Task Group 1.2: Define Event Model

### Task 1.2.1: Create OahspeEvent Interface & Records

**Status:** `[ ]` Not Started  
**Estimated Effort:** 30 minutes  
**Priority:** CRITICAL  
**Owner:** Engineering

**Description:**
Create a sealed interface with record implementations for parser events. This is the core data model that OahspeParser will emit and OahspeIngestionService will consume.

**Key Points:**
- Use sealed interface (Java 17+) for type safety and exhaustive pattern matching
- Records are immutable—perfect for event data
- verseKey and noteKey can be null to indicate continuation lines

**Acceptance Criteria:**
- [ ] File created: `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeEvent.java`
- [ ] `OahspeEvent` interface is sealed with explicit permits clause
- [ ] All 6 record implementations created:
  - `BookStart(String title)` — Book detected
  - `ChapterStart(String title)` — Chapter detected
  - `Verse(@Nullable String verseKey, String text)` — Verse with optional key
  - `Note(@Nullable String noteKey, String text)` — Note with optional key
  - `ImageRef(String imageKey, String caption)` — Image reference
  - `PageBreak(int pageNumber)` — Page transition marker
- [ ] All records are immutable (records auto-enforce this)
- [ ] All implement sealed `OahspeEvent` interface
- [ ] Optional imports included if using @Nullable annotation
- [ ] Class compiles without errors or warnings
- [ ] IDE recognizes all record types for pattern matching

**Code Template:**
```java
package edu.minghualiu.oahspe.ingestion.parser;

import java.util.Optional;

public sealed interface OahspeEvent permits
    OahspeEvent.BookStart,
    OahspeEvent.ChapterStart,
    OahspeEvent.Verse,
    OahspeEvent.Note,
    OahspeEvent.ImageRef,
    OahspeEvent.PageBreak {

    record BookStart(String title) implements OahspeEvent {}
    record ChapterStart(String title) implements OahspeEvent {}
    record Verse(String verseKey, String text) implements OahspeEvent {}
    record Note(String noteKey, String text) implements OahspeEvent {}
    record ImageRef(String imageKey, String caption) implements OahspeEvent {}
    record PageBreak(int pageNumber) implements OahspeEvent {}
}
```

**Related Files:**
- `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeEvent.java`

---

### Task 1.2.2: Create ParserState Enum

**Status:** `[ ]` Not Started  
**Estimated Effort:** 15 minutes  
**Priority:** HIGH  
**Owner:** Engineering

**Description:**
Define the parser state machine states that track parsing context and determine how continuation lines are interpreted.

**State Descriptions:**
- **OUTSIDE_BOOK**: Initial state, no book context yet
- **IN_BOOK**: Inside a book but outside any chapter
- **IN_CHAPTER**: Inside a chapter, ready to parse verses
- **IN_VERSE**: Inside a verse, continuation lines append to verse
- **IN_NOTE**: Inside a note, continuation lines append to note

**Acceptance Criteria:**
- [ ] File created: `src/main/java/edu/minghualiu/oahspe/ingestion/parser/ParserState.java`
- [ ] Enum with all 5 states:
  ```java
  public enum ParserState {
      OUTSIDE_BOOK,
      IN_BOOK,
      IN_CHAPTER,
      IN_VERSE,
      IN_NOTE
  }
  ```
- [ ] Class compiles without errors
- [ ] State names are clear and self-documenting
- [ ] Optional: Add Javadoc comments to each state

**Related Files:**
- `src/main/java/edu/minghualiu/oahspe/ingestion/parser/ParserState.java`

---

## Task Group 1.3: Implement OahspeParser Core Logic

### Task 1.3.1: Define Regex Patterns

**Status:** `[ ]` Not Started  
**Estimated Effort:** 45 minutes  
**Priority:** CRITICAL  
**Owner:** Engineering

**Description:**
Define all 5 regex patterns for detecting Oahspe structural markers. These patterns must be robust, support both English and Chinese text, and correctly extract capture groups.

**Pattern Requirements:**
- Each pattern compiled as `private static final Pattern`
- No runtime compilation (precompiled for performance)
- Support English and Chinese variants where applicable
- Capture groups must extract meaningful data
- Patterns should be case-sensitive or case-insensitive as appropriate

**Acceptance Criteria:**
- [ ] File: `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParser.java` (partially)
- [ ] All 5 patterns compile without regex errors:

#### Pattern 1: BOOK_PATTERN
- [ ] Matches: "Book of Apollo", "Book of Jehovih", "Book of Oahspe"
- [ ] Matches Chinese: "**之书" (handles wildcard between prefix and suffix)
- [ ] Capture group: Full title
- [ ] Regex: `^(Book of .+|.*?之.*?书)$`
- [ ] Test cases:
  - ✓ "Book of Apollo"
  - ✓ "Book of Jehovih"
  - ✗ "book of apollo" (lowercase - verify behavior)
  - ✗ "Chapter 7" (should not match)

#### Pattern 2: CHAPTER_PATTERN
- [ ] Matches: "Chapter 1", "Chapter 7", "Chapter 42"
- [ ] Matches Chinese: "第七章", "第一章"
- [ ] Capture group: Full chapter header
- [ ] Regex: `^(Chapter\\s+\\d+|第[一二三四五六七八九十百]+章)$`
- [ ] Test cases:
  - ✓ "Chapter 1"
  - ✓ "Chapter 42"
  - ✓ "Chapter  7" (multiple spaces)
  - ✗ "14/7.1" (verse, not chapter)

#### Pattern 3: VERSE_PATTERN
- [ ] Matches: "14/7.1 In the beginning...", "18/3.5 And it was..."
- [ ] Capture group 1: Verse key (14/7.1)
- [ ] Capture group 2: Verse text (everything after key)
- [ ] Regex: `^(\\d+\\/\\d+\\.\\d+)\\s+(.*)$`
- [ ] Test cases:
  - ✓ "14/7.1 In the beginning..."
  - ✓ "1/0.1 Jehovih spoke"
  - ✓ "100/99.99 Long verse key"
  - ✗ "14/7 wrong format" (missing second decimal)
  - ✗ "(1) This is a note" (note, not verse)

#### Pattern 4: NOTE_PATTERN
- [ ] Matches: "(1) This refers to...", "1) Also valid", "(42) Another note"
- [ ] Capture group 1: Note number (without parentheses)
- [ ] Capture group 2: Note text
- [ ] Regex: `^\\(?([0-9]+)\\)?\\s+(.*)$`
- [ ] Test cases:
  - ✓ "(1) This is a note"
  - ✓ "1) Also valid"
  - ✓ "(42) Multi-digit number"
  - ✗ "This is not a note"
  - ✗ "i002 Image reference"

#### Pattern 5: IMAGE_PATTERN
- [ ] Matches: "i002 Etherea Roadway", "i045 The divine plate"
- [ ] Capture group 1: 3-digit image number (002)
- [ ] Capture group 2: Image caption
- [ ] Regex: `^i(\\d{3})\\s+(.*)$`
- [ ] Test cases:
  - ✓ "i002 Etherea Roadway"
  - ✓ "i045 Caption"
  - ✓ "i001 First plate"
  - ✗ "i02 Only 2 digits" (must be exactly 3)
  - ✗ "i9999 4 digits" (must be exactly 3)

**Validation Approach:**
- Use online regex tester (regex101.com) to validate patterns before coding
- Create unit tests for pattern matching in Task 1.4.1
- Test with actual Oahspe text samples if available

**Related Files:**
- `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParser.java`

---

### Task 1.3.2: Implement Parser State Machine

**Status:** `[ ]` Not Started  
**Estimated Effort:** 60 minutes  
**Priority:** CRITICAL  
**Owner:** Engineering

**Description:**
Implement the core parser logic with state transitions. This is the main algorithm that converts raw text lines into structured events. The parser must be deterministic and handle all edge cases.

**Algorithm Overview:**
1. Initialize state to `OUTSIDE_BOOK`
2. For each input line:
   a. Trim whitespace
   b. Skip if empty
   c. Try patterns in order: Book → Chapter → Verse → Note → Image → Continuation
   d. On match: create event, update state, add to list
   e. On no match (continuation): append text to current context based on state
3. Return all events

**Acceptance Criteria:**
- [ ] Class created: `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParser.java`
- [ ] Annotated as `@Component` (Spring-managed bean)
- [ ] Private field: `private ParserState state = ParserState.OUTSIDE_BOOK`
- [ ] Public method signature: `public List<OahspeEvent> parse(List<String> lines, int pageNumber)`
- [ ] Method implementation:
  - [ ] Creates new `ArrayList<OahspeEvent>`
  - [ ] Adds `PageBreak(pageNumber)` event immediately
  - [ ] Iterates through each line
  - [ ] Trims whitespace: `line = line.trim()`
  - [ ] Skips empty lines: `if (line.isEmpty()) continue`
  - [ ] Attempts pattern matching in correct order
  - [ ] For each successful match:
    - [ ] Creates appropriate event record
    - [ ] Updates state machine
    - [ ] Adds event to list
  - [ ] Handles continuation lines based on state:
    - [ ] `IN_VERSE` → create `Verse(null, line)`
    - [ ] `IN_NOTE` → create `Note(null, line)`
    - [ ] Other states → skip (or log as unexpected)
  - [ ] Returns complete list of events

**State Transition Diagram:**
```
OUTSIDE_BOOK
    ↓ (BookStart detected)
IN_BOOK
    ↓ (ChapterStart detected)
IN_CHAPTER
    ├→ (Verse detected) → IN_VERSE
    │   ├→ (continuation) → stays IN_VERSE
    │   └→ (Note detected) → IN_NOTE
    │
    └→ (Note detected) → IN_NOTE
        ├→ (continuation) → stays IN_NOTE
        └→ (Verse detected) → IN_VERSE
```

**Pattern Matching Order (IMPORTANT):**
1. Book pattern → state = IN_BOOK
2. Chapter pattern → state = IN_CHAPTER
3. Verse pattern → state = IN_VERSE
4. Note pattern → state = IN_NOTE
5. Image pattern → (state unchanged)
6. Default (continuation) → append based on state

**Continuation Line Logic:**
- If state is `IN_VERSE` and line doesn't match any pattern → `Verse(null, line)`
- If state is `IN_NOTE` and line doesn't match any pattern → `Note(null, line)`
- Otherwise → skip or warn

**Code Outline:**
```java
@Component
public class OahspeParser {

    private ParserState state = ParserState.OUTSIDE_BOOK;

    // Regex patterns (from Task 1.3.1)
    private static final Pattern BOOK_PATTERN = ...;
    private static final Pattern CHAPTER_PATTERN = ...;
    // etc.

    public List<OahspeEvent> parse(List<String> lines, int pageNumber) {
        List<OahspeEvent> events = new ArrayList<>();
        state = ParserState.OUTSIDE_BOOK; // Reset state
        events.add(new OahspeEvent.PageBreak(pageNumber));

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Try patterns in order
            Matcher bookMatcher = BOOK_PATTERN.matcher(line);
            if (bookMatcher.matches()) {
                state = ParserState.IN_BOOK;
                events.add(new OahspeEvent.BookStart(line));
                continue;
            }

            // ... other patterns ...

            // Continuation line handling
            switch (state) {
                case IN_VERSE -> events.add(new OahspeEvent.Verse(null, line));
                case IN_NOTE -> events.add(new OahspeEvent.Note(null, line));
                // other states: skip or log
            }
        }

        return events;
    }
}
```

**Important Notes:**
- Each call to `parse()` should reset state to `OUTSIDE_BOOK`
- Parser must be thread-safe (consider making state local if needed)
- Events must be emitted in order they're detected
- PageBreak should be first event

**Related Files:**
- `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParser.java`

---

### Task 1.3.3: Add Parser Logging

**Status:** `[ ]` Not Started  
**Estimated Effort:** 20 minutes  
**Priority:** MEDIUM  
**Owner:** Engineering

**Description:**
Add SLF4J logging throughout parser for debugging, monitoring, and progress tracking.

**Acceptance Criteria:**
- [ ] SLF4J logger field added:
  ```java
  private static final Logger log = LoggerFactory.getLogger(OahspeParser.class);
  ```
- [ ] DEBUG level logs added for:
  - [ ] Each detected book: `log.debug("Detected book: {}", title)`
  - [ ] Each detected chapter: `log.debug("Detected chapter: {}", title)`
  - [ ] Each detected verse: `log.debug("Detected verse: {} with {} chars", verseKey, textLength)`
  - [ ] State transitions: `log.debug("State transition: {} -> {}", oldState, newState)`
- [ ] TRACE level logs (optional) for:
  - Each line being processed
  - Pattern matching attempts
- [ ] WARN level logs for:
  - Unexpected lines that don't match any pattern: `log.warn("Unexpected line content: {}", line)`
- [ ] All logging uses parameterized messages (no string concatenation)
  - ✓ Correct: `log.debug("Verse: {}", key)`
  - ✗ Wrong: `log.debug("Verse: " + key)`
- [ ] No INFO level logs (too verbose during normal operation)
- [ ] Logger properly initialized and accessible

**Related Files:**
- `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParser.java`

**Configuration:**
Users can control log levels via `application.properties`:
```properties
logging.level.edu.minghualiu.oahspe.ingestion.parser=DEBUG
```

---

## Task Group 1.4: Parser Unit Tests (12 Test Cases)

### Task 1.4.1: Create OahspeParserTest Class

**Status:** `[ ]` Not Started  
**Estimated Effort:** 20 minutes  
**Priority:** HIGH  
**Owner:** QA/Engineering

**Description:**
Set up the test class infrastructure with JUnit 5 annotations, fixtures, and assertion helpers.

**Acceptance Criteria:**
- [ ] File created: `src/test/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParserTest.java`
- [ ] Test class structure:
  ```java
  @DisplayName("OahspeParser Unit Tests")
  class OahspeParserTest {

      private OahspeParser parser;

      @BeforeEach
      void setUp() {
          parser = new OahspeParser();
      }

      // Test methods go here
  }
  ```
- [ ] JUnit 5 annotations used:
  - [ ] `@DisplayName` on class and test methods
  - [ ] `@BeforeEach` for setup
  - [ ] `@Test` on each test method
  - [ ] `@ParameterizedTest` if testing multiple inputs
- [ ] Assertions use JUnit 5:
  - `assertEquals(expected, actual)`
  - `assertTrue(condition)`
  - `assertFalse(condition)`
  - `assertNotNull(obj)`
- [ ] Test naming convention: `testXxx()` or `test_feature_scenario()`
  - Example: `test_P1_DetectBookTitles()`
- [ ] One assertion per test (or logically grouped related assertions)
- [ ] Class compiles and imports resolve
- [ ] IDE recognizes as test class (green test icon)

**Test Execution:**
```bash
mvn test -Dtest=OahspeParserTest
```

**Related Files:**
- `src/test/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParserTest.java`

---

### Task 1.4.2: Test P1 - Detect Book Titles

**Status:** `[ ]` Not Started  
**Estimated Effort:** 20 minutes  
**Priority:** HIGH  
**Owner:** QA/Engineering

**Test Case ID:** P1  
**Test Method Name:** `test_P1_DetectBookTitles()`

**Description:**
Verify parser correctly detects book title lines and emits `BookStart` events with proper title extraction.

**Acceptance Criteria:**
- [ ] Test creates input: `List<String>` with single book title
- [ ] Calls: `List<OahspeEvent> result = parser.parse(input, 1)`
- [ ] Assertions:
  - [ ] Result size is 2 (PageBreak + BookStart)
  - [ ] First event is `PageBreak(1)`
  - [ ] Second event is `BookStart` with correct title
  - [ ] Title matches input exactly (no trimming or modifications)

**Test Cases:**
```
Input: ["Book of Apollo"]
Expected events:
  1. PageBreak(1)
  2. BookStart("Book of Apollo")

Input: ["Book of Jehovih"]
Expected events:
  1. PageBreak(1)
  2. BookStart("Book of Jehovih")

Input: ["Book of Oahspe"]
Expected events:
  1. PageBreak(1)
  2. BookStart("Book of Oahspe")
```

**Test Implementation:**
```java
@Test
@DisplayName("P1: Should detect book titles")
void test_P1_DetectBookTitles() {
    List<String> input = List.of("Book of Apollo");
    List<OahspeEvent> events = parser.parse(input, 1);

    assertEquals(2, events.size());
    assertInstanceOf(OahspeEvent.PageBreak.class, events.get(0));
    assertInstanceOf(OahspeEvent.BookStart.class, events.get(1));
    
    OahspeEvent.BookStart book = (OahspeEvent.BookStart) events.get(1);
    assertEquals("Book of Apollo", book.title());
}
```

---

### Task 1.4.3: Test P2 - Detect Chapter Titles

**Status:** `[ ]` Not Started  
**Estimated Effort:** 20 minutes  
**Priority:** HIGH  
**Owner:** QA/Engineering

**Test Case ID:** P2  
**Test Method Name:** `test_P2_DetectChapterTitles()`

**Description:**
Verify parser correctly detects chapter headers and emits `ChapterStart` events.

**Acceptance Criteria:**
- [ ] Test input: "Chapter 7"
- [ ] Assertions:
  - [ ] Result size is 2 (PageBreak + ChapterStart)
  - [ ] ChapterStart event contains "Chapter 7"
- [ ] Test multiple chapter numbers: 1, 7, 42, 99
- [ ] Parser state transitions to IN_CHAPTER

**Test Cases:**
```
Input: ["Chapter 7"]
Expected: PageBreak(1), ChapterStart("Chapter 7")

Input: ["Chapter 1"]
Expected: PageBreak(1), ChapterStart("Chapter 1")

Input: ["Chapter 42"]
Expected: PageBreak(1), ChapterStart("Chapter 42")
```

---

### Task 1.4.4: Test P3 - Detect Verse Lines

**Status:** `[ ]` Not Started  
**Estimated Effort:** 25 minutes  
**Priority:** HIGH  
**Owner:** QA/Engineering

**Test Case ID:** P3  
**Test Method Name:** `test_P3_DetectVerseLines()`

**Description:**
Verify parser correctly detects verse markers (format: number/chapter.verse) and correctly extracts both verse key and text content.

**Acceptance Criteria:**
- [ ] Test input: "14/7.1 In the beginning..."
- [ ] Assertions:
  - [ ] Verse event created
  - [ ] verseKey extracted: "14/7.1"
  - [ ] text extracted: "In the beginning..."
  - [ ] State transitions to IN_VERSE
- [ ] Test multiple verse formats:
  - 1/0.1 (book 1, verse 0.1)
  - 14/7.1 (book 14, chapter 7, verse 1)
  - 48/7.99 (high numbers)

**Test Cases:**
```
Input: ["14/7.1 In the beginning..."]
Expected:
  - Verse("14/7.1", "In the beginning...")
  - verseKey = "14/7.1"
  - text = "In the beginning..."

Input: ["1/0.1 Jehovih said..."]
Expected:
  - Verse("1/0.1", "Jehovih said...")

Input: ["48/7.99 Far future..."]
Expected:
  - Verse("48/7.99", "Far future...")
```

---

### Task 1.4.5: Test P4 - Detect Note Lines

**Status:** `[ ]` Not Started  
**Estimated Effort:** 20 minutes  
**Priority:** HIGH  
**Owner:** QA/Engineering

**Test Case ID:** P4  
**Test Method Name:** `test_P4_DetectNoteLines()`

**Description:**
Verify parser correctly detects note markers (both "(1)" and "1)" formats) and extracts note key and text.

**Acceptance Criteria:**
- [ ] Test inputs with both formats: "(1)" and "1)"
- [ ] Assertions:
  - [ ] Note event created
  - [ ] noteKey extracted without parentheses: "1"
  - [ ] text extracted correctly
  - [ ] State transitions to IN_NOTE
- [ ] Test multiple note numbers: 1, 42, 999

**Test Cases:**
```
Input: ["(1) This refers to..."]
Expected: Note("1", "This refers to...")

Input: ["1) Also valid format"]
Expected: Note("1", "Also valid format")

Input: ["(42) Multi-digit note"]
Expected: Note("42", "Multi-digit note")
```

---

### Task 1.4.6: Test P5 - Detect Image References

**Status:** `[ ]` Not Started  
**Estimated Effort:** 20 minutes  
**Priority:** HIGH  
**Owner:** QA/Engineering

**Test Case ID:** P5  
**Test Method Name:** `test_P5_DetectImageReferences()`

**Description:**
Verify parser correctly detects image markers (format: i### caption) and creates `ImageRef` events with proper key and caption.

**Acceptance Criteria:**
- [ ] Test input: "i002 Etherea Roadway"
- [ ] Assertions:
  - [ ] ImageRef event created
  - [ ] imageKey is "IMG002" (with IMG prefix)
  - [ ] caption is "Etherea Roadway"
- [ ] Test multiple image numbers: i001, i002, i045, i999
- [ ] Exactly 3 digits required (verified in Pattern test)

**Test Cases:**
```
Input: ["i002 Etherea Roadway"]
Expected: ImageRef("IMG002", "Etherea Roadway")

Input: ["i045 The divine plate"]
Expected: ImageRef("IMG045", "The divine plate")

Input: ["i001 Opening image"]
Expected: ImageRef("IMG001", "Opening image")
```

---

### Task 1.4.7: Test P6 - Continuation Lines

**Status:** `[ ]` Not Started  
**Estimated Effort:** 30 minutes  
**Priority:** HIGH  
**Owner:** QA/Engineering

**Test Case ID:** P6  
**Test Method Name:** `test_P6_ContinuationLines()`

**Description:**
Verify parser correctly handles multi-line verses and notes where continuation lines don't have markers. Continuation lines have `null` keys to distinguish them from new entries.

**Acceptance Criteria:**
- [ ] Test verse continuation:
  ```
  14/7.1 Jehovih said...
  and the Lords answered...
  ```
  Expected:
  - Verse("14/7.1", "Jehovih said...")
  - Verse(null, "and the Lords answered...")

- [ ] Test note continuation:
  ```
  (1) This is a note
  that spans multiple
  lines of text
  ```
  Expected:
  - Note("1", "This is a note")
  - Note(null, "that spans multiple")
  - Note(null, "lines of text")

- [ ] Test state remains consistent across continuations
- [ ] Continuation verseKey/noteKey is exactly null

**Test Cases:**

**Case 1: Verse continuation**
```java
List<String> input = List.of(
    "14/7.1 Jehovih said...",
    "and the Lords answered..."
);
List<OahspeEvent> events = parser.parse(input, 1);

assertEquals(3, events.size()); // PageBreak + Verse + Verse
OahspeEvent.Verse v1 = (OahspeEvent.Verse) events.get(1);
assertEquals("14/7.1", v1.verseKey());
OahspeEvent.Verse v2 = (OahspeEvent.Verse) events.get(2);
assertNull(v2.verseKey()); // Continuation has null key
```

**Case 2: Note continuation**
```java
List<String> input = List.of(
    "(1) This is a note",
    "that spans multiple",
    "lines of text"
);
```

**Case 3: Mixed (verse + continuation + note)**
```java
List<String> input = List.of(
    "Book of Apollo",
    "Chapter 7",
    "14/7.1 In the beginning...",
    "and it was good"
);
```

---

### Task 1.4.8: Test Pattern Edge Cases

**Status:** `[ ]` Not Started  
**Estimated Effort:** 30 minutes  
**Priority:** HIGH  
**Owner:** QA/Engineering

**Test Case ID:** Edge Cases  
**Test Method Name:** `test_PatternEdgeCases()`

**Description:**
Test edge cases and boundary conditions for regex patterns to ensure robustness.

**Acceptance Criteria:**
- [ ] Empty strings handled correctly (no NPE)
- [ ] Whitespace-only lines skipped
- [ ] Case sensitivity tested (verify actual behavior)
- [ ] Special characters in text preserved
- [ ] Very long lines processed without error
- [ ] Lines with leading/trailing whitespace trimmed before processing
- [ ] Multiple spaces between markers and text handled

**Test Scenarios:**

| Scenario | Input | Expected Behavior |
|----------|-------|-------------------|
| Empty string | `""` | Skipped (no event) |
| Whitespace only | `"   "` | Skipped (trimmed to empty) |
| Tab character | `"\t\t"` | Skipped (trimmed to empty) |
| Leading space | `"  Book of Apollo"` | Trimmed, then matched |
| Trailing space | `"Book of Apollo  "` | Trimmed correctly |
| Multiple spaces | `"14/7.1   Multiple   spaces"` | Text includes internal spaces |
| No space after key | `"14/7.1No space"` | Should not match (regex requires space) |
| Special chars | `"14/7.1 Special: @#$%"` | Preserved in text |
| Very long line | `"14/7.1 " + "x"*1000` | Processed without error |

---

### Task 1.4.9: Test Full Page Scenario

**Status:** `[ ]` Not Started  
**Estimated Effort:** 40 minutes  
**Priority:** MEDIUM  
**Owner:** QA/Engineering

**Test Case ID:** Full Page  
**Test Method Name:** `test_FullPageScenario()`

**Description:**
Test a realistic multi-element page with book, chapter, verses, notes, and images to ensure parser handles complete workflows correctly.

**Acceptance Criteria:**
- [ ] Create comprehensive input simulating a full page
- [ ] Verify correct number and order of events
- [ ] Verify state machine consistency throughout
- [ ] All events correctly formed and linked

**Test Data:**
```
Book of Apollo
Chapter 7
14/7.1 In the beginning Jehovih spoke to the hosts...
(1) This refers to the creation moment
14/7.2 And the hosts were assembled in the great council...
(2) The hosts are the divine angels of the creation
i003 The Divine Throne
14/7.3 And it was good...
```

**Expected Events (9 total):**
1. PageBreak(1)
2. BookStart("Book of Apollo")
3. ChapterStart("Chapter 7")
4. Verse("14/7.1", "In the beginning Jehovih spoke to the hosts...")
5. Note("1", "This refers to the creation moment")
6. Verse("14/7.2", "And the hosts were assembled in the great council...")
7. Note("2", "The hosts are the divine angels of the creation")
8. ImageRef("IMG003", "The Divine Throne")
9. Verse("14/7.3", "And it was good...")

**Test Implementation:**
```java
@Test
@DisplayName("Full page scenario with mixed elements")
void test_FullPageScenario() {
    List<String> input = List.of(
        "Book of Apollo",
        "Chapter 7",
        "14/7.1 In the beginning Jehovih spoke to the hosts...",
        "(1) This refers to the creation moment",
        "14/7.2 And the hosts were assembled in the great council...",
        "(2) The hosts are the divine angels of the creation",
        "i003 The Divine Throne",
        "14/7.3 And it was good..."
    );

    List<OahspeEvent> events = parser.parse(input, 1);

    assertEquals(9, events.size());
    assertInstanceOf(OahspeEvent.PageBreak.class, events.get(0));
    assertInstanceOf(OahspeEvent.BookStart.class, events.get(1));
    assertInstanceOf(OahspeEvent.ChapterStart.class, events.get(2));
    assertInstanceOf(OahspeEvent.Verse.class, events.get(3));
    assertInstanceOf(OahspeEvent.Note.class, events.get(4));
    // ... etc
}
```

---

### Task 1.4.10: Test Invalid Patterns

**Status:** `[ ]` Not Started  
**Estimated Effort:** 25 minutes  
**Priority:** MEDIUM  
**Owner:** QA/Engineering

**Test Case ID:** Invalid  
**Test Method Name:** `test_InvalidPatterns()`

**Description:**
Test that invalid or unexpected lines are handled gracefully without errors.

**Acceptance Criteria:**
- [ ] Random text that doesn't match patterns is ignored
- [ ] Malformed verse keys (wrong format) are not matched
- [ ] Malformed note numbers are not matched
- [ ] Image numbers outside expected range (3 digits) are not matched
- [ ] Parser continues processing after invalid line
- [ ] No exception thrown for invalid input
- [ ] Invalid lines result in no events (or appropriate handling)

**Test Scenarios:**

| Invalid Input | Expected | Reason |
|---------------|----------|--------|
| `"This is random text"` | Ignored | No pattern match |
| `"14/7 wrong format"` | Ignored | Missing second decimal |
| `"14.7.1 also wrong"` | Ignored | Wrong separator |
| `"(a) invalid note key"` | Ignored | Letter instead of number |
| `"i99 only 2 digits"` | Ignored | Image number not 3 digits |
| `"i9999 too many digits"` | Ignored | Image number not 3 digits |
| `"(1) Valid but state is OUTSIDE_BOOK"` | Ignored | Wrong context (no book) |

---

### Task 1.4.11: Test State Machine Transitions

**Status:** `[ ]` Not Started  
**Estimated Effort:** 30 minutes  
**Priority:** MEDIUM  
**Owner:** QA/Engineering

**Test Case ID:** State Transitions  
**Test Method Name:** `test_StateTransitions()`

**Description:**
Verify parser correctly maintains and transitions between state machine states throughout document parsing.

**Acceptance Criteria:**
- [ ] Initial state is `OUTSIDE_BOOK`
- [ ] Book pattern detected → state transitions to `IN_BOOK`
- [ ] Chapter pattern detected → state transitions to `IN_CHAPTER`
- [ ] Verse pattern detected → state transitions to `IN_VERSE`
- [ ] Note pattern detected → state transitions to `IN_NOTE`
- [ ] Continuation lines processed based on current state
- [ ] Parser can transition between different states throughout document
- [ ] State resets to `OUTSIDE_BOOK` on next parse() call

**State Transition Paths:**
```
Path 1: OUTSIDE_BOOK → IN_BOOK → IN_CHAPTER → IN_VERSE → IN_NOTE → IN_VERSE

Path 2: OUTSIDE_BOOK → IN_BOOK → IN_CHAPTER → IN_VERSE → (continuation) → (continues IN_VERSE)

Path 3: Full cycle with multiple chapters
```

**Test Implementation:**
```java
@Test
@DisplayName("Parser state transitions correctly")
void test_StateTransitions() {
    List<String> input = List.of(
        "Book of Apollo",        // OUTSIDE_BOOK → IN_BOOK
        "Chapter 7",             // IN_BOOK → IN_CHAPTER
        "14/7.1 Verse...",      // IN_CHAPTER → IN_VERSE
        "(1) Note...",          // IN_VERSE → IN_NOTE
        "continuation",         // stays IN_NOTE
        "14/7.2 Another verse" // IN_NOTE → IN_VERSE
    );

    List<OahspeEvent> events = parser.parse(input, 1);

    // Verify state transitions through event sequence
    assertEquals(7, events.size());
    assertTrue(events.get(1) instanceof OahspeEvent.BookStart);
    assertTrue(events.get(2) instanceof OahspeEvent.ChapterStart);
    assertTrue(events.get(3) instanceof OahspeEvent.Verse);
    assertTrue(events.get(4) instanceof OahspeEvent.Note);
}
```

---

### Task 1.4.12: Parser Performance Test

**Status:** `[ ]` Not Started  
**Estimated Effort:** 30 minutes  
**Priority:** MEDIUM  
**Owner:** QA/Engineering

**Test Case ID:** Performance  
**Test Method Name:** `test_ParserPerformance()`

**Description:**
Test parser performance with large inputs to ensure it can handle full PDF pages efficiently without memory leaks or slowdowns.

**Acceptance Criteria:**
- [ ] Create input with 1000+ lines
- [ ] Parser completes in < 1 second
- [ ] No OutOfMemoryError
- [ ] Event list size is reasonable (linear with input, not exponential)
- [ ] All 1000+ lines processed
- [ ] No performance degradation

**Test Implementation:**
```java
@Test
@DisplayName("Parser handles 1000+ lines efficiently")
void test_ParserPerformance() {
    // Generate 1000 lines of varied content
    List<String> input = new ArrayList<>();
    input.add("Book of Apollo");
    input.add("Chapter 7");
    
    for (int i = 0; i < 500; i++) {
        input.add(String.format("%d/7.%d Verse text...", (i % 50) + 1, i));
        input.add(String.format("(%d) Note text", i % 100));
    }

    long startTime = System.nanoTime();
    List<OahspeEvent> events = parser.parse(input, 1);
    long endTime = System.nanoTime();

    long durationMs = (endTime - startTime) / 1_000_000;

    assertEquals(1 + 1 + 1 + input.size() - 3, events.size()); // accounting for headers
    assertTrue(durationMs < 1000, "Parser took " + durationMs + "ms (should be < 1000ms)");
}
```

---

## Task Group 1.5: Integration & Validation

### Task 1.5.1: Run All Tests & Verify Coverage

**Status:** `[ ]` Not Started  
**Estimated Effort:** 30 minutes  
**Priority:** CRITICAL  
**Owner:** QA/Engineering

**Description:**
Execute full test suite and verify code coverage meets standards.

**Acceptance Criteria:**
- [ ] Command: `mvn clean test` executes successfully
- [ ] All 12 unit tests pass (0 failures)
- [ ] Code coverage for OahspeParser class > 90%
- [ ] All public methods tested
- [ ] All regex patterns tested with multiple inputs
- [ ] All state transitions tested
- [ ] All event types tested
- [ ] Edge cases covered

**Commands:**
```bash
# Run all tests
mvn clean test

# Check test results
mvn test

# Generate coverage report (requires jacoco plugin)
mvn clean test jacoco:report

# View coverage report
# Open target/site/jacoco/index.html in browser
```

**Expected Output:**
```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

Code coverage:
- OahspeParser: >90%
- OahspeEvent: 100%
- ParserState: 100%
```

---

### Task 1.5.2: Documentation - Parser Javadoc

**Status:** `[ ]` Not Started  
**Estimated Effort:** 30 minutes  
**Priority:** HIGH  
**Owner:** Engineering

**Description:**
Add comprehensive Javadoc comments to all parser classes for code documentation.

**Acceptance Criteria:**
- [ ] All public methods have Javadoc comments:
  - [ ] Description of what the method does
  - [ ] @param tags for parameters
  - [ ] @return tag describing return value
  - [ ] @throws tag for exceptions (if any)
  - [ ] Usage examples if complex

- [ ] OahspeEvent record classes documented:
  ```java
  /**
   * Emitted when a book title is detected in the PDF.
   * @param title The title of the book (e.g., "Book of Apollo")
   */
  record BookStart(String title) implements OahspeEvent {}
  ```

- [ ] ParserState enum documented:
  ```java
  /**
   * Parser state machine state.
   * OUTSIDE_BOOK: Initial state, no book context
   * IN_BOOK: Inside a book but outside chapter
   * IN_CHAPTER: Inside a chapter
   * IN_VERSE: Inside a verse (for continuation lines)
   * IN_NOTE: Inside a note (for continuation lines)
   */
  public enum ParserState { ... }
  ```

- [ ] Regex patterns documented (what they match):
  ```java
  /**
   * Matches book titles like "Book of Apollo"
   */
  private static final Pattern BOOK_PATTERN = ...;
  ```

- [ ] State machine documented (state transitions):
  ```java
  /**
   * Parses a list of lines and emits structured events.
   * 
   * The parser maintains a state machine:
   * OUTSIDE_BOOK → IN_BOOK → IN_CHAPTER → IN_VERSE
   * 
   * @param lines Raw text lines from PDF
   * @param pageNumber Source page number
   * @return List of OahspeEvent objects in order
   */
  public List<OahspeEvent> parse(List<String> lines, int pageNumber) { ... }
  ```

- [ ] Generate Javadoc:
  ```bash
  mvn javadoc:javadoc
  # View: target/site/apidocs/index.html
  ```

---

### Task 1.5.3: Create Sample Parser Usage Guide

**Status:** `[ ]` Not Started  
**Estimated Effort:** 20 minutes  
**Priority:** MEDIUM  
**Owner:** Documentation

**Description:**
Create code examples showing how to use OahspeParser for developers and future maintainers.

**Acceptance Criteria:**
- [ ] Code example showing basic usage:
  - How to instantiate OahspeParser
  - How to parse a list of lines
  - How to iterate over events
  - How to handle different event types

- [ ] Example code compiles and runs
- [ ] Examples use pattern matching (Java 17+) for event handling
- [ ] Examples included in:
  - [ ] Class-level Javadoc comment
  - [ ] Project README or wiki
  - [ ] Developer documentation

**Example Code:**
```java
// 1. Create parser
OahspeParser parser = new OahspeParser();

// 2. Prepare input lines
List<String> lines = Arrays.asList(
    "Book of Apollo",
    "Chapter 7",
    "14/7.1 In the beginning...",
    "(1) This refers to creation",
    "i003 Divine throne"
);

// 3. Parse
List<OahspeEvent> events = parser.parse(lines, 1);

// 4. Process events
events.forEach(event -> {
    switch (event) {
        case OahspeEvent.BookStart book -> 
            System.out.println("Book: " + book.title());
        case OahspeEvent.ChapterStart chapter -> 
            System.out.println("Chapter: " + chapter.title());
        case OahspeEvent.Verse verse -> 
            System.out.println("Verse " + verse.verseKey() + ": " + verse.text());
        case OahspeEvent.Note note -> 
            System.out.println("Note (" + note.noteKey() + "): " + note.text());
        case OahspeEvent.ImageRef img -> 
            System.out.println("Image: " + img.imageKey() + " - " + img.caption());
        case OahspeEvent.PageBreak pb -> 
            System.out.println("--- Page " + pb.pageNumber() + " ---");
    }
});
```

**Location:**
- Add to: `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParser.java` (class Javadoc)
- Add to: `docs/architecture/oahspe_ingestion_workflow.md` (section: "Parser Usage")
- Add to: `README.md` (under "Getting Started")

---

## Phase 1 Completion Checklist

Print this checklist and track progress:

```
=== PROJECT SETUP ===
[ ] 1.1.1 - Add PDFBox dependency to pom.xml
[ ] 1.1.2 - Create ingestion package structure
[ ] 1.1.3 - Verify all 5 entity classes

=== EVENT MODEL ===
[ ] 1.2.1 - Create OahspeEvent interface & records
[ ] 1.2.2 - Create ParserState enum

=== PARSER IMPLEMENTATION ===
[ ] 1.3.1 - Define 5 regex patterns
[ ] 1.3.2 - Implement state machine algorithm
[ ] 1.3.3 - Add SLF4J logging

=== UNIT TESTS (12 test cases) ===
[ ] 1.4.1  - Create OahspeParserTest class
[ ] 1.4.2  - Test P1: Detect book titles
[ ] 1.4.3  - Test P2: Detect chapter titles
[ ] 1.4.4  - Test P3: Detect verse lines
[ ] 1.4.5  - Test P4: Detect note lines
[ ] 1.4.6  - Test P5: Detect image references
[ ] 1.4.7  - Test P6: Continuation lines
[ ] 1.4.8  - Test edge cases
[ ] 1.4.9  - Test full page scenario
[ ] 1.4.10 - Test invalid patterns
[ ] 1.4.11 - Test state transitions
[ ] 1.4.12 - Test performance

=== INTEGRATION & VALIDATION ===
[ ] 1.5.1 - Run all tests & verify coverage (>90%)
[ ] 1.5.2 - Add comprehensive Javadoc
[ ] 1.5.3 - Create usage guide

=== FINAL VALIDATION ===
[ ] mvn clean test: ALL TESTS PASS
[ ] Code coverage > 90%
[ ] No compiler warnings
[ ] No runtime errors
[ ] Javadoc generated successfully
```

---

## Phase 1 Success Criteria

Phase 1 is **COMPLETE** when ALL of the following are true:

1. ✅ **OahspeParser class exists and compiles without errors**
   - File: `src/main/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParser.java`
   - No compile errors
   - No warnings related to parser

2. ✅ **All 12 unit tests pass**
   - Test file: `src/test/java/edu/minghualiu/oahspe/ingestion/parser/OahspeParserTest.java`
   - Tests: P1, P2, P3, P4, P5, P6 + EdgeCases + FullPage + Invalid + StateMachine + Performance
   - All green in IDE
   - All pass in Maven build

3. ✅ **Code coverage > 90%**
   - Command: `mvn clean test jacoco:report`
   - Coverage report shows >90% line coverage for OahspeParser
   - All branches covered

4. ✅ **Parser is deterministic**
   - Requirement: Same input always produces same events
   - Verify: Run same input through parser 10 times, get identical results

5. ✅ **Maven build succeeds**
   - Command: `mvn clean test`
   - Output: `[INFO] BUILD SUCCESS`
   - No failures or errors

6. ✅ **Javadoc is complete and accurate**
   - Command: `mvn javadoc:javadoc`
   - All public methods documented
   - No Javadoc warnings
   - Generated docs are readable

7. ✅ **No compiler warnings**
   - Maven build shows `0 warnings`
   - Code compiles cleanly

8. ✅ **Parser handles all test scenarios from testplan.md**
   - All test cases from [oahspe_ingestion_testplan.md](../architecture/oahspe_ingestion_testplan.md) pass
   - Parser behavior matches specification

---

## Estimated Timeline

| Task Group | Tasks | Est. Effort | Dependencies |
|-----------|-------|-------------|--------------|
| 1.1 Setup | 3 | 45 min | None |
| 1.2 Events | 2 | 45 min | 1.1 |
| 1.3 Parser | 3 | 125 min | 1.2 |
| 1.4 Tests | 12 | 275 min | 1.3 |
| 1.5 Validation | 3 | 80 min | 1.4 |
| **TOTAL** | **23** | **570 min (9.5 hrs)** | - |

**Parallel Execution Opportunities:**
- 1.1.1 & 1.1.2 can run in parallel (10 min)
- 1.4.2 through 1.4.12 can be developed in parallel by different team members

---

## Notes for Developers

1. **Read the docs first**: Before coding Task 1.3.1 (regex patterns), carefully read [oahspe_ingestion_workflow.md](../architecture/oahspe_ingestion_workflow.md#4-regex-patterns)

2. **Test regex patterns early**: Use regex101.com to validate patterns before coding parser

3. **One test at a time**: Implement one test case, get it green, move to next

4. **Use your IDE's test runner**: Run tests in IDE as you code, not just in Maven

5. **Check coverage regularly**: After each test, verify coverage is increasing

6. **Ask questions**: If unclear on any test case, refer back to this document or the testplan.md

---

**Document Version:** 1.0  
**Created:** January 30, 2026  
**Status:** Ready for Team Assignment
