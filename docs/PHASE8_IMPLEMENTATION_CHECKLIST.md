# Phase 8 Implementation Checklist

**Project:** Oahspe PDF Ingestion - Phase 8: TOC-Based Workflow  
**Start Date:** February 1, 2026  
**Status:** Not Started  
**Owner:** (Assign Developer)

---

## Pre-Implementation

- [ ] Read PHASE8_TOC_BASED_INGESTION_DESIGN.md (Architecture Overview section)
- [ ] Read PHASE8_QUICK_REFERENCE.md (One-page cheat sheet)
- [ ] Verify Phase 0 prerequisites (1831 pages in database)
- [ ] Set up database migrations
- [ ] Review all 65+ SQL verification queries
- [ ] Plan sprint/implementation schedule

---

## Phase 0: Prerequisites Verification

**Objective:** Verify Phase 1 (page loading) completed successfully

- [ ] Check total pages: `SELECT COUNT(*) FROM page_contents;` → Expected: 1831
- [ ] Check pages with content: All pages have non-null rawText
- [ ] Check TOC page (4): Has content length > 100 chars
- [ ] Check OAHSPE_BOOKS range: 1662 pages have correct category
- [ ] Update status: `--status` command shows Phase 0 ready

**Expected Time:** < 5 minutes

---

## Phase 0.5: PDF Foundation Layer - TextFragment Extraction

**Objective:** Extract geometry-aware text fragments from PDF to handle two-column layouts

**CRITICAL:** This phase is required before Phase 4 content parsing. Current rawText-only approach fails with two-column layout, verse breaks across columns, and interleaved footnotes.

### Entity Creation

- [ ] Create `PdfPage.java` entity (if not exists)
  - [ ] Fields: id, pageNumber, extractedText, processed, loadedAt
  - [ ] Relationship: OneToMany(TextFragments), OneToMany(PdfImages)

- [ ] Create `TextFragment.java` entity
  - [ ] Fields: id, pdfPage, text, x, y, width, height, fontName, fontSize, bold, italic, columnNumber, readingOrder
  - [ ] Indexes: idx_page_column, idx_reading_order

- [ ] Create `PdfImage.java` entity
  - [ ] Fields: id, pdfPage, data, mimeType, x, y, width, height, imageId
  - [ ] Relationship: ManyToOne(PdfPage)

### Database Migration

- [ ] Create migration for TextFragment table
  - [ ] Add columns with proper types and indexes
  - [ ] Add foreign key to PdfPage
  
- [ ] Create migration for PdfImage table
  - [ ] Add columns with proper types
  - [ ] Add foreign key to PdfPage

### Implementation Tasks

- [ ] Create `PdfGeometryExtractor.java` service
  - [ ] Dependency: PDFBox library (org.apache.pdfbox:pdfbox)
  - [ ] Method: `void extractGeometryFromPdf(File pdfFile)`
  - [ ] For each page:
    - [ ] Extract text with coordinates (PDFBox PDFTextStripper)
    - [ ] Extract images with coordinates
    - [ ] Create TextFragment for each text element
    - [ ] Store font properties (name, size, bold, italic)
    - [ ] Auto-detect column boundaries using x-coordinate clustering
    - [ ] Assign readingOrder based on y-coordinate within column
  
- [ ] Create `ColumnDetectionService.java`
  - [ ] Method: `List<Integer> detectColumnBoundaries(List<TextFragment>)`
  - [ ] Uses x-coordinate clustering to identify column edges
  - [ ] Returns x-positions of column boundaries
  
- [ ] Create `ReadingOrderCalculator.java`
  - [ ] Method: `void assignReadingOrder(List<TextFragment>)`
  - [ ] For each column: sort fragments by y-coordinate
  - [ ] Assign sequential reading order
  - [ ] Assign column numbers (0=left, 1=right)

- [ ] Add `--extract-geometry` command to IngestionCliRunner
  - [ ] Loads PDF file from configured location
  - [ ] Runs extraction
  - [ ] Saves all TextFragments and PdfImages
  - [ ] Logs statistics (total fragments, images, pages processed)

- [ ] Create unit tests
  - [ ] Test: TextFragments created for sample page
  - [ ] Test: Font properties extracted
  - [ ] Test: Column boundaries detected correctly
  - [ ] Test: Reading order assigned sequentially
  - [ ] Test: PdfImages extracted with coordinates

### Verification Tasks

