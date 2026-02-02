# Phase 8: PDF Foundation Layer Integration

**Date:** February 2, 2026  
**Status:** Integration Complete - Ready for Implementation  
**Audience:** Developers implementing Phase 8  
**Version:** 1.0

---

## Executive Summary

The current ingestion workflow based on concatenated `rawText` for each page is fundamentally broken for the Oahspe PDF because:

1. **Two-Column Layout**: The PDF uses left and right columns
2. **Verses Broken Across Columns**: Verse might start in left column, continue in right column
3. **Verses Broken Across Pages**: Verse might end on page N and continue on page N+1
4. **Interleaved Footnotes**: Footnotes appear at the bottom of each column, not at page end
5. **Reading Order Destruction**: Simple text concatenation destroys the correct reading order

**Solution:** Implement a **PDF Foundation Layer** that extracts geometry-aware `TextFragment` entities before parsing. This new Phase 0.5 provides:

- **Exact coordinates** (x, y, width, height) for each text element
- **Font properties** (name, size, bold, italic) for structure detection
- **Column detection** by spatial clustering of x-coordinates
- **Reading order reconstruction** (left-column top→bottom, right-column top→bottom)
- **Footnote identification** by y-coordinate (footer area)

## What Changed in Phase 8 Design

### Previous Approach (BROKEN)
```
For each book:
  → Load all pages as concatenated rawText
  → Parse verses using regex on concatenated text
  → Result: Verses broken, footnotes misaligned, data corrupted
```

### New Approach (FIXED)
```
Phase 0.5: Extract TextFragments with geometry
  → 1831 PdfPage entities
  → ~100,000+ TextFragment entities with x,y,font properties
  → TextFragment entities with column assignment and reading order

Phase 1-6: Standard workflow using TextFragments
  → Books, Chapters, Verses, Footnotes parsed from geometry-aware data
  → No more broken verses or misaligned footnotes
  → Perfect extraction despite complex layout
```

---

## Phase 0.5: PDF Foundation Layer (NEW)

### Purpose
Extract text with **geometry information** (position, size, font) from PDF before any parsing happens.

### Key Concepts

#### TextFragment
A single text element with exact position and properties:
```java
class TextFragment {
    Long id;
    PdfPage page;
    
    String text;                    // Actual text: "And the spirit"
    float x, y;                     // Position: (50.5, 230.0)
    float width, height;            // Size: (200.0, 12.0)
    String fontName;                // Font: "Times New Roman"
    float fontSize;                 // Size: 11.0
    boolean bold;                   // Bold: false
    boolean italic;                 // Italic: false
    int columnNumber;               // Column: 0 (left) or 1 (right)
    int readingOrder;               // Sequence: 1, 2, 3, ... per column
}
```

#### Column Detection
Fragments are clustered by x-coordinate:
```
Left column:   x ∈ [50, 300]     → column_number = 0
Right column:  x ∈ [350, 550]    → column_number = 1
Divider:       x ≈ 325           (mid-page boundary)
```

#### Reading Order
Within each column, fragments are sorted top-to-bottom by y-coordinate:
```
Column 0, reading_order=1  → y=50   (top of page, left column)
Column 0, reading_order=2  → y=80   (next line, left column)
Column 0, reading_order=3  → y=110  (next line, left column)
...
Column 0, reading_order=N  → y=700  (bottom of left column)
Column 1, reading_order=1  → y=50   (top of page, right column)
Column 1, reading_order=2  → y=80   (next line, right column)
...
```

### Implementation Tasks

#### Entity Classes
1. **PdfPage.java** (if not already exists)
   - `Long id`
   - `int pageNumber` (1-1831)
   - `String extractedText` (fallback only)
   - `List<TextFragment> fragments` (primary data)
   - `List<PdfImage> images`
   - `boolean processed`
   - `LocalDateTime loadedAt`

2. **TextFragment.java** (NEW)
   - All fields listed in example above
   - Indexes: (pdfPage + columnNumber), (pdfPage + readingOrder)
   - Foreign key: pdfPage_id

3. **PdfImage.java** (if not already exists)
   - `Long id`
   - `PdfPage page`
   - `byte[] data`
   - `String mimeType`
   - `float x, y, width, height`
   - `String imageId`

#### Database Migrations
Create tables for TextFragment and PdfImage with proper indexes.

#### Service Classes

