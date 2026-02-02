# Phase 8 Quick Reference - Developer Cheat Sheet

**Date:** February 1, 2026  
**Updated:** February 2, 2026 - Geometry-Aware PDF Processing  
**For:** Developers implementing Phase 8

---

## Problem Solved

**Issue:** Current page-sequential rawText parsing fails with two-column PDF layout
```
What fails:
  - Verses broken across left/right columns
  - Footnotes interleaved at column bottom
  - Simple concatenation destroys reading order

Solution:
  - Extract geometry-aware TextFragments (x, y, width, height, font properties)
  - Detect columns by spatial clustering
  - Reconstruct reading order: left-column top→bottom, right-column top→bottom
  - Result: Perfect verse/footnote extraction despite complex layout
```

---

## The 8 Phases at a Glance

| Phase | What | Command | Verification |
|-------|------|---------|--------------|
| 0 | Check prerequisites | `--status` | 1831 pages in DB |
| **0.5** | **Extract geometry** | **`--extract-geometry`** | **>50K TextFragments** |
| 1 | Extract TOC | `--toc-extract` | N books printed (note Book 35 SKIP) |
| 2 | Register books | `--toc-register` | N-1 books in DB (Book 35 skipped) |
| 3 | Assign pages | `--assign-pages` | ~1662 pages linked (Book 35 excluded) |
| 4A | Detect chapters | `--parse-book 1` | Chapters found |
| 4B | Extract verses | (automatic) | Verses found |
| 4C | Extract footnotes | (automatic) | Footnotes found |
| 5 | Aggregate stats | `--aggregate-all` | Counts + text |
| 6 | Final verify | `--verify` | All checks pass |

---

## Testing Workflow (Copy-Paste Ready)

```bash
# Check status
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --status

# Phase 0.5: Extract geometry (REQUIRED - use TextFragments, not rawText)
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --extract-geometry
# VERIFY: SELECT COUNT(*) FROM text_fragments; -- Expected: >50,000

# Phase 1: Extract TOC (read-only, safe to run)
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --toc-extract
# VERIFY: Review output, count books (N = actual book count)

# Phase 2: Register books
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --toc-register
# VERIFY: SELECT COUNT(*) FROM books; -- Expected: N-1 (Book 35 skipped)

# Phase 3: Assign pages
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --assign-pages
# VERIFY: SELECT COUNT(*) FROM page_contents WHERE book_id IS NOT NULL; -- ~1662

# Phase 4: Parse single book (test first! - uses TextFragments)
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --parse-book 1
# Processes TextFragments → Detects chapters → Extracts verses → Extracts footnotes
# VERIFY: SELECT COUNT(*) FROM chapters WHERE book_id = 1;

# If Book 1 looks good, parse all books
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --parse-all-books
# Parses all 38 books using geometry-aware TextFragments

# Phase 5: Aggregate
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --aggregate-all

# Phase 6: Final verification
java -jar target/oahspe-0.0.1-SNAPSHOT.jar --verify
# RESULT: Detailed report, should show all ✓
```

---

## Key Services to Implement

### Phase 0.5 (NEW - Required)
- **PdfGeometryExtractor** - Extract TextFragments with coordinates
- **ColumnDetectionService** - Identify left/right column boundaries
- **ReadingOrderCalculator** - Assign sequential reading order

### Essential (Must Have)
1. **TableOfContentsParser** - Extract books from page 4 (determines N)
2. **BookRegistrationService** - Create N Book entities
3. **PageAssignmentService** - Link pages to books
4. **ContentParsingService** - Parse chapters/verses/footnotes using TextFragments
5. **AggregationService** - Calculate counts, combine text
6. **FinalVerificationService** - Check all data integrity

### Phase 4 (Updated)
- **ChapterDetectionService** - Uses TextFragment font properties (size, bold)
- **VerseExtractionService** - Extracts verses using reading order from TextFragments
- **FootnoteExtractionService** - Identifies footer-area TextFragments for footnotes

---

## Database Fields to Add