- [ ] Verify TextFragment count: `SELECT COUNT(*) FROM text_fragments;` → Expected: >50,000
- [ ] Verify sample fragments:
  ```sql
  SELECT id, text, x_coordinate, y_coordinate, font_size, is_bold 
  FROM text_fragments 
  WHERE pdf_page_id IN (SELECT id FROM pdf_pages WHERE page_number = 10)
  LIMIT 10;
  ```
- [ ] Verify column detection:
  ```sql
  SELECT DISTINCT column_number FROM text_fragments;
  -- Expected: 0, 1 (left and right columns)
  ```
- [ ] Verify reading order:
  ```sql
  SELECT COUNT(*) as fragments_without_reading_order 
  FROM text_fragments WHERE reading_order IS NULL;
  -- Expected: 0
  ```
- [ ] Verify coverage: All pages 7-1668 have fragments
- [ ] Spot check coordinates:
  ```sql
  SELECT MAX(y_coordinate) as max_y FROM text_fragments 
  WHERE pdf_page_id = 1;
  -- Expected: ~600-800 (standard page height)
  ```

**Expected Time:** 4-6 hours (depends on PDF library setup)

---

## Phase 1: TOC Extraction

**Objective:** Parse Table of Contents to extract book metadata

### Implementation Tasks

- [ ] Create new folder: `src/main/java/edu/minghualiu/oahspe/ingestion/toc/`
- [ ] Create `BookMetadata.java` class
  - [ ] Fields: title, bookNumber, startPage, endPage
  - [ ] Method: getPageCount()
- [ ] Create `TableOfContentsParser.java` service
  - [ ] Dependency: PageContentRepository
  - [ ] Method: `List<BookMetadata> extractBooks()`
  - [ ] Regex pattern: `^(\d+)/\s+(.+?)\s{2,}(?:pg\s+)?(\d+)\s*$`
  - [ ] Format: `01/ Tae's Prayer...............pg 7` (numbers, title with dots, page)
  - [ ] Handle overlapping page ranges (log warnings)
  - [ ] Handle title extraction with dots (trim correctly)
  - [ ] **Mark Book 35 (Saphah) with skip flag** - different structure, handle separately later
- [ ] Add `--toc-extract` command to IngestionCliRunner
- [ ] Create unit tests for TableOfContentsParser
  - [ ] Test: Extracts all books from TOC (expected ~39)
  - [ ] Test: First entry is "Tae's Prayer" at page 7
  - [ ] Test: Book 35 marked as "SKIP" in output
  - [ ] Test: Last entry ends at page 1614 or nearby
  - [ ] Test: Handles overlapping ranges gracefully

### Verification Tasks

- [ ] Run `--toc-extract`
- [ ] Manually verify output shows all entries extracted
- [ ] Count total entries (record N, likely ~39, with Book 35 marked SKIP)
- [ ] Check for overlapping page ranges in log output
- [ ] Check 3 random book titles against PDF
- [ ] Verify page ranges are contiguous
- [ ] Log output shows: "N books extracted successfully" (N = actual count)

**Expected Time:** 2-3 hours

---

## Phase 2: Book Registration

**Objective:** Create N Book entities with metadata from TOC

### Entity Updates

- [ ] Update `Book.java` entity
  - [ ] Add field: `startPage` (Integer)
  - [ ] Add field: `endPage` (Integer)
  - [ ] Add field: `bookNumber` (Integer, unique)
  - [ ] Add index on bookNumber
  - [ ] Update builder

- [ ] Create database migration for Book table changes

### Implementation Tasks

- [ ] Create `BookRegistrationService.java`
  - [ ] Dependency: BookRepository, TableOfContentsParser
  - [ ] Method: `void registerBooks(List<BookMetadata>)`
  - [ ] Create Book entity for each BookMetadata
  - [ ] **SKIP Book 35 (Saphah)** - do not create entity for it
  - [ ] Save all in single transaction
  - [ ] Handle idempotency (can re-run safely)
- [ ] Add `--toc-register` command to IngestionCliRunner
- [ ] Create unit tests
  - [ ] Test: Creates 38 books (39 extracted - 1 skipped)
  - [ ] Test: All books have correct metadata
  - [ ] Test: Idempotent (can re-run)

### Verification Tasks

