# Phase 7 Implementation Tasklist

**Total Estimated Time:** 27.5 hours (~3.5 working days)

**Objective:** Page-by-page PDF loading with Glossary and Index ingestion for translation preparation

---

## Task Group 7.1: Entity Creation (5 hours)

### ⏱️ 1 hour - PageCategory Enum
- [ ] Create `PageCategory.java` enum
- [ ] Add fields: label, startPage, endPage, shouldIngest, isStructuredContent
- [ ] Implement constructor and getters
- [ ] Add `requiresSpecialParser()` method
- [ ] Add static `fromPageNumber(int)` method
- [ ] Verify 6 categories: COVER, TABLE_OF_CONTENTS, IMAGE_LIST, OAHSPE_BOOKS, GLOSSARIES, INDEX

### ⏱️ 1 hour - PageContent Entity
- [ ] Create `PageContent.java` entity
- [ ] Add fields: id, pageNumber, category, rawText, extractedAt, ingested, ingestedAt, errorMessage
- [ ] Add JPA annotations (@Entity, @Table, @Id, @GeneratedValue)
- [ ] Add indexes on pageNumber, category, ingested
- [ ] Implement equals/hashCode/toString
- [ ] Add convenience methods: hasError(), markIngested()

### ⏱️ 45 min - PageImage Entity
- [ ] Create `PageImage.java` entity
- [ ] Add fields: id, pageContentId, imageSequence, imageData, mimeType, linkedImageId
- [ ] Add relationships: @ManyToOne to PageContent, @OneToOne to Image
- [ ] Add indexes on pageContentId, linkedImageId
- [ ] Implement equals/hashCode/toString

### ⏱️ 30 min - WorkflowState Entity
- [ ] Create `WorkflowState.java` entity
- [ ] Add fields: id, workflowName, currentPhase, status, startedAt, completedAt, statistics, lastError
- [ ] Add enum `WorkflowPhase`: PAGE_LOADING, CLEANUP, CONTENT_INGESTION, COMPLETED
- [ ] Add enum `WorkflowStatus`: NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED
- [ ] Implement equals/hashCode/toString

### ⏱️ 30 min - GlossaryTerm Entity
- [ ] Create `GlossaryTerm.java` entity
- [ ] Add fields: id, term, definition, pageNumber, termType, usageCount
- [ ] Add relationship: @ManyToOne to PageContent (via pageNumber)
- [ ] Add indexes on term (unique), termType
- [ ] Add future fields (commented): chineseSimplified, chineseTraditional, pinyin
- [ ] Implement equals/hashCode/toString

### ⏱️ 30 min - IndexEntry Entity
- [ ] Create `IndexEntry.java` entity
- [ ] Add fields: id, topic, pageReferences, glossaryTermId, extractedFromPage
- [ ] Add relationships: @ManyToOne to GlossaryTerm, @ManyToOne to PageContent
- [ ] Add index on topic
- [ ] Add future field (commented): chineseTopic
- [ ] Implement equals/hashCode/toString

### ⏱️ 45 min - Helper Classes
- [ ] Create `ContentLinkingReport.java`
  - Fields: totalPages, pagesLinked, imagesLinked, failedPages, orphanedImages
- [ ] Create `PageContentSummary.java`
  - Fields: pageNumber, category, hasText, imageCount, ingested, hasError
- [ ] Create `PageRangeContentSummary.java`
  - Fields: category, pageRange, totalPages, ingestedPages, errorCount

---

## Task Group 7.2: Entity Enhancements (1.5 hours)

### ⏱️ 30 min - Book Entity Enhancement
- [ ] Add `pageNumber` field (Integer, nullable)
- [ ] Add index: `@Index(name = "idx_book_page", columnList = "page_number")`
- [ ] Add getter/setter for pageNumber
- [ ] Update equals/hashCode if needed

### ⏱️ 20 min - Chapter Entity Enhancement
- [ ] Add `pageNumber` field (Integer, nullable)
- [ ] Add index: `@Index(name = "idx_chapter_page", columnList = "page_number")`
- [ ] Add getter/setter for pageNumber
- [ ] Update equals/hashCode if needed

### ⏱️ 20 min - Verse Entity Enhancement
- [ ] Add `pageNumber` field (Integer, nullable)
- [ ] Add index: `@Index(name = "idx_verse_page", columnList = "page_number")`
- [ ] Add getter/setter for pageNumber
- [ ] Update equals/hashCode if needed

