# Phase 8: TOC-Based Ingestion Architecture & Design Guide

**Date:** February 1, 2026  
**Status:** Design Complete - Ready for Implementation  
**Audience:** Developers implementing Phase 8  
**Version:** 1.0

---

## Executive Summary

**Problem:** Current page-sequential ingestion loses book context, resulting in missing books, truncated verses, and lost notes.

**Solution:** Parse Table of Contents (TOC) to establish book boundaries, then process each complete book as an independent unit with verification gates at each step.

**Key Benefits:**
- ✅ Structural awareness (books are units, not pages)
- ✅ Complete context before parsing (no cross-page state issues)
- ✅ Parallel-safe (each book independent)
- ✅ Verification gates prevent garbage data
- ✅ Easy to test (single-book processing)
- ✅ Manual checkpoints for validation
- ✅ Clear error recovery procedures

**Expected Outcome:**
- 38 books (39 extracted, Book 35 skipped for separate handling)
- 500-1000+ chapters with verse/note aggregates (varies with N)
- 10,000-20,000+ verses with note aggregates (varies with N)
- All aggregation fields (counts, combined text) populated
- 100% data integrity verification passing

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Phase Breakdown (0-6)](#phase-breakdown)
3. [CLI Commands](#cli-commands)
4. [Verification Procedures](#verification-procedures)
5. [SQL Verification Queries](#sql-verification-queries)
6. [Error Recovery Guide](#error-recovery-guide)
7. [Service Layer Design](#service-layer-design)
8. [Data Model Changes](#data-model-changes)
9. [Implementation Checklist](#implementation-checklist)
10. [Parallel Processing (Phase 2)](#parallel-processing-phase-2)

---

## Architecture Overview

### Core Principle: Structure-Aware, Book-Centric Processing

```
Traditional Approach (Page-Sequential):
Page 1 → State Machine → Parser → Event Stream
Page 2 → State Machine → Parser → Event Stream  [Lost context from Page 1]
Page 3 → State Machine → Parser → Event Stream  [Lost context from Pages 1-2]
RESULT: Missing books, truncated verses

New Approach (TOC-Driven):
Table of Contents (Page 4)
    ↓
Extract Book Boundaries
    ↓
For Each Book (Complete Unit):
  ├─ Collect All Pages (startPage → endPage)
  ├─ Parse Entire Book Content
  ├─ Create Book → Chapters → Verses → Notes
  ├─ Aggregate Statistics
  └─ Verify Correctness
RESULT: All books complete with verified data
```

### Three-Layer Architecture

**Layer 1: Structural Discovery**
- Extract book metadata from Table of Contents
- Identify book boundaries and page ranges
- Single source of truth for book structure

**Layer 2: Content Organization**
- Group pages by book
- Validate page continuity
- Assign pages to books for tracking

**Layer 3: Intelligent Processing**
- Parse chapters, verses, notes with complete context
- Calculate aggregates (counts, combined text)
- Verify at each step

---

## Phase Breakdown

### Phase 0: Prerequisites Verification

**What It Does:**
Validates that Phase 1 (page loading) completed successfully and database has required data.

**Inputs:**
- Page loading already complete (1831 pages in database)

**Verification Checks:**

```
□ PageContent count = 1831
□ All pages have non-null rawText
□ Page 4 (TOC) has sufficient length (> 100 chars)
□ Pages 7-1668 have category = 'OAHSPE_BOOKS' (1662 pages)
```

**Manual Verification (SQL):**
```sql
-- Check 1: Total pages
SELECT COUNT(*) as total_pages FROM page_contents;
-- Expected: 1831

-- Check 2: Pages with content
SELECT COUNT(*) as pages_with_content FROM page_contents WHERE raw_text IS NOT NULL;
-- Expected: 1831

-- Check 3: TOC page
SELECT page_number, LENGTH(raw_text) as content_length 
FROM page_contents WHERE page_number = 4;
-- Expected: page_number=4, content_length > 100

-- Check 4: OAHSPE_BOOKS range
SELECT COUNT(*) as book_section_pages FROM page_contents 
WHERE category = 'OAHSPE_BOOKS' AND page_number BETWEEN 7 AND 1668;
-- Expected: 1662
```

**Success Criteria:**
- All 4 checks pass
- If any check fails: Investigate Phase 1, do not proceed

**Decision Gate:**
```
PASS: Proceed to Phase 1
FAIL: Stop, investigate phase 1 page loading
```

---

### Phase 1: TOC Extraction

**What It Does:**
Parses Table of Contents (page 4) to extract book metadata (title, book number, page range).

**Inputs:**
- Page 4 raw text from database

**Process:**
1. Load page 4 raw text
2. Parse text using regex to find book entries
3. Expected format: `Book 1. Book of Jehovih ... 7` (dots/spaces to page number)
4. Extract: book number, title, start page
5. Calculate end page: next book's start page (All books overlap at boundaries)
   - Book 01: pages 7-11, overlaps with Book 02 at page 11
   - Books 02-39: each book's start page = previous book's end page (boundary overlap)
6. Output: List<BookMetadata> (does NOT save to database)

**Sample Output:**
```
================================================================================
TOC EXTRACTION RESULTS
================================================================================
Extracted 39 books from Table of Contents (38 to process, 1 to skip):

#   Title                                    Start   End     Status
--  ----------------------------------------  -----   -----   --------
01  Tae's Prayer                             7       11      PROCESS
02  Oahspe Prologue                          11      13      PROCESS
03  Voice of Man                             14      27      PROCESS
04  Book of Jehovih                          27      88      PROCESS
05  Book of Sethantes                        88      115     PROCESS
06  First Book of the First Lords            115     135     PROCESS
07  Book of Ah'shong                         135     166     PROCESS
08  Second Book of Lords                     166     195     PROCESS
09  Synopsis of Sixteen Cycles               195     225     PROCESS
10  Book of Aph                              225     250     PROCESS
11  The Lords' First Book                    250     271     PROCESS
12  Book of Sue                              271     297     PROCESS
13  The Lords' Second Book                   297     318     PROCESS
14  Book of Apollo                           318     346     PROCESS
15  The Lords' Third Book                    346     367     PROCESS
16  Book of Thor                             367     395     PROCESS
17  The Lords' Fourth Book                   395     416     PROCESS
18  Book of Osiris                           416     444     PROCESS
19  The Lords' Fifth Book                    444     465     PROCESS
20  Book of Fragapatti                       465     493     PROCESS
21  Book of God's Word                       493     542     PROCESS
22  Book of Divinity                         542     594     PROCESS
23  Book of Cpenta-armij                     594     640     PROCESS
24  First Book of God                        640     667     PROCESS
25  Book of Wars Against Jehovih             667     737     PROCESS
26  Book of Lika                             737     773     PROCESS
27  Book of the Arc of Bon                   773     814     PROCESS
28  God's Book of Eskra                      814     850     PROCESS
29  Book of Es                               850     890     PROCESS
30  Bon's Book of Praise                     890     920     PROCESS
31  Book of Ouranothen                       920     962     PROCESS
32  Book of Judgment                         962     1001    PROCESS
33  Book of Discipline                       1001    1047    PROCESS
34  Book of Inspiration                      1047    1089    PROCESS
35  Book of Saphah                           [TBD]   [TBD]   SKIP (different structure)
36  God's Book of Ben                        [pages] [pages] PROCESS
37  Book of Knowledge                        [pages] [pages] PROCESS
38  Book of Cosmogony and Prophecy           [pages] [pages] PROCESS
39  Bk of Jehovih's Kingdom on Earth         1600+   1614    PROCESS
                                                              ------
                                             Total: 38 books (SKIP Saphah)
================================================================================
```

**Verification Checks:**

```
□ All books extracted from Table of Contents (39 entries: 38 process, 1 skip)
□ First entry starts at page 7 (Tae's Prayer)
□ Last entry ends at page 1614 or nearby
□ Verify entry count matches expected (39 total: 38 to process + 1 skip)
□ Verify boundary overlaps (ALL books have overlaps at boundaries):
  - Book 01: pages 7-11, overlaps with Book 02 at page 11
  - Books 02-39: each starts where previous ends (page shared between both)
  - When assigning pages: boundary pages go to book STARTING at that page
□ Verify title extraction handles dots correctly (not truncated)
□ Note: Book 35 (Saphah) correctly identified for skipping
□ All titles are non-empty strings
□ All page numbers are positive integers
```

**Manual Verification:**
1. Review printed list
2. Manually check 3 book titles against PDF TOC
3. Verify first/last book page numbers
4. Confirm total page count = 1662

**Decision Gate:**
```
PASS: All 8 checks pass → Proceed to Phase 2
FAIL: Pattern mismatch → Adjust TOC parser regex, re-run Phase 1
FAIL: Missing books → Check TOC page 4 content (might be corrupted)
```

**Implementation Notes:**
- Service: `TableOfContentsParser.extractBooks()`
- Regex pattern: `Pattern.compile("^(\\d+)/\\s+(.+?)\\s{2,}(?:pg\\s+)?(\\d+)\\s*$", Pattern.MULTILINE)`
  - Group 1: Book number (01, 02, ..., 39)
  - Group 2: Book title
  - Group 3: Page number (start page)
- Format example: `01/ Tae's Prayer...............pg 7` or `02/ Oahspe Prologue.........  11`
- Return type: `List<BookMetadata>`
- Do NOT save to database in this phase
- **IMPORTANT:** Book 35 (Book of Saphah) has different structure - will be extracted but marked as "SKIP"
- **BOUNDARY OVERLAPS (EXPECTED):**
  - ALL books (01-39): Have boundary overlaps at starting/ending pages
  - Book 01: pages 7-11 (overlaps at page 11 with Book 02)
  - Books 02-39: Start page of book N = End page of book N-1 (boundary shared)
  - Example: Book 01 ends at 11, Book 02 starts at 11 (11 is shared between both)
  - When assigning pages to books: 
    - Overlapping pages → Assign to the book STARTING at that page
    - Example: Page 11 is end of Book 01 AND start of Book 02 → Page 11 assigned to Book 02
    - Use book_number to determine ownership at boundaries
  - Log overlaps for verification but do NOT reject them

---

### Phase 2: Book Registration

**What It Does:**
Creates Book entities in database with metadata from TOC extraction.

**Inputs:**
- List<BookMetadata> from Phase 1

**Process:**
1. For each BookMetadata:
   - Create Book entity
   - Set: title, bookNumber, startPage, endPage
   - Set pageNumber = startPage
   - Save to database
2. Do NOT set chapters or verses yet

**Output:**
```
Book Registration Results:
├─ 38 books created (Book 35 skipped)
├─ IDs assigned: 1-38 (skipping 35)
└─ Ready for page assignment
```

**Verification Checks:**

```
□ All books created (38 in database, Book 35 marked skip)
□ All books have non-null title
□ All books have startPage and endPage set
□ startPage and endPage are positive integers
□ Page ranges are contiguous (no gaps): SUM(endPage - startPage + 1) = 1662
□ No overlapping page ranges
□ First book startPage = 7
□ Last book endPage = 1668
```

**Manual Verification (SQL):**
```sql
-- Check 1: Book count
SELECT COUNT(*) as total_books FROM books;
-- Expected: N (from Phase 1)

-- Check 2: Sample book data
SELECT id, title, book_number, start_page, end_page 
FROM books ORDER BY book_number LIMIT 5;
-- Expected: 5 rows with valid sequential data

-- Check 3: Page range coverage
SELECT SUM(end_page - start_page + 1) AS total_pages FROM books;
-- Expected: 1662

-- Check 4: No overlapping ranges
SELECT COUNT(*) as overlaps FROM books b1
JOIN books b2 ON b1.book_number < b2.book_number
WHERE b1.end_page >= b2.start_page;
-- Expected: 0

-- Check 5: Contiguous pages
SELECT COUNT(*) as gaps FROM (
  SELECT b1.title AS book_from, b1.end_page AS ends, 
         b2.title AS book_to, b2.start_page AS starts
  FROM books b1
  JOIN books b2 ON b2.book_number = b1.book_number + 1
  WHERE b1.end_page + 1 != b2.start_page
) as issues;
-- Expected: 0

-- Check 6: First/last books
SELECT book_number, title, start_page, end_page FROM books 
WHERE book_number IN (1, N);
-- Expected: Book 1: start=7, Book N: end=1668
```

**Decision Gate:**
```
PASS: All checks pass → Proceed to Phase 3
FAIL: Duplicate books → Run: DELETE FROM books; Re-run Phase 2
FAIL: Wrong page ranges → Fix TOC parser, re-run Phases 1-2
FAIL: Missing books → Check Phase 1 output
```

**Implementation Notes:**
- Service: `BookRegistrationService.registerBooks(List<BookMetadata>)`
- Transaction: Single transaction for all 38 books (Book 35 excluded)
- Idempotent: Can re-run (delete and recreate)

---

### Phase 3: Page Assignment

**What It Does:**
Links each PageContent to its owning Book by comparing page numbers with book ranges.

**Inputs:**
- Books table (with startPage, endPage)
- PageContent table (pages 7-1668)

**Process:**
1. For each page in PageContent where page_number BETWEEN 7 AND 1668:
   - Find book where: bookMetadata.startPage <= page_number <= bookMetadata.endPage
   - Update PageContent.bookId = book.id
2. Log assignment counts

**Output:**
```
Page Assignment Results:
├─ 1662 pages assigned to books
├─ Book 1: 142 pages
├─ Book 2: 132 pages
├─ ...
└─ Book 66: 69 pages
```

**Verification Checks:**

```
□ All 1662 pages in range [7-1668] are assigned
□ No pages are unassigned (bookId IS NOT NULL)
□ Each book has exactly (endPage - startPage + 1) pages
□ Page numbers within each book are contiguous
□ Pages are assigned only to correct book (by range)
```

**Manual Verification (SQL):**
```sql
-- Check 1: All pages assigned
SELECT COUNT(*) as assigned_pages FROM page_contents 
WHERE category = 'OAHSPE_BOOKS' AND book_id IS NOT NULL;
-- Expected: 1662

-- Check 2: No unassigned pages
SELECT COUNT(*) as unassigned_pages FROM page_contents 
WHERE category = 'OAHSPE_BOOKS' AND book_id IS NULL;
-- Expected: 0

-- Check 3: Page count per book
SELECT b.id, b.title, COUNT(pc.id) AS actual_pages,
       (b.end_page - b.start_page + 1) AS expected_pages
FROM books b
LEFT JOIN page_contents pc ON pc.book_id = b.id
GROUP BY b.id
ORDER BY b.book_number;
-- Expected: actual_pages = expected_pages for all books

-- Check 4: Books with page count mismatch
SELECT b.id, b.title, COUNT(pc.id) AS actual, 
       (b.end_page - b.start_page + 1) AS expected
FROM books b
LEFT JOIN page_contents pc ON pc.book_id = b.id
GROUP BY b.id
HAVING COUNT(pc.id) != (b.end_page - b.start_page + 1);
-- Expected: 0 rows

-- Check 5: Sample book page numbers
SELECT b.title, 
       MIN(pc.page_number) AS first_page,
       MAX(pc.page_number) AS last_page,
       b.start_page, b.end_page
FROM books b
JOIN page_contents pc ON pc.book_id = b.id
WHERE b.book_number IN (1, 2, 66)
GROUP BY b.id;
-- Expected: first_page = start_page, last_page = end_page
```

**Decision Gate:**
```
PASS: All checks pass → Proceed to Phase 4
FAIL: Pages assigned to wrong book → Check page ranges, re-run Phase 3
FAIL: Missing pages for book → Check Phase 0 (page loading issue)
FAIL: Duplicate assignments → Clear and re-run: UPDATE page_contents SET book_id = NULL; Phase 3
```

**Implementation Notes:**
- Service: `PageAssignmentService.assignPagesToBooks()`
- SQL approach: Update PageContent.bookId using range join
- **IMPORTANT: Handle boundary overlaps**
  - For pages with overlapping ranges: assign to the book whose START page matches
  - Example: Page 11 overlaps Books 02 and 03
    - If Book 02 ends at 11 and Book 03 starts at 11
    - Assign page 11 to Book 03 (the one starting at that page)
  - SQL query: Use book_number to determine owner when page is at boundary
- Idempotent: Clear assignment and re-run

---

## PDF Foundation Layer (Required for Phase 4+)

**Prerequisite:** Before parsing content in Phase 4, the system must extract geometry-aware text fragments from the PDF instead of relying on simple concatenated text.

### Why This Foundation Layer Is Essential

Current approach (fails with two-column layout):
```
rawText = concatenated text from entire page
Problem: Verses broken across columns, footnotes interleaved
Result: Parsing fails, verses truncated, notes misaligned
```

New approach (handles complex layouts):
```
TextFragments = List<x, y, width, height, text, font properties>
Columns detected by x-coordinate clustering
Reading order = top-to-bottom within column, left-column first, then right-column
Result: Accurate verse/note extraction despite layout complexity
```

### PDF Foundation Entities

These entities provide the structural basis for intelligent parsing:

#### PdfPage
```java
class PdfPage {
    Long id;
    int pageNumber;                    // 1-1831, matches physical PDF
    String extractedText;              // Full concatenated text (fallback)
    List<TextFragment> fragments;      // Geometry-aware text pieces
    List<PdfImage> images;             // Raw images on page
    boolean processed;
    LocalDateTime loadedAt;
}
```

#### TextFragment
```java
class TextFragment {
    Long id;
    PdfPage page;
    
    String text;                       // Actual text content
    float x, y;                        // Position on page
    float width, height;               // Dimensions
    String fontName;
    float fontSize;                    // Critical for structure detection
    boolean bold;
    boolean italic;
    int columnNumber;                  // 0=left, 1=right (auto-detected)
    float readingOrder;                // Y-position within column for sorting
}
```

#### PdfImage
```java
class PdfImage {
    Long id;
    PdfPage page;
    
    byte[] data;
    String mimeType;
    float x, y;                        // Position on page
    float width, height;               // Dimensions
    String imageId;                    // Identifier if present in PDF
}
```

### Parsing Strategy Using TextFragments

#### Step 0: Load TextFragments for Book

```java
List<TextFragment> bookFragments = textFragmentRepository
    .findByPageInOrderByPageNumberAscXAscYAsc(book.getPages());
```

#### Step 1: Group by Page and Column

```
For each page in book:
  ├─ Cluster fragments by x-coordinate
  │  ├─ Cluster 1 (x < center): Left column
  │  └─ Cluster 2 (x > center): Right column
  └─ Sort fragments within each column by y-coordinate (top to bottom)
```

#### Step 2: Reconstruct Reading Order

```
Logical text = ""
For left column fragments (sorted by y):
  Logical text += fragment.text + " "
For right column fragments (sorted by y):
  Logical text += fragment.text + " "
```

#### Step 3: Detect Structure Using Font Properties

```
For each fragment in reading order:
  If fontSize > threshold AND bold:
    → Potential chapter/section header
  Else if fontSize == normal AND not bold:
    → Body text (verses or prose)
  Else if font == footnote font OR y > page_height - footer_margin:
    → Footnote text
```

#### Step 4: Identify Column Boundaries (Footer vs Body)

```
footer_margin = last_text_y - first_text_y) * 0.15  // Last 15% of page
For each fragment:
  If y > (page_height - footer_margin):
    → Likely footnote or footer
  Else:
    → Body text
```

### Expected Data Structure After Foundation Layer

After TextFragments are extracted:
- **1831 PdfPage entities** (one per page)
- **~100,000+ TextFragment entities** (varies by PDF compression)
- **Fragments clustered** by page and column
- **Font properties available** for structure detection
- **Page geometry preserved** for accurate layout reconstruction

### Migration from rawText to TextFragments

#### Phase 4 Requires Choice

**Option A: Full Geometry Parsing (Recommended for accuracy)**
- Use TextFragment x,y coordinates
- Detect columns by spatial clustering
- Sort by reading order (left-to-right, top-to-bottom)
- Extract verses/footnotes with perfect alignment

**Option B: Hybrid Approach (Faster implementation)**
- Use TextFragments where available
- Fallback to rawText for simple parsing
- Use font size to detect chapter headers
- Trade some accuracy for faster implementation

**Option C: Legacy rawText (Current, Failing)**
- Continue using rawText only
- **Will fail** for two-column verses and complex layouts
- **Not recommended** - blocks Phase 4 completion

### Database Schema Additions

```sql
-- TextFragment table
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

-- PdfImage table
CREATE TABLE pdf_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pdf_page_id BIGINT NOT NULL,
    image_data LONGBLOB NOT NULL,
    mime_type VARCHAR(50),
    x_coordinate FLOAT NOT NULL,
    y_coordinate FLOAT NOT NULL,
    width FLOAT NOT NULL,
    height FLOAT NOT NULL,
    image_id VARCHAR(255),
    FOREIGN KEY (pdf_page_id) REFERENCES pdf_pages(id)
);
```

### Pre-Phase 4 Verification

Before starting Phase 4, verify:

```
□ TextFragment count > 50,000 (indicates good text extraction)
□ Sample fragments have correct x,y coordinates
□ Font properties populated (fontSize, bold, italic)
□ No duplicate fragments for same text on same page
□ TextFragments cover pages 7-1668 (book content range)
□ Column detection working (fragments in left/right clusters)
□ Reading order preserved (y-coordinates monotonically increasing within column)
```

---

### Phase 4: Content Parsing (Per Book)

**CRITICAL DESIGN CHANGE: Two-Column PDF Layout Handling**

The Oahspe PDF uses a two-column layout with footnotes at column bottom. Using concatenated `rawText` fails because:
- Verses are broken across columns (Column 1 bottom → Column 2 top)
- Verses continue across pages
- Footnotes are interleaved at column bottom
- Reading order is left-column-top to left-column-bottom, then right-column-top to right-column-bottom

**Solution: Geometry-Aware TextFragment Processing**

Instead of using concatenated `rawText`, parse using `TextFragment` entities that preserve:
1. **Position (x, y, width, height)** - Exact location on page
2. **Font properties** - Size, bold, italic for structure detection
3. **Reading order** - Process left column top→bottom, then right column top→bottom
4. **Footnote detection** - Footer text identified by y-coordinate

**Key Innovation: Two-Pass Parsing with Geometry Awareness**

#### Pass 4A: Structure Detection (Chapter Extraction)

**Process:**
1. For each book (sequential first):
   - Load all TextFragments for book pages (ordered by page_number, then by position)
   - Reconstruct reading order: left column (x < center) top→bottom, then right column (x > center) top→bottom
   - Detect chapter headers using: font size > body text AND bold AND page start
   - Extract: chapter title, page number where chapter starts
   - Create Chapter entity (title only, no verses yet)
   - Save to database

**Expected Pattern:**
- Chapter header: Large font (14+ pt), bold, at/near page start
- Verse marker: Superscript number or bracketed number like "¹²" or "[12]"
- Footnote: Located in footer area (y > page_height - footer_margin)
- Regex patterns adapted for geometry-aware matching

**Sample Output After Pass 4A:**
```
Book 1: Detected 7 chapters (geometry-aware)
├─ Chapter 1: Starting page 7 (font: bold, 16pt)
├─ Chapter 2: Starting page 45 (font: bold, 16pt)
├─ Chapter 3: Starting page 82 (font: bold, 16pt)
├─ Chapter 4: Starting page 120 (font: bold, 16pt)
├─ Chapter 5: Starting page 135 (font: bold, 16pt)
├─ Chapter 6: Starting page 142 (font: bold, 16pt)
└─ Chapter 7: Starting page 147 (font: bold, 16pt)
```

**Verification After Pass 4A (SQL):**
```sql
-- Check 1: Chapters created
SELECT COUNT(*) as total_chapters FROM chapters;
-- Expected: > 500 (typically 500-1000)

-- Check 2: Chapters per book
SELECT b.title, COUNT(c.id) AS chapter_count
FROM books b
LEFT JOIN chapters c ON c.book_id = b.id
GROUP BY b.id
ORDER BY b.book_number;
-- Expected: Each book has 1+ chapters

-- Check 3: All chapters have titles
SELECT COUNT(*) as chapters_without_title FROM chapters WHERE title IS NULL OR title = '';
-- Expected: 0

-- Check 4: Sample chapters from Book 1
SELECT b.title AS book, c.title AS chapter, c.page_number
FROM chapters c
JOIN books b ON c.book_id = b.id
WHERE b.book_number = 1
ORDER BY c.id;
-- Expected: 7 chapters with titles like "1", "2", etc.
```

#### Pass 4B: Verse Extraction

**Process:**
1. For each chapter:
   - Get text segment: from chapter start to next chapter start (or book end)
   - Parse verses using regex pattern
   - Extract: verse key, verse text
   - Create Verse entity
   - Save to database

**Expected Pattern:**
- Verse key format: `35/D.15` (book/chapter.verse)
- Regex: `^(\d+/[A-Za-z0-9]+\.\d+)\s+(.*)$`

**Sample Output After Pass 4B:**
```
Book 1, Chapter 1: Extracted 20 verses
├─ 1/1.1 And the spirit...
├─ 1/1.2 Behold the order...
├─ 1/1.3 In the beginning...
...
└─ 1/1.20 Thus spake Jehovih...
```

**Verification After Pass 4B (SQL):**
```sql
-- Check 1: Verses created
SELECT COUNT(*) as total_verses FROM verses;
-- Expected: 10,000-20,000

-- Check 2: Verses per chapter
SELECT c.title, COUNT(v.id) AS verse_count
FROM chapters c
LEFT JOIN verses v ON v.chapter_id = c.id
WHERE c.book_id = 1
GROUP BY c.id
ORDER BY c.id;
-- Expected: Each chapter has 1+ verses

-- Check 3: Verse key format validation
SELECT verse_key, SUBSTRING(text, 1, 50) as text_preview
FROM verses
WHERE chapter_id = 1
ORDER BY id
LIMIT 10;
-- Expected: Keys like "1/1.1", "1/1.2", format is correct

-- Check 4: No verses without text
SELECT COUNT(*) as verses_without_text FROM verses 
WHERE text IS NULL OR TRIM(text) = '';
-- Expected: 0

-- Check 5: No orphaned verses
SELECT COUNT(*) as orphaned_verses FROM verses WHERE chapter_id IS NULL;
-- Expected: 0
```

#### Pass 4C: Note Extraction

**Process:**
1. For each verse:
   - Check if verse text contains note markers
   - Extract notes (by note key pattern)
   - Create Note entity (noteKey, text, verseId)
   - Save to database

**Expected Pattern:**
- Note key format: `(1)`, `(2)`, `(a)`, etc.
- Regex: `^\(([^)]+)\)\s+(.*)$`

**Sample Output After Pass 4C:**
```
Verse 1/1.1: Attached 2 notes
├─ (1) The spirit speaks...
└─ (2) This refers to...
```

**Verification After Pass 4C (SQL):**
```sql
-- Check 1: Notes created
SELECT COUNT(*) as total_notes FROM notes;
-- Expected: 100-1,000 (varies by book)

-- Check 2: Notes per verse
SELECT v.verse_key, COUNT(n.id) AS note_count
FROM verses v
LEFT JOIN notes n ON n.verse_id = v.id
WHERE v.chapter_id = 1
GROUP BY v.id
HAVING COUNT(n.id) > 0
LIMIT 10;
-- Expected: Some verses have notes

-- Check 3: Note key format
SELECT n.note_key, SUBSTRING(n.text, 1, 50) as text_preview
FROM notes n
LIMIT 10;
-- Expected: Keys like "(1)", "(2)", "(a)", format is correct

-- Check 4: No orphaned notes in OAHSPE_BOOKS
SELECT COUNT(*) as orphaned_notes FROM notes 
WHERE verse_id IS NULL AND id NOT IN (
  SELECT n.id FROM notes n
  JOIN verses v ON n.verse_id = v.id
  WHERE v.id IS NOT NULL
);
-- Expected: 0 (or minimal for glossary notes)
```

**Phase 4 Summary Verification:**

```
□ Every book has at least 1 chapter
□ Every chapter has at least 1 verse
□ Every verse has text (not null or empty)
□ All chapter titles are populated
□ All verse keys follow pattern: digit/letter.digit
□ All verse texts non-empty
□ No orphaned entities (chapters→books, verses→chapters, notes→verses)
□ Verse keys are unique across all verses
```

**Decision Gate:**
```
PASS: All checks pass → Proceed to Phase 5
FAIL: Missing chapters for book X → Check chapter regex pattern
FAIL: Missing verses for chapter Y → Check verse regex pattern
FAIL: Wrong note attachment → Check note parsing logic, re-run Phase 4
FAIL: Duplicate verses → Check continuation line handling
```

**Implementation Notes:**

Services needed:
- `ChapterDetectionService.detectChapters(String bookText) → List<ChapterInfo>`
- `VerseExtractionService.extractVerses(String chapterText) → List<VerseInfo>`
- `NoteExtractionService.extractNotes(Verse verse) → List<NoteInfo>`

Main orchestrator:
- `ContentParsingService.parseBook(Book, List<PageContent>) → void`
  - Process passes 4A, 4B, 4C sequentially
  - Each step saves to database
  - Log progress after each pass

**Testing Approach:**
- Phase 4 can be run per-book: `--parse-book 1` then verify
- Only after single book succeeds: `--parse-all-books`
- This allows rapid iteration on regex patterns

---

### Phase 5: Aggregation (Per Book)

**What It Does:**
Calculates and populates aggregation fields: verse counts, note counts, combined text content.

**Why Separate Phase:**
- Aggregation is independent of parsing
- Can be re-run without re-parsing
- Fresh context from database (no lazy loading)
- Easy to verify (count = actual count)

**Process (Bottom-Up):**

#### Step 5A: Aggregate Verses

For each verse:
1. Count notes attached to verse: `noteCount = COUNT(notes WHERE verse_id = ?)`
2. Combine all note text with keys:
   ```
   noteContent = "(1) First note text\n(2) Second note text\n..."
   ```
3. Update verse: `noteCount`, `noteContent`

**Sample Data After 5A:**
```sql
verse_key | note_count | note_content
1/1.1     | 2          | "(1) The spirit speaks\n(2) This refers to..."
1/1.2     | 0          | NULL
1/1.3     | 1          | "(1) Commentary on third verse"
```

**Verification for Step 5A:**
```sql
-- Check 1: Verses with notes have content
SELECT COUNT(*) as verses_with_notes_but_no_content 
FROM verses WHERE note_count > 0 AND note_content IS NULL;
-- Expected: 0

-- Check 2: Note count accuracy
SELECT verse_key, note_count, 
       (SELECT COUNT(*) FROM notes WHERE verse_id = v.id) as actual_count
FROM verses v
WHERE note_count > 0
LIMIT 20;
-- Expected: note_count = actual_count for all rows

-- Check 3: Content contains all note keys
SELECT v.verse_key, COUNT(n.id) as expected,
       (SELECT COUNT(*) FROM (
         SELECT SUBSTRING_INDEX(SUBSTRING_INDEX(v.note_content, '(', 2), ')', 1) as key
       ) x WHERE x.key IS NOT NULL) as found_in_content
FROM verses v
LEFT JOIN notes n ON n.verse_id = v.id
WHERE v.note_count > 0
GROUP BY v.id;
-- Expected: expected = found_in_content
```

#### Step 5B: Aggregate Chapters

For each chapter:
1. Count verses in chapter: `verseCount = COUNT(verses WHERE chapter_id = ?)`
2. Combine all verse text with keys:
   ```
   verseContent = "1/1.1 First verse text\n1/1.2 Second verse text\n..."
   ```
3. Update chapter: `verseCount`, `verseContent`

**Sample Data After 5B:**
```sql
chapter_id | verse_count | verse_content
1          | 20          | "1/1.1 And the spirit...\n1/1.2 Behold the order...\n..."
2          | 18          | "1/2.1 In the second chapter...\n..."
```

**Verification for Step 5B:**
```sql
-- Check 1: Verse count accuracy
SELECT c.id, c.title, c.verse_count,
       (SELECT COUNT(*) FROM verses WHERE chapter_id = c.id) as actual
FROM chapters c
LIMIT 20;
-- Expected: verse_count = actual for all rows

-- Check 2: Content population
SELECT COUNT(*) as chapters_with_verses_but_no_content 
FROM chapters WHERE verse_count > 0 AND verse_content IS NULL;
-- Expected: 0

-- Check 3: Content includes all verse keys
SELECT c.id, c.title, c.verse_count
FROM chapters c
WHERE c.book_id = 1
LIMIT 10;
-- Then manually verify: c.verse_content contains all verse keys
```

#### Step 5C: Aggregate Books (Optional)

For completeness:
1. Count chapters in book
2. Optionally combine chapter summaries
3. Update book metadata

**Phase 5 Summary Verification:**

```
□ All verses with notes have noteCount > 0 and noteContent populated
□ All chapters with verses have verseCount > 0 and verseContent populated
□ verseCount = actual verse count for all chapters
□ noteCount = actual note count for all verses
□ Combined content includes all keys in correct format
□ No null content fields where count > 0
```

**Decision Gate:**
```
PASS: All checks pass → Proceed to Phase 6
FAIL: Count mismatch → Check aggregation logic
FAIL: Missing content → Check text concatenation
FAIL: Null content with count > 0 → Re-run Phase 5
```

**Implementation Notes:**

Services needed:
- `VerseAggregationService.aggregateVerse(Verse) → Verse`
- `ChapterAggregationService.aggregateChapter(Chapter) → Chapter`
- `BookAggregationService.aggregateBook(Book) → void`

Main orchestrator:
- `AggregationOrchestrator.aggregateAllBooks() → void`
  - For each book: Aggregate chapters, then verses
  - Transactional per book
  - Log progress

**Testing Approach:**
- Aggregate single book: `--aggregate-book 1`
- Verify output with SQL queries above
- Only after single book succeeds: `--aggregate-all-books`

---

### Phase 6: Final Verification

**What It Does:**
Comprehensive data integrity checks across all books, chapters, verses, and notes.

**Process:**

#### Verification Check Set 1: Referential Integrity

```sql
-- No orphaned chapters
SELECT COUNT(*) as orphaned FROM chapters WHERE book_id IS NULL;
-- Expected: 0

-- No orphaned verses
SELECT COUNT(*) as orphaned FROM verses WHERE chapter_id IS NULL;
-- Expected: 0

-- No orphaned notes in OAHSPE_BOOKS section
SELECT COUNT(*) as orphaned FROM notes n
WHERE verse_id IS NULL
AND NOT EXISTS (
  SELECT 1 FROM notes_images ni WHERE ni.note_id = n.id
);
-- Expected: 0 (or minimal for glossary section)
```

#### Verification Check Set 2: Hierarchical Completeness

```sql
-- Books without chapters
SELECT id, title FROM books b
WHERE NOT EXISTS (SELECT 1 FROM chapters WHERE book_id = b.id);
-- Expected: 0 rows

-- Chapters without verses
SELECT id, title FROM chapters c
WHERE NOT EXISTS (SELECT 1 FROM verses WHERE chapter_id = c.id);
-- Expected: 0 rows

-- Verses without text
SELECT id, verse_key FROM verses WHERE text IS NULL OR TRIM(text) = '';
-- Expected: 0 rows
```

#### Verification Check Set 3: Aggregation Consistency

```sql
-- Chapters with wrong verse_count
SELECT c.id, c.title, c.verse_count,
       (SELECT COUNT(*) FROM verses WHERE chapter_id = c.id) as actual
FROM chapters c
WHERE c.verse_count != (SELECT COUNT(*) FROM verses WHERE chapter_id = c.id);
-- Expected: 0 rows

-- Verses with wrong note_count
SELECT v.id, v.verse_key, v.note_count,
       (SELECT COUNT(*) FROM notes WHERE verse_id = v.id) as actual
FROM verses v
WHERE v.note_count != (SELECT COUNT(*) FROM notes WHERE verse_id = v.id);
-- Expected: 0 rows

-- Chapters with missing verse_content when verseCount > 0
SELECT id, title FROM chapters 
WHERE verse_count > 0 AND (verse_content IS NULL OR TRIM(verse_content) = '');
-- Expected: 0 rows

-- Verses with missing note_content when noteCount > 0
SELECT id, verse_key FROM verses 
WHERE note_count > 0 AND (note_content IS NULL OR TRIM(note_content) = '');
-- Expected: 0 rows
```

#### Verification Check Set 4: Page Coverage

```sql
-- Pages assigned to books
SELECT COUNT(*) as assigned FROM page_contents WHERE book_id IS NOT NULL;
-- Expected: 1662

-- Pages with book_id in correct range
SELECT COUNT(*) as in_range FROM page_contents pc
JOIN books b ON pc.book_id = b.id
WHERE pc.page_number < b.start_page OR pc.page_number > b.end_page;
-- Expected: 0
```

#### Verification Check Set 5: Sanity Checks

```sql
-- Total statistics
SELECT 
  (SELECT COUNT(*) FROM books) as total_books,
  (SELECT COUNT(*) FROM chapters) as total_chapters,
  (SELECT COUNT(*) FROM verses) as total_verses,
  (SELECT COUNT(*) FROM notes) as total_notes,
  (SELECT COUNT(*) FROM page_contents WHERE book_id IS NOT NULL) as pages_assigned;

-- Expected ranges:
-- total_books: 66
-- total_chapters: 500-1000
-- total_verses: 10,000-20,000
-- total_notes: 100-1,000
-- pages_assigned: 1662
```

**Phase 6 Summary Verification:**

```
□ No orphaned entities at any level
□ All chapters have verses
□ All verses have text
□ verseCount matches actual verses for all chapters
□ noteCount matches actual notes for all verses
□ aggregation content fields populated when counts > 0
□ All 1662 pages assigned to books
□ Statistics within expected ranges
```

**Decision Gate:**
```
PASS: All checks pass → WORKFLOW COMPLETE ✓
      Generate final report
      
FAIL: Referential integrity issue → Investigate and fix
FAIL: Aggregation mismatch → Re-run Phase 5
FAIL: Missing content → Investigate specific book/chapter/verse
```

**Implementation Notes:**

Service needed:
- `FinalVerificationService.verifyCompleteWorkflow() → VerificationReport`
  - Run all SQL queries above
  - Collect results
  - Generate human-readable report
  - Return success/failure status

CLI command:
- `--verify` - Runs Phase 6 verification
- Output: Detailed report with check results
- Example:
  ```
  ================================================================================
  PHASE 6: FINAL VERIFICATION REPORT
  ================================================================================
  
  Referential Integrity:
  ✓ No orphaned chapters (0 found)
  ✓ No orphaned verses (0 found)
  ✓ No orphaned notes (0 found)
  
  Hierarchical Completeness:
  ✓ No books without chapters (0 found)
  ✓ No chapters without verses (0 found)
  ✓ No verses without text (0 found)
  
  Aggregation Consistency:
  ✓ All chapters have correct verse_count
  ✓ All verses have correct note_count
  ✓ All aggregation content fields populated
  
  Page Coverage:
  ✓ 1662 pages assigned to books
  ✓ All pages in correct book range
  
  Sanity Checks:
  ✓ 39 books total (38 processed, 1 skipped)
  ✓ 847 chapters total
  ✓ 15,432 verses total
  ✓ 523 notes total
  
  ================================================================================
  ✓ WORKFLOW VERIFICATION COMPLETE - ALL CHECKS PASSED
  ================================================================================
  ```

---

## CLI Commands

### Purpose
Each CLI command executes one phase or provides status information. Commands can be run independently or as a sequence.

### Command Syntax
```bash
java -jar target/oahspe-0.0.1-SNAPSHOT.jar [COMMAND] [OPTIONS]
```

### Available Commands

#### Status & Information
```bash
--help                      Show all available commands
--status                    Show current workflow state and database counts
--workflow-state            Show detailed workflow state (phase, status, timestamps)
```

**Example Output - `--status`:**
```
================================================================================
WORKFLOW STATUS
================================================================================

Phase 0: Prerequisites Verification
Status: COMPLETED ✓
  - Total pages in database: 1831
  - OAHSPE_BOOKS pages: 1662
  - TOC page (4) exists: Yes

Phase 1: TOC Extraction
Status: PENDING
  - Books extracted: 0
  - Command: java -jar oahspe.jar --toc-extract

Phase 2: Book Registration
Status: PENDING
  - Books in database: 0
  - Command: java -jar oahspe.jar --toc-register

Phase 3: Page Assignment
Status: PENDING
  - Pages assigned: 0/1662
  - Command: java -jar oahspe.jar --assign-pages

Phase 4: Content Parsing
Status: PENDING
  - Books parsed: 0/66
  - Chapters created: 0
  - Verses created: 0
  - Commands:
    * Test single book: java -jar oahspe.jar --parse-book 1
    * Parse all: java -jar oahspe.jar --parse-all-books

Phase 5: Aggregation
Status: PENDING
  - Books aggregated: 0/66
  - Commands:
    * Test single book: java -jar oahspe.jar --aggregate-book 1
    * Aggregate all: java -jar oahspe.jar --aggregate-all

Phase 6: Final Verification
Status: PENDING
  - Command: java -jar oahspe.jar --verify

Next Step:
  Run: java -jar oahspe.jar --toc-extract

================================================================================
```

#### Phase 1: TOC Extraction
```bash
--toc-extract              Extract books from Table of Contents (read-only)
```

**What It Does:**
- Parses page 4 (TOC) 
- Outputs 39 books with page ranges (Book 35 marked SKIP)
- Does NOT save to database

**Example:**
```bash
$ java -jar oahspe.jar --toc-extract

================================================================================
TOC EXTRACTION RESULTS
================================================================================

Extracted 39 books from Table of Contents:

#   Title                              Start   End     Pages
--  ---------------------------------  ------  ------  -----
1   Book of Jehovih                    7       148     142
2   Book of Inspiration                149     280     132
3   Book of Ah'shong                   281     350     70
...
66  Book of Orachnebuahgalah           1600    1668    69

                                       Total:  1662

================================================================================
VERIFICATION CHECKS:
✓ All 39 books extracted (Book 35 marked SKIP)
✓ First book starts at page 7
✓ Last book ends at page 1668
✓ No gaps in page ranges
✓ All titles present

RESULT: READY TO REGISTER
Next: java -jar oahspe.jar --toc-register
```

#### Phase 2: Book Registration
```bash
--toc-register             Register extracted books in database
```

**What It Does:**
- Creates 66 Book entities with metadata
- Sets: title, bookNumber, startPage, endPage
- Does NOT parse content yet

**Example:**
```bash
$ java -jar oahspe.jar --toc-register

================================================================================
BOOK REGISTRATION RESULTS
================================================================================

✓ Created 38 books in database (Book 35 skipped)
✓ Set metadata for all books
✓ IDs assigned: 1-38 (35 skipped)

VERIFICATION CHECKS:
✓ 38 books in database (39 extracted - Book 35 skipped)
✓ All have valid title, startPage, endPage
✓ Page ranges contiguous (no gaps)
✓ Total pages: ~1662

RESULT: SUCCESS
Next: java -jar oahspe.jar --assign-pages
```

#### Phase 3: Page Assignment
```bash
--assign-pages             Assign pages to their owning books
```

**What It Does:**
- Links each PageContent to its Book (via page_number ranges)
- Updates PageContent.bookId for all 1662 pages

**Example:**
```bash
$ java -jar oahspe.jar --assign-pages

================================================================================
PAGE ASSIGNMENT RESULTS
================================================================================

✓ Assigned ~1662 pages to 38 books (Book 35 pages excluded)

Book Distribution:
  Book 1: 142 pages (7-148)
  Book 2: 132 pages (149-280)
  ...
  Book 66: 69 pages (1600-1668)

VERIFICATION CHECKS:
✓ 1662 pages assigned
✓ 0 pages unassigned
✓ Each book has correct page count
✓ Page numbers contiguous within books

RESULT: SUCCESS
Next: java -jar oahspe.jar --parse-book 1
```

#### Phase 4: Content Parsing

##### Parse Single Book (Testing)
```bash
--parse-book <BOOK_NUMBER>  Parse single book (for testing)
```

**What It Does:**
- Parses Book 1 (for example) through all three passes
- Pass 4A: Detect chapters
- Pass 4B: Extract verses
- Pass 4C: Extract notes
- Saves all entities to database

**Example:**
```bash
$ java -jar oahspe.jar --parse-book 1

================================================================================
PARSING BOOK 1: Book of Jehovih
================================================================================

Pass 4A: Structure Detection (Chapters)
  ✓ Loaded 142 pages
  ✓ Concatenated to 145 KB text
  ✓ Detected 7 chapters
  ✓ Created 7 Chapter entities

Pass 4B: Verse Extraction
  ✓ Processing 7 chapters
  ✓ Found 143 verses
  ✓ Created 143 Verse entities

Pass 4C: Note Extraction
  ✓ Scanning verses for notes
  ✓ Found 12 notes
  ✓ Attached to verses

RESULTS:
  - Chapters: 7
  - Verses: 143
  - Notes: 12

VERIFICATION CHECKS:
✓ All chapters have verses
✓ All verses have text
✓ No orphaned entities
✓ Verse keys valid format
✓ Note keys valid format

NEXT: Review SQL queries to verify Book 1
  SELECT * FROM chapters WHERE book_id = 1;
  SELECT COUNT(*) FROM verses WHERE chapter_id IN (...);
  
If successful, proceed with: java -jar oahspe.jar --parse-all-books
```

##### Parse All Books
```bash
--parse-all-books          Parse all 38 books sequentially (Book 35 auto-skipped)
```

**What It Does:**
- Parses all 38 books sequentially (Book 35 auto-skipped)
- Each book goes through Passes 4A-4C
- Logs progress after each book
- Stops on first error

**Example:**
```bash
$ java -jar oahspe.jar --parse-all-books

================================================================================
PARSING ALL 38 BOOKS (Sequential, Book 35 auto-skipped)
================================================================================

[1/66] Book of Jehovih (142 pages)
  ✓ 7 chapters, 143 verses, 12 notes

[2/66] Book of Inspiration (132 pages)
  ✓ 8 chapters, 156 verses, 8 notes

[3/66] Book of Ah'shong (70 pages)
  ✓ 5 chapters, 89 verses, 3 notes

...

[66/66] Book of Orachnebuahgalah (69 pages)
  ✓ 6 chapters, 112 verses, 5 notes

================================================================================
PARSING COMPLETE
================================================================================

SUMMARY:
  Books processed: 66/66 ✓
  Total chapters: 847
  Total verses: 15,432
  Total notes: 523

VERIFICATION CHECKS:
✓ Every book has chapters
✓ Every chapter has verses
✓ No orphaned entities
✓ Verse keys valid format

NEXT: java -jar oahspe.jar --aggregate-all
```

#### Phase 5: Aggregation

##### Aggregate Single Book (Testing)
```bash
--aggregate-book <BOOK_NUMBER>  Aggregate single book (for testing)
```

**What It Does:**
- For Book 1 (example):
  - Calculate verseCount per chapter, combine verse text
  - Calculate noteCount per verse, combine note text
- Updates Chapter and Verse entities

**Example:**
```bash
$ java -jar oahspe.jar --aggregate-book 1

================================================================================
AGGREGATING BOOK 1: Book of Jehovih
================================================================================

Step 5A: Aggregate Verses
  ✓ Processing 143 verses
  ✓ Updated noteCount for 12 verses with notes
  ✓ Generated noteContent for 12 verses

Step 5B: Aggregate Chapters
  ✓ Processing 7 chapters
  ✓ Updated verseCount for all chapters
  ✓ Generated verseContent for all chapters

VERIFICATION CHECKS:
✓ All verses: noteCount = actual notes
✓ All chapters: verseCount = actual verses
✓ All aggregated content populated
✓ No null content where count > 0

RESULT: SUCCESS
Next: java -jar oahspe.jar --aggregate-all
```

##### Aggregate All Books
```bash
--aggregate-all            Aggregate all 38 books (Book 35 auto-skipped)
```

**What It Does:**
- Aggregates all 38 books sequentially (Book 35 auto-skipped)
- Logs progress
- Stops on first error

**Example:**
```bash
$ java -jar oahspe.jar --aggregate-all

================================================================================
AGGREGATING ALL 38 BOOKS (Book 35 auto-skipped)
================================================================================

[1/66] Book of Jehovih
  ✓ 7 chapters, 143 verses with notes

[2/66] Book of Inspiration
  ✓ 8 chapters, 156 verses with notes

...

[66/66] Book of Orachnebuahgalah
  ✓ 6 chapters, 112 verses with notes

================================================================================
AGGREGATION COMPLETE
================================================================================

SUMMARY:
  Books aggregated: 66/66 ✓
  Total chapters aggregated: 847
  Total verses aggregated: 15,432

VERIFICATION CHECKS:
✓ All verse counts accurate
✓ All note counts accurate
✓ All aggregation content populated

NEXT: java -jar oahspe.jar --verify
```

#### Phase 6: Verification
```bash
--verify                   Run final verification (all checks)
```

**What It Does:**
- Runs comprehensive verification suite
- Checks referential integrity, completeness, aggregation, page coverage
- Outputs detailed report with pass/fail for each check
- Generates final statistics

**Example:**
```bash
$ java -jar oahspe.jar --verify

================================================================================
PHASE 6: FINAL VERIFICATION REPORT
================================================================================

Referential Integrity Checks:
  ✓ No orphaned chapters (0)
  ✓ No orphaned verses (0)
  ✓ No orphaned notes (0)

Hierarchical Completeness:
  ✓ No books without chapters (0)
  ✓ No chapters without verses (0)
  ✓ No verses without text (0)

Aggregation Consistency:
  ✓ All chapters have correct verseCount
  ✓ All verses have correct noteCount
  ✓ All aggregation content populated

Page Coverage:
  ✓ 1662/1662 pages assigned to books
  ✓ All pages in correct book range

Sanity Checks:
  ✓ Total books: 66 (expected 66)
  ✓ Total chapters: 847 (expected 500-1000)
  ✓ Total verses: 15,432 (expected 10,000-20,000)
  ✓ Total notes: 523 (expected 100-1,000)

================================================================================
✓ WORKFLOW VERIFICATION COMPLETE - ALL CHECKS PASSED
================================================================================

Final Statistics:
  - Workflow status: COMPLETE
  - Time elapsed: 2 hours 15 minutes
  - Success rate: 100% (38/38 books)
  - Data integrity: 100%

The ingestion workflow has completed successfully.
All 38 books have been parsed, aggregated, and verified.

To explore the data:
  SELECT * FROM books;
  SELECT b.title, COUNT(c.id) as chapters FROM books b 
    LEFT JOIN chapters c ON c.book_id = b.id GROUP BY b.id;
  SELECT * FROM verses WHERE verse_key = '1/1.1';
```

---

## Verification Procedures

### Complete Testing Sequence

```bash
# Step 1: Check prerequisites
java -jar oahspe.jar --status

# Step 2: Phase 1 - Extract TOC (no database changes)
java -jar oahspe.jar --toc-extract
# VERIFY MANUALLY: Review output, check 39 books extracted correctly (38 process, 1 skip)

# Step 3: Phase 2 - Register books
java -jar oahspe.jar --toc-register
# VERIFY WITH SQL: SELECT COUNT(*) FROM books; -- Expected: 66

# Step 4: Phase 3 - Assign pages
java -jar oahspe.jar --assign-pages
# VERIFY WITH SQL: SELECT COUNT(*) FROM page_contents WHERE book_id IS NOT NULL; -- Expected: 1662

# Step 5: Phase 4 - Test single book first
java -jar oahspe.jar --parse-book 1
# VERIFY WITH SQL: SELECT COUNT(*) FROM chapters WHERE book_id = 1;
#                 SELECT COUNT(*) FROM verses WHERE chapter_id IN (SELECT id FROM chapters WHERE book_id = 1);

# Step 6: If Book 1 looks good, parse all books
java -jar oahspe.jar --parse-all-books
# VERIFY WITH SQL: SELECT COUNT(*) FROM chapters;

# Step 7: Phase 5 - Aggregate all books
java -jar oahspe.jar --aggregate-all
# VERIFY WITH SQL: SELECT verse_count, verse_content FROM chapters LIMIT 5;

# Step 8: Phase 6 - Final verification
java -jar oahspe.jar --verify
# OUTPUT: Detailed report with all checks
```

### Recovery Procedures

**If Phase 1 fails (TOC extraction):**
```bash
1. Check page 4 content: SELECT raw_text FROM page_contents WHERE page_number = 4;
2. Compare with PDF Table of Contents
3. If format different: Adjust TableOfContentsParser regex pattern
4. Re-run: java -jar oahspe.jar --toc-extract
```

**If Phase 2 fails (Book registration):**
```bash
1. Clear books: DELETE FROM books;
2. Fix TOC parser if needed
3. Re-run: java -jar oahspe.jar --toc-register
```

**If Phase 3 fails (Page assignment):**
```bash
1. Clear assignments: UPDATE page_contents SET book_id = NULL;
2. Verify book ranges: SELECT * FROM books ORDER BY book_number;
3. Re-run: java -jar oahspe.jar --assign-pages
```

**If Phase 4 fails for one book:**
```bash
1. Check raw content: SELECT raw_text FROM page_contents WHERE book_id = <BOOK_ID> ORDER BY page_number;
2. Clear parsed data:
   DELETE FROM notes WHERE verse_id IN (SELECT id FROM verses WHERE chapter_id IN (SELECT id FROM chapters WHERE book_id = <BOOK_ID>));
   DELETE FROM verses WHERE chapter_id IN (SELECT id FROM chapters WHERE book_id = <BOOK_ID>);
   DELETE FROM chapters WHERE book_id = <BOOK_ID>;
3. Adjust parser regex
4. Re-run: java -jar oahspe.jar --parse-book <BOOK_ID>
```

---

## SQL Verification Queries

All queries provided in Phase breakdown sections above. Use as a reference:

- **Phase 0:** 4 prerequisite checks
- **Phase 1:** 8 TOC extraction checks
- **Phase 2:** 6 book registration checks
- **Phase 3:** 5 page assignment checks
- **Phase 4A:** 4 chapter detection checks
- **Phase 4B:** 5 verse extraction checks
- **Phase 4C:** 4 note extraction checks
- **Phase 5A:** 3 verse aggregation checks
- **Phase 5B:** 3 chapter aggregation checks
- **Phase 6:** 20+ verification checks

Total: 65+ verification queries covering all aspects of the workflow.

---

## Error Recovery Guide

### Failure Scenarios by Phase

#### Phase 0: Prerequisites Failed
```
Error: PageContent count < 1831
Root Cause: Phase 1 (page loading) didn't complete or failed
Recovery:
  1. Run Phase 1 page loading: java -jar oahspe.jar --load-pages data/OAHSPE.pdf
  2. Verify all 1831 pages loaded
  3. Re-check Phase 0
```

#### Phase 1: TOC Extraction Failed
```
Error: Fewer than 39 books extracted
Root Cause: TOC format differs from expected pattern
Recovery:
  1. Inspect page 4: SELECT raw_text FROM page_contents WHERE page_number = 4;
  2. Compare with PDF page 4
  3. If format different (e.g., different dot pattern): Adjust regex in TableOfContentsParser
  4. Re-run: java -jar oahspe.jar --toc-extract
  5. Verify output shows 39 books (38 to process, 1 skipped) before proceeding
```

#### Phase 2: Book Registration Failed
```
Error: Duplicate books or wrong metadata
Root Cause: Phase 1 output invalid or re-run without cleanup
Recovery:
  1. Clear books: DELETE FROM books;
  2. Verify Phase 1 output correct
  3. Re-run: java -jar oahspe.jar --toc-register
```

#### Phase 3: Page Assignment Failed
```
Error: Pages assigned to wrong book or gaps detected
Root Cause: Book page ranges incorrect
Recovery:
  1. Check book ranges: SELECT * FROM books ORDER BY book_number;
  2. If ranges wrong, clear and re-do Phase 2
  3. Clear assignments: UPDATE page_contents SET book_id = NULL;
  4. Re-run: java -jar oahspe.jar --assign-pages
```

#### Phase 4: Content Parsing Failed
```
Error: Specific book missing chapters or verses
Root Cause: Regex pattern doesn't match actual text format
Recovery:
  1. Check raw text for problematic book:
     SELECT raw_text FROM page_contents 
     WHERE book_id = <BOOK_ID> 
     ORDER BY page_number;
  2. Visually inspect actual chapter/verse format
  3. Compare against regex pattern in ChapterDetectionService/VerseExtractionService
  4. Adjust pattern if needed
  5. Clear parsed data for this book:
     DELETE FROM notes WHERE verse_id IN (SELECT id FROM verses WHERE chapter_id IN (SELECT id FROM chapters WHERE book_id = <BOOK_ID>));
     DELETE FROM verses WHERE chapter_id IN (SELECT id FROM chapters WHERE book_id = <BOOK_ID>);
     DELETE FROM chapters WHERE book_id = <BOOK_ID>;
  6. Re-run: java -jar oahspe.jar --parse-book <BOOK_ID>
```

#### Phase 5: Aggregation Failed
```
Error: Count mismatch or missing content
Root Cause: Aggregation logic bug
Recovery:
  1. Clear aggregation fields:
     UPDATE verses SET note_count = 0, note_content = NULL;
     UPDATE chapters SET verse_count = 0, verse_content = NULL;
  2. Verify parsing completed correctly: SELECT COUNT(*) FROM verses;
  3. Re-run: java -jar oahspe.jar --aggregate-all
```

#### Phase 6: Verification Failed
```
Error: Specific check fails (e.g., "chapters with wrong verse_count")
Root Cause: Aggregation inconsistency or parsing error
Recovery:
  1. Identify specific check that failed
  2. Run the query from Phase 6 to see which entities have issues
  3. Investigate root cause (Phase 4 or 5)
  4. Fix by re-running appropriate phase
  5. Re-run: java -jar oahspe.jar --verify
```

---

## Service Layer Design

### Service Dependencies & Responsibilities

```
TableOfContentsParser
  ├─ Dependency: PageContentRepository
  ├─ Method: List<BookMetadata> extractBooks()
  └─ Responsibility: Parse page 4, extract book boundaries

BookRegistrationService
  ├─ Dependency: BookRepository, TableOfContentsParser
  ├─ Method: void registerBooks(List<BookMetadata>)
  └─ Responsibility: Create Book entities in database

PageAssignmentService
  ├─ Dependency: PageContentRepository, BookRepository
  ├─ Method: void assignPagesToBooks()
  └─ Responsibility: Link pages to books via page number ranges

ContentParsingService
  ├─ Dependency: ChapterDetectionService, VerseExtractionService, NoteExtractionService
  ├─ Method: void parseBook(Book, List<PageContent>)
  └─ Responsibility: Orchestrate 3-pass parsing (chapters, verses, notes)

ChapterDetectionService
  ├─ Dependency: ChapterRepository
  ├─ Method: List<Chapter> detectChapters(String bookText)
  └─ Responsibility: Find chapter headers, create Chapter entities

VerseExtractionService
  ├─ Dependency: VerseRepository
  ├─ Method: List<Verse> extractVerses(String chapterText, Chapter)
  └─ Responsibility: Find verse keys, create Verse entities

NoteExtractionService
  ├─ Dependency: NoteRepository
  ├─ Method: void extractNotes(Verse)
  └─ Responsibility: Find note markers, create Note entities

AggregationOrchestrator
  ├─ Dependency: VerseAggregationService, ChapterAggregationService, BookAggregationService
  ├─ Method: void aggregateAllBooks()
  └─ Responsibility: Orchestrate bottom-up aggregation

VerseAggregationService
  ├─ Dependency: VerseRepository
  ├─ Method: Verse aggregateVerse(Verse)
  └─ Responsibility: Calculate noteCount, combine note text

ChapterAggregationService
  ├─ Dependency: ChapterRepository
  ├─ Method: Chapter aggregateChapter(Chapter)
  └─ Responsibility: Calculate verseCount, combine verse text

BookAggregationService
  ├─ Dependency: BookRepository
  ├─ Method: void aggregateBook(Book)
  └─ Responsibility: Orchestrate chapter/verse aggregation for one book

FinalVerificationService
  ├─ Dependency: BookRepository, ChapterRepository, VerseRepository, NoteRepository
  ├─ Method: VerificationReport verifyCompleteWorkflow()
  └─ Responsibility: Run all verification checks, generate report

IngestionOrchestrator
  ├─ Dependency: All services above
  ├─ Method: void executeFullWorkflow()
  └─ Responsibility: Coordinate phases 0-6, manage gates, handle errors
```

### Service Interfaces (Pseudo-code)

```java
interface TableOfContentsParser {
  List<BookMetadata> extractBooks();
}

interface BookRegistrationService {
  void registerBooks(List<BookMetadata>);
}

interface PageAssignmentService {
  void assignPagesToBooks();
}

interface ContentParsingService {
  void parseBook(Book, List<PageContent>);
  void parseAllBooks();
}

interface AggregationOrchestrator {
  void aggregateBook(Book);
  void aggregateAllBooks();
}

interface FinalVerificationService {
  VerificationReport verifyCompleteWorkflow();
  boolean verifyBook(Integer bookId);
  boolean verifyChapter(Long chapterId);
}

interface IngestionOrchestrator {
  void executeFullWorkflow(ProgressCallback callback);
  WorkflowState getStatus();
}
```

---

## Data Model Changes

### New/Modified Database Tables

#### Books Table - New Fields

```sql
ALTER TABLE books ADD COLUMN start_page INT;
ALTER TABLE books ADD COLUMN end_page INT;
ALTER TABLE books ADD COLUMN book_number INT UNIQUE;
```

#### Chapters Table - New Fields

```sql
ALTER TABLE chapters ADD COLUMN verse_count INT DEFAULT 0;
ALTER TABLE chapters ADD COLUMN verse_content LONGTEXT;
```

#### Verses Table - New Fields

```sql
ALTER TABLE verses ADD COLUMN note_count INT DEFAULT 0;
ALTER TABLE verses ADD COLUMN note_content LONGTEXT;
```

#### PageContent Table - New Fields

```sql
ALTER TABLE page_contents ADD COLUMN book_id INT;
ALTER TABLE page_contents ADD CONSTRAINT fk_page_book 
  FOREIGN KEY (book_id) REFERENCES books(id);
```

### Entity Class Updates

**Book.java:**
```java
@Column(name = "start_page")
private Integer startPage;

@Column(name = "end_page")
private Integer endPage;

@Column(name = "book_number", unique = true)
private Integer bookNumber;
```

**Chapter.java:**
```java
@Column(name = "verse_count")
@Builder.Default
private Integer verseCount = 0;

@Lob
@Column(columnDefinition = "LONGTEXT", name = "verse_content")
private String verseContent;
```

**Verse.java:**
```java
@Column(name = "note_count")
@Builder.Default
private Integer noteCount = 0;

@Lob
@Column(columnDefinition = "LONGTEXT", name = "note_content")
private String noteContent;
```

**PageContent.java:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "book_id")
private Book book;

@Column(name = "book_id", insertable = false, updatable = false)
private Integer bookId;
```

---

## Implementation Checklist

### Phase 0: Prerequisites
- [ ] Phase 1 (page loading) completed and verified
- [ ] 1831 pages in database with content
- [ ] Database migration completed

### Phase 1: TOC Extraction
- [ ] Create `TableOfContentsParser` service
- [ ] Implement `extractBooks()` method with regex
- [ ] Add `--toc-extract` CLI command
- [ ] Verify output shows 39 books (Book 35 SKIP noted)
- [ ] Add 8 verification checks

### Phase 2: Book Registration
- [ ] Create `BookRegistrationService` service
- [ ] Add fields to `Book` entity (startPage, endPage, bookNumber)
- [ ] Implement `registerBooks()` method
- [ ] Add `--toc-register` CLI command
- [ ] Add 6 verification SQL queries

### Phase 3: Page Assignment
- [ ] Create `PageAssignmentService` service
- [ ] Add `bookId` field to `PageContent` entity
- [ ] Implement `assignPagesToBooks()` method
- [ ] Add `--assign-pages` CLI command
- [ ] Add 5 verification SQL queries

### Phase 4: Content Parsing
- [ ] Create `ChapterDetectionService` service
  - [ ] Implement chapter regex pattern
  - [ ] Add 4 verification SQL queries (4A)
- [ ] Create `VerseExtractionService` service
  - [ ] Implement verse key regex pattern
  - [ ] Add 5 verification SQL queries (4B)
- [ ] Create `NoteExtractionService` service
  - [ ] Implement note key regex pattern
  - [ ] Add 4 verification SQL queries (4C)
- [ ] Create `ContentParsingService` orchestrator
  - [ ] Implement 3-pass parsing (4A → 4B → 4C)
  - [ ] Add `--parse-book <N>` CLI command
  - [ ] Add `--parse-all-books` CLI command
- [ ] Test single book thoroughly before parsing all

### Phase 5: Aggregation
- [ ] Add fields to entities (verseCount, noteCount, verseContent, noteContent)
- [ ] Create `VerseAggregationService` service
  - [ ] Implement verse aggregation
  - [ ] Add 3 verification SQL queries (5A)
- [ ] Create `ChapterAggregationService` service
  - [ ] Implement chapter aggregation
  - [ ] Add 3 verification SQL queries (5B)
- [ ] Create `BookAggregationService` service
  - [ ] Orchestrate aggregation (verses → chapters → book)
- [ ] Create `AggregationOrchestrator`
  - [ ] Add `--aggregate-book <N>` CLI command
  - [ ] Add `--aggregate-all` CLI command
- [ ] Test single book thoroughly before aggregating all

### Phase 6: Verification
- [ ] Create `FinalVerificationService` service
  - [ ] Implement 20+ verification checks
  - [ ] Generate human-readable report
- [ ] Add `--verify` CLI command
- [ ] Add database migration for all table changes
- [ ] Document expected data ranges (38 books processed, 500-1000+ chapters, 10k-20k+ verses)

### Phase 7: CLI & Orchestration
- [ ] Create main `IngestionOrchestrator` service
- [ ] Add all CLI commands to `IngestionCliRunner`
- [ ] Add `--status` command to show workflow progress
- [ ] Add `--help` command with all commands listed
- [ ] Test complete workflow: Phase 0 → Phase 6
- [ ] Create error recovery procedures documentation

### Testing & Quality
- [ ] Unit tests for each service
- [ ] Integration test: Parse single book completely
- [ ] Integration test: Full workflow 38 books (Book 35 skipped)
- [ ] Manual testing: Run each CLI command independently
- [ ] Manual verification: Run SQL checks after each phase
- [ ] Error injection: Test recovery procedures

### Documentation
- [ ] Update this design document with final decisions
- [ ] Create API documentation for service interfaces
- [ ] Create troubleshooting guide for common errors
- [ ] Document regex patterns and their purpose
- [ ] Create database migration scripts

---

## Parallel Processing (Phase 2 - After Sequential Works)

### When to Enable
Only after:
- Sequential Phase 4 & 5 works for all 38 books (Book 35 auto-skipped)
- Phase 6 verification passes 100%
- Manual spot-check of 5 random books shows correct data

### How to Enable

Add `@EnableAsync` to main application class and update CLI:

```bash
--parse-all-books --parallel    Parse books in parallel (thread pool)
--aggregate-all --parallel      Aggregate in parallel
--verify --parallel             Verify books in parallel
```

### Thread Safety Guarantees

Each book processing:
- Runs in separate thread
- Has own database transaction (REQUIRES_NEW)
- No shared mutable state
- No cross-book communication
- Can fail independently without affecting others

### Expected Performance (Estimated)

**Sequential (Current):**
- Phase 4: ~30 minutes (500 pages/minute)
- Phase 5: ~10 minutes
- Phase 6: ~2 minutes
- Total: ~42 minutes

**Parallel (With 8 cores):**
- Phase 4: ~5 minutes (38 books / 8 cores, Book 35 skipped)
- Phase 5: ~2 minutes
- Phase 6: ~2 minutes
- Total: ~9 minutes

### Configuration

```properties
# application.properties
spring.task.execution.pool.core-size=8
spring.task.execution.pool.max-size=16
spring.task.execution.pool.queue-capacity=100

# Or in code:
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("OahspeBookParser-");
        executor.initialize();
        return executor;
    }
}
```

---

## Final Notes

### Why This Design Works

1. **Structural Awareness:** TOC establishes book boundaries upfront (not guessed)
2. **Complete Context:** All pages for a book available before parsing (no state loss)
3. **Independent Units:** Each book can be processed, tested, verified separately
4. **Quality Gates:** Verification at every step prevents bad data from propagating
5. **Manual Control:** User decides when to proceed (can pause and inspect)
6. **Easy Debugging:** Failures pinpointed to specific book and phase
7. **Resumable:** Can resume from any phase without reprocessing
8. **Observable:** Logs and SQL queries provide transparency
9. **Testable:** Single-book commands enable rapid iteration
10. **Parallel-Safe:** Sequential-first proves correctness; parallelization is additive

### Success Indicators

After completing all phases:
- ✅ 38 books with correct titles and page ranges (Book 35 Saphah skipped)
- ✅ 500-1000 chapters with verse aggregates
- ✅ 10,000-20,000 verses with note aggregates
- ✅ All verseCount and noteCount accurate
- ✅ All verseContent and noteContent populated
- ✅ Zero orphaned entities
- ✅ Phase 6 verification 100% pass rate
- ✅ Clear audit trail in PageContent

### Future Enhancements (Post-Phase 8)

- **Phase 8.5: Book of Saphah** (separate workflow)
  - Book 35 has different structure (different chapter/verse format)
  - Will require custom parser and ingestion logic
  - Planned as separate phase after Phase 8 complete
  - Not blocking Phase 8 completion
- Phase 9: Translation workflow (glossary → titles → verses → notes)
- Phase 10: Content linking (cross-references, image captions)
- Phase 11: REST API layer for data access
- Phase 12: Web UI for browsing books/chapters/verses

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** Ready for Implementation (Book 35 Saphah marked for Phase 8.5)  
**Next Action:** Begin Phase 0 verification and Phase 1 implementation