- [ ] Run `--toc-register`
- [ ] Verify count: `SELECT COUNT(*) FROM books;` → Expected: 38 (39 extracted - 1 skipped)
- [ ] Verify Book 35 NOT in database: `SELECT COUNT(*) FROM books WHERE id = 35;` → Expected: 0
- [ ] Verify first book: startPage = 7
- [ ] Verify last book: Check book number 39 is not in table (skipped)
- [ ] Verify page coverage: `SELECT SUM(end_page - start_page + 1) FROM books;` → Expected: ~1662
- [ ] Verify no gaps: Run gap detection query
- [ ] All 6 Phase 2 verification SQL queries pass

**Expected Time:** 2-3 hours

---

## Phase 3: Page Assignment

**Objective:** Link each page to its owning book

### Entity Updates

- [ ] Update `PageContent.java` entity
  - [ ] Add field: `bookId` (Integer, foreign key to Book)
  - [ ] Add ManyToOne relationship to Book
  - [ ] Create database migration

### Implementation Tasks

- [ ] Create `PageAssignmentService.java`
  - [ ] Dependency: PageContentRepository, BookRepository
  - [ ] Method: `void assignPagesToBooks()`
  - [ ] For each page 7-1668: Find owning book by range, set bookId
  - [ ] Log assignment counts
  - [ ] Handle idempotency
- [ ] Add `--assign-pages` command to IngestionCliRunner
- [ ] Create unit tests
  - [ ] Test: All 1662 pages assigned
  - [ ] Test: No pages assigned outside range
  - [ ] Test: Idempotent

### Verification Tasks

- [ ] Run `--assign-pages`
- [ ] Verify assigned pages: `SELECT COUNT(*) FROM page_contents WHERE book_id IS NOT NULL;` → Expected: ~1662
- [ ] Verify no unassigned: `SELECT COUNT(*) FROM page_contents WHERE category = 'OAHSPE_BOOKS' AND book_id IS NULL;` → Expected: 0
- [ ] Verify page count per book correct
- [ ] Verify book 1 has pages 7-148: Sample query
- [ ] All 5 Phase 3 verification SQL queries pass

**Expected Time:** 2-3 hours

---

## Phase 4: Content Parsing

**Objective:** Parse book content into Book → Chapters → Verses → Notes hierarchy

**CRITICAL:** This phase uses TextFragments (geometry-aware text) extracted in Phase 0.5, NOT concatenated rawText. This enables accurate parsing of two-column layouts and footnotes.

**Architecture Overview:**
```
For each book:
  Load all TextFragments for book pages
  Group by page and column (left vs right)
  Sort fragments by reading order (top-to-bottom per column)
  Reconstruct logical reading order (left column → right column)
  Detect structure using font properties (size, bold, italic)
  Extract chapters (large bold headers)
  Extract verses (superscript numbers, indented lines)
  Extract footnotes (footer area text, special markers)
```

### Pass 4A: Chapter Detection

- [ ] Create `ChapterDetectionService.java`
  - [ ] Method: `List<Chapter> detectChapters(List<TextFragment>, Book)`
  - [ ] Process fragments in reading order
  - [ ] Identify chapter headers: fontSize > threshold AND bold
  - [ ] Regex pattern: `^Chapter\s+(\d+)|^[A-Z][A-Z\s]+$` (all-caps titles)
  - [ ] Handle chapter title extraction
  - [ ] Record page number where chapter starts
- [ ] Create unit tests for chapter detection
  - [ ] Test: Detects chapters correctly from TextFragments
  - [ ] Test: Font properties used for detection
  - [ ] Test: Extracts chapter titles accurately
  - [ ] Test: Identifies chapter start page

### Pass 4B: Verse Extraction

- [ ] Create `VerseExtractionService.java`
  - [ ] Method: `List<Verse> extractVerses(List<TextFragment>, Chapter)`
  - [ ] Identify verse markers: superscript numbers (¹, ², ³...) or [N]
  - [ ] Extract verse content following marker
  - [ ] Handle multi-line verses (continue until next verse marker)
  - [ ] Detect verse keys: digit/letter.digit format
  - [ ] Handle continuation lines across columns/pages
- [ ] Create unit tests for verse extraction
  - [ ] Test: Extracts verses correctly from TextFragments
  - [ ] Test: Verse keys recognized in correct format
  - [ ] Test: Multi-line verses handled
  - [ ] Test: No duplicate verses

### Pass 4C: Footnote Extraction