### ⏱️ 20 min - Note Entity Enhancement
- [ ] Add `pageNumber` field (Integer, nullable)
- [ ] Add index: `@Index(name = "idx_note_page", columnList = "page_number")`
- [ ] Add getter/setter for pageNumber
- [ ] Update equals/hashCode if needed

---

## Task Group 7.3: Repository Creation (2.5 hours)

### ⏱️ 30 min - PageContentRepository
- [ ] Create `PageContentRepository.java` interface extending JpaRepository
- [ ] Add query: `Optional<PageContent> findByPageNumber(Integer)`
- [ ] Add query: `List<PageContent> findByCategory(PageCategory)`
- [ ] Add query: `List<PageContent> findByCategoryAndIngestedFalse(PageCategory)`
- [ ] Add query: `long countByCategory(PageCategory)`
- [ ] Add query: `long countByCategoryAndIngestedTrue(PageCategory)`
- [ ] Add query: `List<PageContent> findByPageNumberBetween(int, int)`

### ⏱️ 20 min - PageImageRepository
- [ ] Create `PageImageRepository.java` interface extending JpaRepository
- [ ] Add query: `List<PageImage> findByPageContentId(Long)`
- [ ] Add query: `List<PageImage> findByLinkedImageIdIsNull()`
- [ ] Add query: `long countByPageContentId(Long)`

### ⏱️ 20 min - WorkflowStateRepository
- [ ] Create `WorkflowStateRepository.java` interface extending JpaRepository
- [ ] Add query: `Optional<WorkflowState> findByWorkflowName(String)`
- [ ] Add query: `List<WorkflowState> findByStatus(WorkflowStatus)`

### ⏱️ 20 min - GlossaryTermRepository
- [ ] Create `GlossaryTermRepository.java` interface extending JpaRepository
- [ ] Add query: `Optional<GlossaryTerm> findByTerm(String)`
- [ ] Add query: `List<GlossaryTerm> findByTermType(String)`
- [ ] Add query: `List<GlossaryTerm> findByPageNumber(Integer)`
- [ ] Add query: `List<GlossaryTerm> findByTermContaining(String)` for fuzzy search

### ⏱️ 15 min - IndexEntryRepository
- [ ] Create `IndexEntryRepository.java` interface extending JpaRepository
- [ ] Add query: `Optional<IndexEntry> findByTopic(String)`
- [ ] Add query: `List<IndexEntry> findByGlossaryTermId(Long)`
- [ ] Add pagination support with `findAll(Pageable)`

### ⏱️ 20 min - BookRepository Enhancement
- [ ] Add query: `List<Book> findByPageNumber(Integer)`
- [ ] Add query: `List<Book> findByPageNumberBetween(int, int)`
- [ ] Add query: `long countByPageNumberIsNull()`

### ⏱️ 15 min - ChapterRepository Enhancement
- [ ] Add query: `List<Chapter> findByPageNumber(Integer)`
- [ ] Add query: `List<Chapter> findByPageNumberBetween(int, int)`
- [ ] Add query: `long countByPageNumberIsNull()`

### ⏱️ 20 min - VerseRepository Enhancement
- [ ] Add query: `List<Verse> findByPageNumber(Integer)`
- [ ] Add query: `List<Verse> findByPageNumberBetween(int, int)`
- [ ] Add query: `long countByPageNumberIsNull()`

### ⏱️ 10 min - NoteRepository Enhancement
- [ ] Add query: `List<Note> findByPageNumber(Integer)`
- [ ] Add query: `long countByPageNumberIsNull()`

---

## Task Group 7.4: Service Implementation (8.5 hours)

### ⏱️ 1.5 hours - PageLoader Service
- [ ] Create `PageLoader.java` service class with @Service annotation
- [ ] Inject: PDFTextExtractor, PDFImageExtractor, PageContentRepository, PageImageRepository
- [ ] Implement `IngestionContext loadAllPages(String pdfPath, ProgressCallback)`
  - [ ] Extract text from all 1831 pages
  - [ ] Determine PageCategory for each page
  - [ ] Save PageContent entities
  - [ ] Extract images to PageImage entities
  - [ ] Track progress with IngestionContext
- [ ] Implement `PageContent loadSinglePage(String pdfPath, int pageNumber)`
- [ ] Implement `PageRangeContentSummary getPageRangeSummary(PageCategory)`
- [ ] Add error handling and transaction management