```sql
-- TextFragment entity (NEW - REQUIRED)
CREATE TABLE text_fragments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pdf_page_id BIGINT NOT NULL,
    text LONGTEXT NOT NULL,
    x_coordinate FLOAT NOT NULL,
    y_coordinate FLOAT NOT NULL,
    width FLOAT NOT NULL,
    height FLOAT NOT NULL,
    font_name VARCHAR(255),
    font_size FLOAT,
    is_bold BOOLEAN DEFAULT FALSE,
    is_italic BOOLEAN DEFAULT FALSE,
    column_number INT,
    reading_order INT,
    FOREIGN KEY (pdf_page_id) REFERENCES pdf_pages(id),
    INDEX idx_page_column (pdf_page_id, column_number),
    INDEX idx_reading_order (pdf_page_id, reading_order)
);

-- Book entity
ALTER TABLE books ADD COLUMN start_page INT;
ALTER TABLE books ADD COLUMN end_page INT;
ALTER TABLE books ADD COLUMN book_number INT UNIQUE;

-- Chapter entity
ALTER TABLE chapters ADD COLUMN verse_count INT DEFAULT 0;
ALTER TABLE chapters ADD COLUMN verse_content LONGTEXT;

-- Verse entity
ALTER TABLE verses ADD COLUMN note_count INT DEFAULT 0;
ALTER TABLE verses ADD COLUMN note_content LONGTEXT;

-- PageContent entity
ALTER TABLE page_contents ADD COLUMN book_id INT;
ALTER TABLE page_contents ADD CONSTRAINT fk_page_book 
  FOREIGN KEY (book_id) REFERENCES books(id);
```

---

## CLI Commands to Implement

```bash
--extract-geometry        Phase 0.5: Extract TextFragments (NEW - REQUIRED)
--toc-extract              Phase 1: Extract books from TOC (no DB changes)
--toc-register             Phase 2: Create Book entities
--assign-pages             Phase 3: Link pages to books
--parse-book <N>           Phase 4: Parse single book using TextFragments
--parse-all-books          Phase 4: Parse all N books using TextFragments
--aggregate-book <N>       Phase 5: Aggregate single book
--aggregate-all            Phase 5: Aggregate all N books
--verify                   Phase 6: Run verification suite
--status                   Show workflow progress
--help                     Show all commands
```

---

## Regex Patterns

### TOC Extraction (Page 4 Format)
```
Pattern: ^(\d+)/\s+(.+?)\s{2,}(?:pg\s+)?(\d+)\s*$
Example: 01/ Tae's Prayer...............pg 7
         02/ Oahspe Prologue.........  11
         39/ Bk of Jehovah's Kingdom on Earth 1614
```

### Chapter Detection
```
Pattern: ^Chapter\s+(\d+)\s*[:.]?\s*(.*)$
Example: Chapter 1: The Beginning
         Chapter 7. Creation
```

### Verse Extraction
```
Pattern: ^(\d+/[A-Za-z0-9]+\.\d+)\s+(.*)$
Example: 1/1.1 And the spirit said
         35/D.15 In the eternal realms
```

### Note Extraction
```
Pattern: ^\(([^)]+)\)\s+(.*)$
Example: (1) This refers to the spirit
         (a) Commentary on verse 1
```

---

## Verification Quick Check

After each phase, run these SQL checks:

```sql
-- Phase 1 result
SELECT COUNT(DISTINCT title) FROM (SELECT title FROM ... WHERE title IS NOT NULL);

-- Phase 2 result
SELECT COUNT(*) FROM books;
-- Expected: ~38 (39 extracted, Book 35 Saphah skipped)

-- Phase 3 result
SELECT COUNT(*) FROM page_contents WHERE book_id IS NOT NULL;
-- Expected: ~1662 (Book 35 pages excluded)

-- Phase 4 result
SELECT COUNT(DISTINCT book_id) FROM chapters;

-- Phase 5 result
SELECT COUNT(*) FROM chapters WHERE verse_count IS NOT NULL AND verse_count > 0;

-- Phase 6 result
-- Run: java -jar oahspe.jar --verify
```

---

## Error Recovery (Quick Fixes)

| Error | Fix |
|-------|-----|
| Less than 39 books extracted | Adjust TOC regex pattern, re-run Phase 1 |
| Book 35 was processed | DELETE FROM books WHERE book_number = 35; and restart |
| Pages assigned to wrong book | UPDATE page_contents SET book_id = NULL; Re-run Phase 3 |
| Missing verses in Book 5 | Adjust verse regex, clear Book 5, re-run Phase 4 |
| Count mismatch in aggregation | Clear aggregation fields, re-run Phase 5 |

---

## Expected Data Ranges (Success Criteria)

```
Books:    38(exact)
Chapters:  500-1000
Verses:    10,000-20,000
Notes:     100-1,000
Pages:     1,662 (exact)
```