- [ ] Create `FootnoteExtractionService.java`
  - [ ] Method: `void extractFootnotes(Verse, List<TextFragment>, int pageNumber)`
  - [ ] Identify footnotes: text in footer area (y > page_height - footer_margin)
  - [ ] Extract footnote markers: superscript numbers matching verse markers
  - [ ] Regex pattern: `^\(([^)]+)\)\s+(.*)$` or superscript-keyed text
  - [ ] Attach footnotes to correct verses (by matching marker numbers)
  - [ ] Handle orphaned footnotes (no matching verse marker)
- [ ] Create unit tests for footnote extraction
  - [ ] Test: Extracts footnotes correctly from footer area
  - [ ] Test: Footnote markers recognized
  - [ ] Test: Footnotes attached to correct verses
  - [ ] Test: Orphaned footnotes identified

### Main Orchestrator

- [ ] Create `ContentParsingService.java`
  - [ ] Dependency: ChapterDetectionService, VerseExtractionService, FootnoteExtractionService
  - [ ] Method: `void parseBook(Book, List<TextFragment>)`
  - [ ] Load all TextFragments for book
  - [ ] Orchestrate 3-pass parsing: Chapters → Verses → Footnotes
  - [ ] Each pass saves to database
  - [ ] Handle per-book transaction with REQUIRES_NEW
  - [ ] Log progress after each pass (chapters found, verses found, footnotes found)
- [ ] Add `--parse-book <N>` command to IngestionCliRunner
- [ ] Add `--parse-all-books` command to IngestionCliRunner
- [ ] Create integration tests
  - [ ] Test: Parse single book completely
  - [ ] Test: All entities created correctly
  - [ ] Test: TextFragments correctly processed
  - [ ] Test: Relationships correct

### Verification Tasks (After Single Book Test)

- [ ] Run `--parse-book 1` (test first!)
- [ ] Verify chapters created for Book 1: `SELECT COUNT(*) FROM chapters WHERE book_id = 1;` → Expected: >1
- [ ] Verify verses created: `SELECT COUNT(*) FROM verses WHERE chapter_id IN (...)`  → Expected: >5
- [ ] Verify verse keys valid format: `SELECT DISTINCT verse_key FROM verses WHERE chapter_id = 1;`
- [ ] Verify footnotes extracted if present: `SELECT COUNT(*) FROM footnotes;`
- [ ] Review SQL queries: check orphaned entities, count verification
- [ ] Manual spot-check: `SELECT * FROM chapters/verses/footnotes FOR Book 1`

### Full Parse (After Single Book Verified)

- [ ] Run `--parse-all-books`
- [ ] Monitor logs for progress (expected: pass 4A → 4B → 4C for each book)
- [ ] Verify chapters created: `SELECT COUNT(*) FROM chapters;` → Expected: 500-1000
- [ ] Verify verses created: `SELECT COUNT(*) FROM verses;` → Expected: 10,000-20,000
- [ ] Verify footnotes created: `SELECT COUNT(*) FROM footnotes;` → Expected: 1,000-5,000
- [ ] Verify no orphaned entities

**Expected Time:** 8-12 hours (single book testing takes time)

---

## Phase 5: Aggregation

**Objective:** Calculate verse/note counts and combine text content

### Entity Updates

- [ ] Update `Chapter.java` entity
  - [ ] Add field: `verseCount` (Integer, default 0)
  - [ ] Add field: `verseContent` (LONGTEXT)
  - [ ] Update builder

- [ ] Update `Verse.java` entity
  - [ ] Add field: `noteCount` (Integer, default 0)
  - [ ] Add field: `noteContent` (LONGTEXT)
  - [ ] Update builder

- [ ] Create database migration for Chapter and Verse changes

### Implementation Tasks

- [ ] Create `src/main/java/edu/minghualiu/oahspe/ingestion/aggregation/` folder

- [ ] Create `VerseAggregationService.java`
  - [ ] Dependency: VerseRepository
  - [ ] Method: `Verse aggregateVerse(Verse)`
  - [ ] Set noteCount = COUNT(notes WHERE verse_id = ?)
  - [ ] Set noteContent = Combined note text with keys: "(1) ...\n(2) ...\n"
  - [ ] Save updated verse