### ⏱️ 1.5 hours - GlossaryParser Service
- [ ] Create `GlossaryParser.java` service class
- [ ] Implement `List<GlossaryTerm> parseGlossaryPage(String rawText, int pageNumber)`
- [ ] Implement `GlossaryTerm extractTermDefinition(String line)`
- [ ] Pattern recognition:
  - [ ] Detect terms (bold/uppercase patterns)
  - [ ] Extract definitions (same/next line)
  - [ ] Handle multi-line definitions
- [ ] Categorize term types: spiritual/person/place/concept
- [ ] Add unit tests for edge cases

### ⏱️ 1 hour - IndexParser Service
- [ ] Create `IndexParser.java` service class
- [ ] Implement `List<IndexEntry> parseIndexPage(String rawText, int pageNumber)`
- [ ] Implement `IndexEntry extractTopicReferences(String line)`
- [ ] Pattern recognition:
  - [ ] Extract topic names
  - [ ] Parse page numbers (single, ranges, comma-separated)
  - [ ] Handle cross-references ("see also...")
- [ ] Add unit tests for edge cases

### ⏱️ 2 hours - PageIngestionLinker Service
- [ ] Create `PageIngestionLinker.java` service class
- [ ] Inject: PageContentRepository, OahspeParser, GlossaryParser, IndexParser, OahspeIngestionService, GlossaryTermRepository, IndexEntryRepository
- [ ] Implement `IngestionContext ingestAllPageContents(ProgressCallback)`
  - [ ] Query all PageContent with shouldIngest=true
  - [ ] Process each page with appropriate parser
  - [ ] Save parsed data to DB
- [ ] Implement `void ingestSinglePageContent(PageContent pageContent, IngestionContext context)`
  - [ ] Determine parser by category (GLOSSARIES→GlossaryParser, INDEX→IndexParser, OAHSPE_BOOKS→OahspeParser)
  - [ ] Call appropriate parser
  - [ ] Pass pageNumber to ingestion service
  - [ ] Update PageContent.ingested status
- [ ] Implement `IngestionContext ingestCategoryPages(PageCategory category, ProgressCallback)`
- [ ] Implement `void linkPageImagesToImageEntities(PageContent pageContent)`
- [ ] Add error handling and rollback for individual pages

### ⏱️ 1.5 hours - WorkflowOrchestrator Service
- [ ] Create `WorkflowOrchestrator.java` service class
- [ ] Inject: PageLoader, IngestionDataCleanup, PageIngestionLinker, WorkflowStateRepository
- [ ] Implement `WorkflowState executeFullWorkflow(String pdfPath, ProgressCallback)`
  - [ ] Phase 1: loadAllPages()
  - [ ] Gate 1: verifyPageLoading()
  - [ ] Phase 2: cleanupOldData()
  - [ ] Gate 2: verifyCleanup()
  - [ ] Phase 3: ingestAllPageContents()
  - [ ] Gate 3: verifyIngestion()
- [ ] Implement verification gates:
  - [ ] `boolean verifyPageLoading()` - check 1831 pages loaded
  - [ ] `boolean verifyCleanup()` - check old data cleared
  - [ ] `boolean verifyIngestion()` - check all shouldIngest pages processed
- [ ] Implement `WorkflowState resumeWorkflow(String workflowName)`
- [ ] Add progress tracking with WorkflowState entity updates

### ⏱️ 30 min - IngestionDataCleanup Service
- [ ] Create `IngestionDataCleanup.java` service class
- [ ] Inject all entity repositories (Book, Chapter, Verse, Note, Image, GlossaryTerm, IndexEntry)
- [ ] Implement `void cleanupAllIngestedData()`
  - [ ] Delete all Books (cascade to Chapters/Verses/Notes)
  - [ ] Delete all orphaned Images
  - [ ] Delete all GlossaryTerms
  - [ ] Delete all IndexEntries
  - [ ] Log deletion counts
- [ ] Implement `void preservePageContents()` - verify PageContent/PageImage not deleted
- [ ] Add confirmation mechanism (require explicit flag to prevent accidents)
- [ ] Add @Transactional with proper isolation level