---

## Key Design Decisions

✅ **TOC-Driven** - Book boundaries from Table of Contents (not guessed)  
✅ **Book-Centric** - Each book processed complete before next  
✅ **Sequential First** - Prove correctness before parallelizing  
✅ **Two-Pass Parsing** - Chapters first, then verses, then notes  
✅ **Aggregation Separate** - Independent phase, can re-run safely  
✅ **Manual Checkpoints** - User verifies before proceeding  
✅ **Verification Gates** - Fail fast on first problem  
✅ **Single-Book Testing** - Test 1 book before processing 38  

---

## Development Checklist

Phase 0:
- [ ] Prerequisites verified

Phase 1:
- [ ] TableOfContentsParser created
- [ ] Regex pattern working
- [ ] --toc-extract command works
- [ ] Output shows N books (manually verify count)

Phase 2:
- [ ] BookRegistrationService created
- [ ] Book entity has new fields
- [ ] --toc-register command works
- [ ] N books in database (matches Phase 1)

Phase 3:
- [ ] PageAssignmentService created
- [ ] PageContent has book_id field
- [ ] --assign-pages command works
- [ ] ~1662 pages linked to books (N-dependent)

Phase 4:
- [ ] ChapterDetectionService created
- [ ] VerseExtractionService created
- [ ] NoteExtractionService created
- [ ] ContentParsingService orchestrator created
- [ ] --parse-book 1 works correctly
- [ ] Book 1 has chapters and verses
- [ ] --parse-all-books works
- [ ] All N books parsed

Phase 5:
- [ ] Chapter entity has new fields
- [ ] Verse entity has new fields
- [ ] ChapterAggregationService created
- [ ] VerseAggregationService created
- [ ] --aggregate-all command works
- [ ] Counts and content populated

Phase 6:
- [ ] FinalVerificationService created
- [ ] 20+ verification checks implemented
- [ ] --verify command works
- [ ] All checks pass

Testing:
- [ ] Single-book workflow works
- [ ] Full 38-book workflow works
- [ ] All SQL verification queries work
- [ ] Error recovery procedures tested
- [ ] CLI commands documented

---

## File Locations to Create/Modify

```
src/main/java/edu/minghualiu/oahspe/
  ├─ ingestion/
  │  ├─ toc/                          (NEW FOLDER)
  │  │  ├─ TableOfContentsParser.java
  │  │  ├─ BookRegistrationService.java
  │  │  ├─ PageAssignmentService.java
  │  │  ├─ ContentParsingService.java
  │  │  ├─ ChapterDetectionService.java
  │  │  ├─ VerseExtractionService.java
  │  │  ├─ NoteExtractionService.java
  │  │  ├─ AggregationOrchestrator.java
  │  │  └─ FinalVerificationService.java
  │  ├─ aggregation/                  (NEW FOLDER - for aggregation)
  │  │  ├─ VerseAggregationService.java
  │  │  ├─ ChapterAggregationService.java
  │  │  └─ BookAggregationService.java
  │  └─ (existing services)
  ├─ entities/
  │  ├─ Book.java                     (MODIFY - add start_page, end_page, book_number)
  │  ├─ Chapter.java                  (MODIFY - add verse_count, verse_content)
  │  ├─ Verse.java                    (MODIFY - add note_count, note_content)
  │  └─ PageContent.java              (MODIFY - add book_id)
  ├─ repositories/
  │  └─ (no new repos needed)
  └─ cli/
     └─ IngestionCliRunner.java       (MODIFY - add new commands)
```

---

## Performance Targets

**Sequential Mode (Recommended First):**
- Phase 1: < 1 second
- Phase 2: < 1 second
- Phase 3: ~5 seconds
- Phase 4: ~30 minutes
- Phase 5: ~10 minutes
- Phase 6: ~2 minutes
- **Total: ~42 minutes**

**Parallel Mode (After Sequential Verified):**
- Phase 4: ~5 minutes (with 8 cores)
- Phase 5: ~2 minutes
- **Total: ~7-8 minutes**

---

## Full Design Document

For complete details, specifications, and SQL queries:
→ **[PHASE8_TOC_BASED_INGESTION_DESIGN.md](PHASE8_TOC_BASED_INGESTION_DESIGN.md)**

1,950+ lines covering every aspect of Phase 8

---

**Version:** 1.0  
**Created:** February 1, 2026  
**Status:** Ready for Implementation