- [ ] Create `ChapterAggregationService.java`
  - [ ] Dependency: ChapterRepository
  - [ ] Method: `Chapter aggregateChapter(Chapter)`
  - [ ] Set verseCount = COUNT(verses WHERE chapter_id = ?)
  - [ ] Set verseContent = Combined verse text with keys: "1/1.1 ...\n1/1.2 ...\n"
  - [ ] Save updated chapter

- [ ] Create `BookAggregationService.java`
  - [ ] Dependency: BookRepository, VerseAggregationService, ChapterAggregationService
  - [ ] Method: `void aggregateBook(Book)`
  - [ ] Orchestrate: For each chapter → aggregate verses → aggregate chapter

- [ ] Create `AggregationOrchestrator.java`
  - [ ] Dependency: BookAggregationService
  - [ ] Method: `void aggregateAllBooks()`
  - [ ] Sequential book aggregation with logging
  - [ ] Handle transaction boundaries

- [ ] Add `--aggregate-book <N>` command to IngestionCliRunner
- [ ] Add `--aggregate-all` command to IngestionCliRunner

- [ ] Create unit tests
  - [ ] Test: Verse aggregation calculates correct counts
  - [ ] Test: Chapter aggregation combines verses
  - [ ] Test: Content fields populated correctly

### Verification Tasks (After Single Book Test)

- [ ] Run `--aggregate-book 1` (test first!)
- [ ] Verify verseCount set: `SELECT verse_count FROM chapters WHERE book_id = 1;`
- [ ] Verify verseContent populated: Check length > 0
- [ ] Verify noteCount set for verses with notes
- [ ] Verify noteContent populated: Check length > 0
- [ ] Review 3 SQL queries for Pass 5A, 3 for 5B

### Full Aggregation (After Single Book Verified)

- [ ] Run `--aggregate-all`
- [ ] Monitor logs for progress
- [ ] Verify all chapters have verseCount: `SELECT COUNT(*) FROM chapters WHERE verse_count IS NULL;` → Expected: 0
- [ ] Verify all verses have noteCount: `SELECT COUNT(*) FROM verses WHERE note_count IS NULL;` → Expected: 0
- [ ] Spot-check: `SELECT verse_count, verse_content FROM chapters LIMIT 5;`

**Expected Time:** 4-6 hours

---

## Phase 6: Final Verification

**Objective:** Comprehensive data integrity verification

### Implementation Tasks

- [ ] Create `FinalVerificationService.java`
  - [ ] Dependency: BookRepository, ChapterRepository, VerseRepository, NoteRepository
  - [ ] Method: `VerificationReport verifyCompleteWorkflow()`
  - [ ] Implement 20+ verification checks:
    - [ ] Check Set 1: Referential Integrity (no orphans)
    - [ ] Check Set 2: Hierarchical Completeness (no empty parents)
    - [ ] Check Set 3: Aggregation Consistency (counts match)
    - [ ] Check Set 4: Page Coverage (all pages assigned)
    - [ ] Check Set 5: Sanity Checks (expected ranges)
  - [ ] Generate human-readable report with ✓/✗ for each check
  - [ ] Return overall success/failure status

- [ ] Create VerificationReport class
  - [ ] Field: List<CheckResult> checks
  - [ ] Field: boolean successful
  - [ ] Method: String generateReport()
  - [ ] Method: List<String> getFailedChecks()

- [ ] Add `--verify` command to IngestionCliRunner
  - [ ] Calls FinalVerificationService.verifyCompleteWorkflow()
  - [ ] Prints detailed report
  - [ ] Returns exit code 0 (success) or 1 (failure)

- [ ] Create unit tests
  - [ ] Test: All checks execute
  - [ ] Test: Report generation
  - [ ] Test: Failure detection

### Verification Tasks

- [ ] Run `--verify`
- [ ] Review output report
- [ ] All checks should show ✓
- [ ] Expected statistics:
  - [ ] N books (from Phase 1)
  - [ ] 500-1000 chapters
  - [ ] 10,000-20,000 verses
  - [ ] 100-1,000 notes
  - [ ] 1,662 pages assigned
- [ ] Run all 20+ SQL queries from Phase 6 section
- [ ] All queries return expected results

**Expected Time:** 4-5 hours

---

## Phase 7: CLI & Orchestration

**Objective:** Integrate all services into cohesive CLI interface

### Implementation Tasks

- [ ] Create `IngestionOrchestrator.java` (main workflow coordinator)
  - [ ] Dependency: All phase services
  - [ ] Method: `void executeFullWorkflow()`
  - [ ] Orchestrate: Phase 0 → Phase 6 with gates
  - [ ] Handle failures and logging