### ⏱️ 1 hour - ContentPageLinkingService
- [ ] Create `ContentPageLinkingService.java` service class
- [ ] Inject: BookRepository, ChapterRepository, VerseRepository, NoteRepository, PageContentRepository
- [ ] Implement `ContentLinkingReport linkAllContentToPages()`
  - [ ] For each Book/Chapter/Verse/Note, find corresponding PageContent
  - [ ] Update pageNumber field
  - [ ] Track linking statistics
- [ ] Implement `List<PageContentSummary> findUnlinkedPages()`
- [ ] Implement `List<Verse> findOrphanedVerses()` (pageNumber is null)
- [ ] Add validation reports

---

## Task Group 7.5: Update Existing Services (2 hours)

### ⏱️ 1 hour - OahspeIngestionService Enhancement
- [ ] Open `OahspeIngestionService.java`
- [ ] Update `processEvent()` method to accept `Integer pageNumber` parameter
- [ ] Modify entity creation:
  - [ ] Set `book.setPageNumber(pageNumber)` when creating Book
  - [ ] Set `chapter.setPageNumber(pageNumber)` when creating Chapter
  - [ ] Set `verse.setPageNumber(pageNumber)` when creating Verse
  - [ ] Set `note.setPageNumber(pageNumber)` when creating Note
- [ ] Update all callers to pass pageNumber
- [ ] Verify transaction boundaries

### ⏱️ 1 hour - PDFImageExtractor Enhancement
- [ ] Open `PDFImageExtractor.java`
- [ ] Create new method: `List<PageImage> extractImagesFromPageAsObjects(PDDocument doc, int pageNum, Long pageContentId)`
- [ ] Returns PageImage objects instead of saving to Image entities
- [ ] Keep existing `extractImagesFromPage()` for backward compatibility
- [ ] Add sequence numbering logic for PageImage.imageSequence
- [ ] Add unit tests

---

## Task Group 7.6: CLI Enhancement (2 hours) ✅

### ⏱️ 2 hours - IngestionCliRunner Enhancement ✅
- [x] Open `IngestionCliRunner.java`
- [x] Add new command-line options:
  - [x] `--workflow` - Execute full 3-phase workflow
  - [x] `--load-pages` - Phase 1 only (page loading)
  - [x] `--ingest-pages` - Phase 3 only (content ingestion)
  - [x] `--verify-links` - Run content-page linking verification
  - [x] `--cleanup` - Phase 2 only (data cleanup, with confirmation)
  - [x] `--resume` - Resume interrupted workflow
- [x] Inject WorkflowOrchestrator service
- [x] Implement command handlers:
  - [x] `runFullWorkflow()`
  - [x] `runPageLoading()`
  - [x] `runIngestion()`
  - [x] `runVerification()`
  - [x] `runCleanup()` with confirmation prompt
  - [x] `resumeWorkflow()`
- [x] Add progress callbacks with console output
- [x] Add summary statistics reporting
- [x] Update help text with new commands

---

## Task Group 7.7: Testing (4 hours)

### ⏱️ 1 hour - Unit Tests
- [ ] Test `PageCategory.fromPageNumber()` for all ranges
- [ ] Test `PageLoader` with mock PDF (10 pages)
- [ ] Test `GlossaryParser` with sample glossary text
- [ ] Test `IndexParser` with sample index text
- [ ] Test `PageIngestionLinker` with mock PageContent
- [ ] Test `IngestionDataCleanup` deletion logic
- [ ] Test repository queries

### ⏱️ 1.5 hours - Integration Tests
- [ ] Test Phase 1 (page loading) with 50-page sample
- [ ] Test Phase 2 (cleanup) with sample data
- [ ] Test Phase 3 (ingestion) with 50-page sample
- [ ] Test full workflow orchestration with sample PDF
- [ ] Test error recovery and rollback scenarios
- [ ] Verify pageNumber field population across all entities

### ⏱️ 1.5 hours - Manual Testing
- [ ] Run `--load-pages` on full PDF (1831 pages)
  - [ ] Verify PageContent table: 1831 rows
  - [ ] Verify category distribution: COVER(3), TOC(1), IMAGE_LIST(2), OAHSPE_BOOKS(1662), GLOSSARIES(22), INDEX(141)
  - [ ] Verify PageImage extraction
- [ ] Run `--verify-links` before ingestion
  - [ ] Confirm zero links (baseline)
- [ ] Run `--cleanup` with confirmation
  - [ ] Verify old data deleted
  - [ ] Verify PageContent preserved