**PdfGeometryExtractor.java**
- Dependency: Apache PDFBox library
- Method: `void extractGeometryFromPdf(File pdfFile, String outputPath)`
- Logic:
  1. Open PDF with PDFBox
  2. For each page:
     - Extract text with coordinates using PDFTextStripper
     - Extract images with coordinates
     - For each text element:
       - Create TextFragment with text, x, y, width, height, font properties
       - Save to TextFragmentRepository
     - Detect columns (see ColumnDetectionService)
     - Assign reading order (see ReadingOrderCalculator)

**ColumnDetectionService.java**
- Method: `List<Integer> detectColumnBoundaries(List<TextFragment>)`
- Logic:
  1. Collect all x-coordinates from fragments
  2. Cluster into groups (left and right columns)
  3. Use k-means or histogram approach
  4. Return cluster boundaries

**ReadingOrderCalculator.java**
- Method: `void assignReadingOrder(List<TextFragment>)`
- Logic:
  1. Group fragments by columnNumber
  2. Within each column: sort by y-coordinate
  3. Assign sequential readingOrder (1, 2, 3, ...)

#### CLI Command
```bash
java -jar oahspe.jar --extract-geometry
```
- Loads PDF from configured location
- Runs extraction pipeline
- Saves all TextFragments and PdfImages
- Logs statistics: N fragments, M images, P pages processed

#### Verification Queries
```sql
-- Check fragment count
SELECT COUNT(*) FROM text_fragments;
-- Expected: >50,000

-- Check column detection
SELECT COUNT(DISTINCT column_number) FROM text_fragments;
-- Expected: 2 (left=0, right=1)

-- Check reading order
SELECT COUNT(*) FROM text_fragments WHERE reading_order IS NULL;
-- Expected: 0

-- Sample page geometry
SELECT id, text, x_coordinate, y_coordinate, column_number, reading_order
FROM text_fragments
WHERE pdf_page_id = (SELECT id FROM pdf_pages WHERE page_number = 50)
ORDER BY column_number, reading_order
LIMIT 20;
```

---

## Phase 1-6: Unchanged (TOC-Based Workflow)

These phases remain as designed in PHASE8_TOC_BASED_INGESTION_DESIGN.md:
- Phase 0: Prerequisites Verification
- Phase 1: TOC Extraction
- Phase 2: Book Registration
- Phase 3: Page Assignment
- Phase 4: Content Parsing (**NOW USES TEXTFRAGMENTS**)
- Phase 5: Aggregation
- Phase 6: Final Verification

---

## Phase 4: Content Parsing (UPDATED)

### Key Change: TextFragment-Based Parsing

**Before (BROKEN):**
```java
for (PageContent page : book.getPages()) {
    String text = page.getRawText();  // Concatenated text
    // Parse using regex - FAILS on two-column layout
    List<Verse> verses = verseParser.parse(text);
}
```

**After (FIXED):**
```java
List<TextFragment> fragments = textFragmentRepository
    .findByPageInOrderByPageNumberAscColumnNumberAscReadingOrderAsc(book.getPages());

// Reconstruct reading order
String logicalText = reconstructReadingOrder(fragments);

// Detect structure using font properties
List<Chapter> chapters = chapterDetector.detectChapters(fragments);
List<Verse> verses = verseExtractor.extractVerses(fragments);
List<Footnote> footnotes = footnoteExtractor.extractFootnotes(fragments);
```

### Service Changes

#### ChapterDetectionService
- Uses `fontSize` and `bold` properties to detect headers
- Previously: `Pattern.compile("^Chapter\\s+\\d+")`
- Now: `if (fragment.fontSize > 14 && fragment.isBold()) { ... }`

#### VerseExtractionService
- Processes fragments in reading order (not concatenated text)
- Identifies verse markers: superscript numbers (¹, ², ³) or [N]
- Correctly handles multi-line verses across columns/pages
- Reading order ensures proper assembly

#### FootnoteExtractionService
- Identifies footnotes by y-coordinate range
- Footer detection: `if (fragment.y > page_height - footer_margin) { ... }`
- Matches footnote markers to verse markers
- No longer loses footnotes at page breaks

---

## Impact on Other Components

### Database Schema

**New tables:**
- `text_fragments` (with indexes on pdfPage + columnNumber, pdfPage + readingOrder)
- `pdf_images` (if not already present)