- [ ] Update `IngestionCliRunner.java`
  - [ ] Add all new commands
  - [ ] Add `--status` command
  - [ ] Update `--help` output with new commands
  - [ ] Add error handling for each command

- [ ] Create `WorkflowStatus` enum or class
  - [ ] Track current phase
  - [ ] Track completion status
  - [ ] Provide human-readable status output

- [ ] Create integration test for full workflow
  - [ ] Test: Phase 0 → Phase 1 → Phase 2 ... Phase 6
  - [ ] Test: All N books processed
  - [ ] Test: Final verification passes

### Testing Tasks

- [ ] Manual test: Run --status
- [ ] Manual test: Run each command independently
- [ ] Manual test: Run full workflow sequence
- [ ] Manual test: Test error recovery (delete data, re-run phase)
- [ ] Performance test: Time each phase
- [ ] Spot-check: Verify 5 random books manually

**Expected Time:** 6-8 hours

---

## Documentation & Finalization

- [ ] Review PHASE8_TOC_BASED_INGESTION_DESIGN.md for completeness
- [ ] Update README.md to mention Phase 8
- [ ] Document any deviations from design
- [ ] Create troubleshooting guide (if issues found)
- [ ] Document regex patterns and their purpose
- [ ] Create database migration scripts
- [ ] Add code comments explaining key logic
- [ ] Create summary of implementation decisions

---

## Testing Checklist

### Unit Tests
- [ ] TableOfContentsParser: 5+ tests
- [ ] BookRegistrationService: 3+ tests
- [ ] PageAssignmentService: 3+ tests
- [ ] ChapterDetectionService: 5+ tests
- [ ] VerseExtractionService: 5+ tests
- [ ] NoteExtractionService: 4+ tests
- [ ] VerseAggregationService: 3+ tests
- [ ] ChapterAggregationService: 3+ tests
- [ ] FinalVerificationService: 5+ tests

### Integration Tests
- [ ] Single book parsing (Phase 4)
- [ ] Single book aggregation (Phase 5)
- [ ] Full N-book workflow
- [ ] Error recovery (Phase 4 failure)
- [ ] Error recovery (Phase 5 failure)

### Manual Tests
- [ ] --status command
- [ ] --toc-extract (verify N books extracted)
- [ ] --toc-register (verify DB)
- [ ] --assign-pages (verify DB)
- [ ] --parse-book 1 (verify output)
- [ ] --parse-all-books (verify complete)
- [ ] --aggregate-all (verify complete)
- [ ] --verify (verify all checks pass)

### Data Verification
- [ ] All 65+ SQL verification queries
- [ ] Spot-check 5 random books for correctness
- [ ] Check verse key format for sample verses
- [ ] Check note attachment for sample notes
- [ ] Verify no duplicate verses
- [ ] Verify no duplicate notes

**Expected Total Time:** 40-60 hours (5-7 days of development)

---

## Sign-Off Criteria

✅ All implementation tasks completed  
✅ All unit tests passing (100% coverage of critical paths)  
✅ All integration tests passing  
✅ All manual tests passing  
✅ All 65+ SQL verification queries return expected results  
✅ All N books fully parsed with 100% data integrity  
✅ Phase 6 verification shows all ✓  
✅ CLI commands documented and working  
✅ Error recovery procedures tested  
✅ Documentation complete and reviewed  
✅ Performance targets met (< 45 minutes sequential)  

---

## Implementation Notes

**Start Date:** _____________  
**Completed Date:** _____________  
**Developer:** _____________  

**Issues Encountered:**
```
(Document any issues and resolutions here)
```

**Performance Metrics:**
- Phase 1 Duration: _____ seconds
- Phase 2 Duration: _____ seconds
- Phase 3 Duration: _____ seconds
- Phase 4 Duration: _____ minutes
- Phase 5 Duration: _____ minutes
- Phase 6 Duration: _____ minutes
- Total Duration: _____ minutes

**Data Results:**
- Books Created: _____ (Expected: N from Phase 1)
- Chapters Created: _____ (Expected: 500-1000)
- Verses Created: _____ (Expected: 10,000-20,000)
- Notes Created: _____ (Expected: 100-1,000)

---

**Document Version:** 1.0  
**Created:** February 1, 2026  
**Status:** Ready for Implementation