- [ ] Run `--ingest-pages` on full dataset
  - [ ] Monitor progress every 50 pages
  - [ ] Verify Books created (~50)
  - [ ] Verify Chapters created (~400)
  - [ ] Verify Verses created (~40,000)
  - [ ] Verify GlossaryTerms created (~500-1000)
  - [ ] Verify IndexEntries created (~3000-5000)
- [ ] Run `--verify-links` after ingestion
  - [ ] Verify all entities have pageNumber
  - [ ] Check for orphaned content (pageNumber = null)
- [ ] Query verification:
  - [ ] SELECT COUNT(*) FROM page_contents
  - [ ] SELECT category, COUNT(*) FROM page_contents GROUP BY category
  - [ ] SELECT COUNT(*) FROM glossary_terms
  - [ ] SELECT COUNT(*) FROM index_entries
  - [ ] SELECT COUNT(*) FROM books WHERE page_number IS NULL
  - [ ] SELECT COUNT(*) FROM verses WHERE page_number IS NULL

---

## Task Group 7.8: Documentation (2 hours)

### ⏱️ 1 hour - API Documentation
- [ ] Create `PHASE7_USAGE_GUIDE.md`
  - [ ] CLI command reference
  - [ ] Workflow sequence explanation
  - [ ] Example commands
  - [ ] Troubleshooting guide
- [ ] Update JavaDoc comments for all new classes
- [ ] Add code examples for PageLoader, GlossaryParser, IndexParser

### ⏱️ 1 hour - Completion Report
- [ ] Create `PHASE7_COMPLETION_REPORT.md`
  - [ ] Executive summary
  - [ ] Implementation timeline
  - [ ] Database statistics (rows per table)
  - [ ] Performance metrics (loading time, ingestion time)
  - [ ] Known issues and limitations
  - [ ] Lessons learned
  - [ ] Phase 8 readiness checklist (translation workflow requirements)
- [ ] Update `README.md` with Phase 7 status
- [ ] Add schema diagrams for new tables

---

## Success Criteria

### Phase 1 Success
- [ ] All 1831 pages loaded to PageContent
- [ ] All images extracted to PageImage
- [ ] Zero extraction failures
- [ ] Categories correctly assigned

### Phase 2 Success
- [ ] All old data deleted (books/chapters/verses/notes/glossary/index)
- [ ] PageContent preserved (1831 rows intact)
- [ ] Images preserved

### Phase 3 Success
- [ ] All shouldIngest pages ingested (GLOSSARIES + OAHSPE_BOOKS + INDEX)
- [ ] Glossary terms extracted (~500-1000 terms)
- [ ] Index entries extracted (~3000-5000 entries)
- [ ] All entities have pageNumber field populated
- [ ] All PageImages linked to Image entities
- [ ] Zero ingestion errors

### Overall Success
- [ ] Full workflow executes end-to-end without manual intervention
- [ ] All verification gates pass
- [ ] Database schema matches design
- [ ] Performance meets targets (< 30 min total)
- [ ] Code coverage > 70%
- [ ] Documentation complete
- [ ] Ready for Phase 8 (translation workflow)

---

## Timeline

| Day | Task Groups | Hours |
|-----|-------------|-------|
| **Day 1** | 7.1 (Entities) + 7.2 (Enhancements) + Start 7.3 (Repos) | 8 |
| **Day 2** | Finish 7.3 (Repos) + 7.4 (Services) | 8 |
| **Day 3** | 7.5 (Updates) + 7.6 (CLI) + Start 7.7 (Testing) | 8 |
| **Day 4** | Finish 7.7 (Testing) + 7.8 (Documentation) | 3.5 |

**Total:** 27.5 hours (~3.5 working days)

---

## Risk Mitigation

- [ ] **Memory issues during full PDF load:** Use streaming, process in batches
- [ ] **Transaction timeouts:** Configure longer timeout for Phase 3
- [ ] **Glossary/Index parser accuracy:** Create test samples from manual inspection
- [ ] **Orphaned content:** Acceptable in Phase 7, handled by improved parsers in Phase 8
- [ ] **Duplicate detection:** Add unique constraints, handle gracefully

---

## Notes

- Keep translation model comparison framework for Phase 8
- Glossary ingestion is CRITICAL for Phase 8 translation consistency
- Index provides QA value for cross-reference validation
- All entities now track pageNumber for full traceability