**Modified tables:**
- `page_contents` (no changes needed, TextFragments independent)
- `books`, `chapters`, `verses`, `footnotes` (no changes needed)

### Dependencies

**New library:**
- `org.apache.pdfbox:pdfbox` (for PDF geometry extraction)

### Service Layer

**New services:**
- `PdfGeometryExtractor`
- `ColumnDetectionService`
- `ReadingOrderCalculator`

**Modified services:**
- `ContentParsingService` (uses TextFragments instead of rawText)
- `ChapterDetectionService` (uses font properties)
- `VerseExtractionService` (uses reading order)
- `FootnoteExtractionService` (uses y-coordinate detection)

### CLI

**New command:**
- `--extract-geometry` (Phase 0.5, must run before Phase 4)

**Updated command:**
- `--parse-book <N>` (now accepts TextFragments from Phase 0.5)
- `--parse-all-books` (now uses TextFragments)

---

## Migration Path

### For Existing Implementations

If you've already started implementing Phase 8 with rawText approach:

1. **Stop using rawText** for content parsing
2. **Implement Phase 0.5** (extract geometry) first
3. **Update Phase 4 services** to use TextFragments
4. **Verify with test PDF** before full run

### No Data Loss

Phase 0.5 is non-destructive:
- Adds new TextFragment data only
- Does not modify existing page_contents
- rawText still available as fallback
- Can re-run Phase 0.5 without issues

---

## Success Criteria

### Phase 0.5 Completion
```
✓ 1831 PdfPage entities exist
✓ >50,000 TextFragment entities created
✓ All fragments have coordinates (x, y, width, height)
✓ All fragments have font properties (fontSize, bold, italic)
✓ Column numbers assigned (0 or 1)
✓ Reading order assigned sequentially per column
✓ No NULL values in required fields
```

### Phase 4 Parsing
```
✓ All chapters detected using font properties
✓ All verses extracted using fragment reading order
✓ All footnotes identified by y-coordinate
✓ No verses broken across columns
✓ No footnotes lost at page boundaries
✓ Cross-page verses assembled correctly
```

---

## Key Files Updated

1. **PHASE8_TOC_BASED_INGESTION_DESIGN.md**
   - Added "PDF Foundation Layer" section before Phase 4
   - Updated Phase 4 description with geometry-aware approach
   - Added database schema for TextFragment and PdfImage

2. **PHASE8_IMPLEMENTATION_CHECKLIST.md**
   - Added "Phase 0.5: PDF Foundation Layer - TextFragment Extraction"
   - Updated Phase 4 checklist to reference TextFragments
   - Added verification tasks for Phase 0.5

3. **PHASE8_QUICK_REFERENCE.md**
   - Added Phase 0.5 to phase table
   - Updated testing workflow to include geometry extraction
   - Updated CLI commands list
   - Added new services to implementation list

4. **PHASE8_PDF_FOUNDATION_INTEGRATION.md** (THIS FILE)
   - Comprehensive integration guide
   - Problem statement and solution
   - Implementation tasks and verification

---

## Questions & Answers

**Q: Can I skip Phase 0.5 and use rawText?**
A: No. The rawText approach is proven to fail with the two-column layout. Phase 0.5 is mandatory.

**Q: Will this break existing data?**
A: No. Phase 0.5 is purely additive. All existing data remains unchanged.

**Q: How long does Phase 0.5 take?**
A: 4-6 hours depending on PDF library setup and PDF file size.

**Q: Do I need to modify Book/Chapter/Verse entities?**
A: No. TextFragments are independent. Existing entities unchanged.

**Q: What if PDFBox can't extract coordinates?**
A: Use PDFTextStripper with detailed position tracking. Most PDFs support this.

**Q: Can I parallelize Phase 0.5?**
A: Yes, but PDFBox is not thread-safe. Extract sequentially, persist in batches.

---

## References

- Main design: [PHASE8_TOC_BASED_INGESTION_DESIGN.md](PHASE8_TOC_BASED_INGESTION_DESIGN.md)
- Implementation checklist: [PHASE8_IMPLEMENTATION_CHECKLIST.md](PHASE8_IMPLEMENTATION_CHECKLIST.md)
- Quick reference: [PHASE8_QUICK_REFERENCE.md](PHASE8_QUICK_REFERENCE.md)
- Phase 8 unified design: [phase8_toc_pdf_ingestion_design.md](phase8_toc_pdf_ingestion_design.md)
