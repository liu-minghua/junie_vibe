# Phase 7 Usage Guide: Page-Based Ingestion Workflow

**Date:** January 31, 2026  
**Version:** 1.0  
**Audience:** Developers and operators running Oahspe ingestion  
**Related:** [Database Migrations](DATABASE_MIGRATIONS.md) | [Migration V001 Quick Reference](FLYWAY_MIGRATION_V001_QUICK_REFERENCE.md)

---

## Overview

Phase 7 introduces a resilient two-workflow architecture for ingesting the Oahspe PDF with complete page-level tracking and verification.

### Key Features

- ✅ **Page-Level Extraction**: All 1831 pages stored individually with raw text and images
- ✅ **Category-Based Processing**: Automatic routing (Books, Glossaries, Index)
- ✅ **3-Phase Workflow**: Load → Cleanup → Ingest with verification gates
- ✅ **Error Recovery**: Failed ingestion can restart from saved PageContent
- ✅ **Page Number Tracking**: Every entity links back to source page via `pageNumber` field

---

## CLI Commands

### 1. Complete Workflow (Recommended)

Runs all 3 phases with automatic verification:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--workflow data/OAHSPE.pdf"
```

**What happens:**
1. **Phase 1 (30-60s)**: Loads all 1831 pages from PDF → PageContent entities
2. **Gate 1**: Verifies all pages loaded successfully
3. **Phase 2 (5-10s)**: Cleans up old Books/Chapters/Verses (preserves PageContent)
4. **Gate 2**: Verifies cleanup complete, PageContent preserved
5. **Phase 3 (2-5 min)**: Ingests PageContent → Books/Chapters/Verses/Glossaries/Index
6. **Gate 3**: Verifies all required pages ingested
7. **Summary**: Reports statistics and completion status

**Expected Output:**
```
================================================================================
PHASE 7 WORKFLOW: Complete 3-Phase Ingestion
================================================================================
PDF File: data/OAHSPE.pdf

=== Phase 1: Loading pages from PDF ===
[50/1831] Loaded page 50/1831 - Category: OAHSPE_BOOKS
[100/1831] Loaded page 100/1831 - Category: OAHSPE_BOOKS
...
✓ Gate 1 PASSED: All 1831 pages loaded

=== Phase 2: Cleaning up old data ===
Deleted 43 books, 250 chapters, 5000 verses, 100 notes, 150 images
✓ Gate 2 PASSED: PageContent preserved (1831 pages)

=== Phase 3: Ingesting content ===
[50/1662] Ingested page 50 [OAHSPE_BOOKS]
...
✓ Gate 3 PASSED: All required pages ingested

================================================================================
✓ WORKFLOW COMPLETE!
================================================================================
Duration: 180000 ms (180.0 seconds)
Status: COMPLETED
```

---

### 2. Individual Phase Commands

#### Load Pages Only

Extract all pages from PDF without ingesting content:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--load-pages data/OAHSPE.pdf"
```

**Use cases:**
- Testing page extraction
- Debugging PDF parsing issues
- Pre-loading before cleanup decision

**Expected Duration:** 30-60 seconds

---

#### Cleanup Old Data

Delete previously ingested Books/Chapters/Verses while preserving PageContent:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--cleanup"
```

**⚠️ WARNING:** This is destructive! All ingested content will be deleted.

**Deleted:**
- ✗ Books
- ✗ Chapters
- ✗ Verses
- ✗ Notes
- ✗ Images
- ✗ Glossary Terms
- ✗ Index Entries

**Preserved:**
- ✓ PageContent (raw page data)
- ✓ PageImage (embedded images)

**Use cases:**
- Re-ingesting after parser improvements
- Clearing database before fresh run
- Troubleshooting ingestion issues

---

#### Ingest Pages

Parse PageContent into domain entities (Books, Chapters, Verses):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--ingest-pages"
```

**Requirements:**
- PageContent must be loaded (via --load-pages or --workflow)
- Old data cleaned up (via --cleanup or --workflow)

**Expected Duration:** 2-5 minutes for 1662 pages (OAHSPE_BOOKS + GLOSSARIES + INDEX)

**Process:**
1. Queries all PageContent where category.shouldIngest() == true
2. Routes to appropriate parser:
   - OAHSPE_BOOKS → OahspeParser
   - GLOSSARIES → GlossaryParser
   - INDEX → IndexParser
3. Creates entities with pageNumber populated
4. Marks PageContent as ingested

---

#### Verify Content Linking

Generates report on pageNumber field population:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--verify-links"
```

**Sample Output:**
```
Content Linking Report:
  Pages: 1831 total, 1662 linked (90.8%)
  Images: 150 linked, 5 orphaned
  Books: 43 with page, 0 without
  Chapters: 250 with page, 0 without
  Verses: 5000 with page, 0 without
  Notes: 100 with page, 0 without
  Status: FULLY LINKED ✓
```

**Use cases:**
- Quality assurance after ingestion
- Finding orphaned content
- Debugging page linking issues

---

#### Resume Workflow

Resume interrupted workflow from last saved state:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--resume oahspe-ingestion"
```

**Note:** Currently not fully implemented - pdfPath needs to be stored in WorkflowState

---

#### Legacy Mode (Backward Compatible)

Run old single-pass ingestion (pre-Phase 7):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="data/OAHSPE.pdf"
```

**⚠️ Deprecated:** Use `--workflow` for new ingestion runs

---

## Database Migrations

### Automatic Schema Management

Flyway migrations run automatically on application startup:

```bash
mvn spring-boot:run
```

**First time setup:**
1. Application starts
2. Flyway detects missing migrations
3. Applies all pending migrations (currently V001)
4. Populates schema
5. Application ready to use

### Migration V001: Classification Metadata

**File:** `src/main/resources/db/migration/V001__Initial_PageContent_Classification_Metadata.sql`

**Purpose:** Add lightweight classification fields to support two-step approach for identifying pages needing geometry extraction.

**New Columns:**
- `text_length`, `line_count`, `verse_count` - Step 1 metrics
- `has_footnote_markers`, `has_illustration_keywords`, `has_saphah_keywords`, `contains_images` - Classification flags
- `needs_geometry`, `is_book_content` - Step 2 results

**Duration:** <5 seconds  
**Impact:** Zero (additive only, all columns nullable)  
**Indexes:** idx_needs_geometry, idx_is_book_content

**For more details:** See [DATABASE_MIGRATIONS.md](DATABASE_MIGRATIONS.md) and [FLYWAY_MIGRATION_V001_QUICK_REFERENCE.md](FLYWAY_MIGRATION_V001_QUICK_REFERENCE.md)

---

## Database Schema

### Key Tables

#### page_contents

Stores raw extracted page data with lightweight classification metadata:

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| page_number | INT | 1-based page number (unique) |
| category | VARCHAR(50) | PageCategory enum |
| raw_text | TEXT | Extracted text content |
| extracted_at | TIMESTAMP | When extracted from PDF |
| ingested | BOOLEAN | Whether parsed into entities |
| ingested_at | TIMESTAMP | When ingestion completed |
| error_message | TEXT | Any extraction/ingestion errors |
| **text_length** | **INT** | **Length of raw text (Step 1 classification)** |
| **line_count** | **INT** | **Number of lines (Step 1 classification)** |
| **verse_count** | **INT** | **Number of verses detected (Step 1 classification)** |
| **has_footnote_markers** | **BOOLEAN** | **Step 1 classification flag** |
| **has_illustration_keywords** | **BOOLEAN** | **Step 1 classification flag** |
| **has_saphah_keywords** | **BOOLEAN** | **Step 1 classification flag** |
| **contains_images** | **BOOLEAN** | **Cheap image detection (no geometry stored)** |
| **needs_geometry** | **BOOLEAN** | **Step 2 result: geometry extraction needed?** |
| **is_book_content** | **BOOLEAN** | **Step 2 result: is this book content?** |

#### page_images

Stores embedded images per page:

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| page_content_id | BIGINT | FK to page_contents |
| image_sequence | INT | 1, 2, 3... for multiple images |
| image_data | BLOB | Binary image data |
| mime_type | VARCHAR(50) | image/jpeg, image/png |
| linked_image_id | BIGINT | FK to images (after linking) |

#### workflow_states

Tracks multi-phase workflow execution:

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| workflow_name | VARCHAR(100) | Unique workflow identifier |
| current_phase | VARCHAR(50) | WorkflowPhase enum |
| status | VARCHAR(50) | WorkflowStatus enum |
| started_at | TIMESTAMP | Workflow start time |
| completed_at | TIMESTAMP | Workflow end time (if terminal) |
| statistics | TEXT | JSON statistics summary |
| last_error | TEXT | Last error encountered |

---

## Page Categories

Phase 7 categorizes all 1831 pages into 6 categories:

| Category | Pages | shouldIngest | Parser |
|----------|-------|-------------|--------|
| COVER | 1-3 | ❌ | N/A |
| TABLE_OF_CONTENTS | 4 | ❌ | N/A |
| IMAGE_LIST | 5-6 | ❌ | N/A |
| **OAHSPE_BOOKS** | **7-1668** | **✅** | **OahspeParser** |
| **GLOSSARIES** | **1668-1690** | **✅** | **GlossaryParser** |
| **INDEX** | **1691-1831** | **✅** | **IndexParser** |

**Total pages to ingest:** 1662 pages (OAHSPE_BOOKS + GLOSSARIES + INDEX)

---

## Classification Strategy

### Two-Step Classification (Lightweight & Efficient)

PageContent implements a two-step classification approach to determine which pages need expensive PDF geometry extraction:

#### Step 1: Cheap Extraction (Performed on All Pages)
Stored during initial PDF extraction from PDFBox:
- `textLength` - Length of extracted text
- `lineCount` - Number of text lines
- `verseCount` - Number of verses detected
- `hasFootnoteMarkers` - Boolean flag
- `hasIllustrationKeywords` - Boolean flag
- `hasSaphahKeywords` - Boolean flag
- `containsImages` - Cheap PDFBox image detection (no geometry data stored)

**Cost:** Negligible (text operations only)

#### Step 2: Classification Results
Determined by analyzer logic based on Step 1 metrics:
- `needsGeometry` - **Whether PDF geometry extraction is necessary** (default: false)
- `isBookContent` - Whether page is book content vs glossary/index

**Design Philosophy:** Geometry extraction is expensive (coordinates, layouts, spacing). Only perform it when necessary based on Step 1 analysis.

### When Geometry Is Extracted

Geometry extraction should ONLY occur for pages where `needsGeometry = true`, such as:
- Pages with complex layouts
- Pages with images requiring positioning
- Pages with special formatting (Saphah, tables)
- Pages where text-only extraction is insufficient

### Database Indexes

Classification results are indexed for efficient querying:
- `idx_needs_geometry` - Find pages requiring geometry work
- `idx_is_book_content` - Filter book content vs reference material

---

## Verification Gates

### Gate 1: Page Loading Complete

**Checks:**
- ✓ All 1831 pages loaded from PDF
- ✓ No extraction failures (error_message IS NULL)
- ✓ Each page has category assigned

**Failure Handling:**
- Logs missing page numbers
- Reports extraction error count
- Workflow terminates with exception

---

### Gate 2: Cleanup Complete

**Checks:**
- ✓ Books table empty
- ✓ Chapters table empty
- ✓ Verses table empty
- ✓ PageContent table **NOT** empty (preserved)

**Failure Handling:**
- Warns if old data still exists
- Warns if PageContent accidentally deleted
- Workflow terminates with exception

---

### Gate 3: Ingestion Complete

**Checks:**
- ✓ All pages with shouldIngest=true marked as ingested
- ✓ COVER/TOC/IMAGE_LIST pages ignored (not required to ingest)
- ✓ No ingestion errors

**Failure Handling:**
- Lists unprocessed pages
- Reports ingestion error count
- Workflow terminates with exception

---

## Troubleshooting

### Issue: "Gate 1 FAILED: Expected 1831 pages, found 1500"

**Cause:** PDF extraction failed partway through

**Solutions:**
1. Check PDF file integrity: `file data/OAHSPE.pdf`
2. Verify disk space available
3. Check logs for PDFBox errors
4. Re-run: `--load-pages data/OAHSPE.pdf`

---

### Issue: "Gate 2 FAILED: PageContent corrupted"

**Cause:** Cleanup accidentally deleted PageContent

**Solutions:**
1. **DO NOT** use manual SQL DELETE on page_contents
2. Always use `--cleanup` command (preserves PageContent)
3. If PageContent lost, re-run: `--load-pages data/OAHSPE.pdf`

---

### Issue: "Gate 3 FAILED: 100 pages still unprocessed"

**Cause:** Ingestion errors or parser failures

**Solutions:**
1. Query errors: `SELECT page_number, error_message FROM page_contents WHERE error_message IS NOT NULL`
2. Check logs for parser exceptions
3. Fix parser code and re-run: `--ingest-pages`

---

### Issue: "Compilation error in IngestionCliRunner"

**Cause:** Code corruption or merge conflict

**Solutions:**
1. Check for malformed switch-case statements
2. Verify all methods exist: runPageLoading, runIngestion, runVerification, runCleanup
3. Re-pull from git if needed

---

### Issue: Integration tests fail with "ApplicationContext failure threshold exceeded"

**Cause:** Main source code has compilation errors

**Solutions:**
1. Fix compilation errors in src/main/java first
2. Run: `mvn clean compile` to verify
3. Then run tests: `mvn test`

---

## Performance Expectations

### Phase 1: Page Loading

- **Duration:** 30-60 seconds
- **Bottleneck:** PDF I/O and image extraction
- **Memory:** ~500MB peak (PDF loaded in memory)
- **Disk:** ~50MB for PageContent + ~100MB for PageImages

### Phase 2: Cleanup

- **Duration:** 5-10 seconds
- **Bottleneck:** Foreign key cascade deletes
- **Memory:** ~100MB
- **Disk:** Frees up space from old entities

### Phase 3: Content Ingestion

- **Duration:** 2-5 minutes
- **Bottleneck:** Parser complexity and database I/O
- **Memory:** ~300MB
- **Disk:** ~200MB for Books/Chapters/Verses/Glossaries/Index

### Total Workflow

- **Duration:** ~3-6 minutes end-to-end
- **Memory:** ~500MB peak
- **Disk:** ~350MB total database size

---

## Best Practices

### 1. Always Use --workflow for Production

Individual phase commands are for debugging. Production runs should use:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--workflow data/OAHSPE.pdf"
```

---

### 2. Verify After Ingestion

Always run verification after ingestion completes:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--verify-links"
```

---

### 3. Keep PageContent Permanent

**NEVER** manually delete PageContent:
- ✓ Use `--cleanup` to delete Books/Chapters/Verses
- ✗ **DO NOT** `DELETE FROM page_contents`

PageContent is the source of truth for re-ingestion.

---

### 4. Monitor Logs

Key log patterns to watch:

- `✓ Gate X PASSED` - Verification success
- `✗ Gate X FAILED` - Verification failure (workflow stops)
- `Failed to ingest page X` - Parser error (workflow continues)
- `PageContent corrupted` - Critical error (PageContent deleted accidentally)

---

### 5. Database Backup Before Cleanup

Before running `--cleanup`, backup your database:

```bash
# H2 file-based database
cp -r data/oahspe-db.mv.db data/oahspe-db.backup.mv.db
```

---

## FAQ

### Q: Can I re-ingest without re-loading pages?

**A:** Yes! That's the main benefit of Phase 7:

```bash
# First time: load + ingest
mvn spring-boot:run -Dspring-boot.run.arguments="--workflow data/OAHSPE.pdf"

# Later: re-ingest only (faster, uses saved PageContent)
mvn spring-boot:run -Dspring-boot.run.arguments="--cleanup"
mvn spring-boot:run -Dspring-boot.run.arguments="--ingest-pages"
```

---

### Q: What happens if ingestion fails halfway?

**A:** Workflow stops at the verification gate. You can:

1. Check error logs
2. Fix parser code
3. Re-run `--ingest-pages` (will skip already-ingested pages)

---

### Q: How do I know which pages failed?

**A:** Query the database:

```sql
SELECT page_number, category, error_message 
FROM page_contents 
WHERE error_message IS NOT NULL
ORDER BY page_number;
```

---

### Q: Can I ingest specific page ranges?

**A:** Not directly via CLI. You can:

1. Query PageContent manually
2. Call PageIngestionLinker.ingestSinglePageContent() in code
3. Or wait for Phase 8 REST API for selective ingestion

---

### Q: How are glossary terms linked to index entries?

**A:** During ingestion:

1. GlossaryParser creates GlossaryTerm entities
2. IndexParser creates IndexEntry entities
3. If IndexEntry.topic matches GlossaryTerm.term, sets IndexEntry.glossaryTermId
4. Use ContentPageLinkingService to verify linking

---

## Next Steps

After successful Phase 7 ingestion:

1. **Verify Data Quality:**
   - Run `--verify-links` to check pageNumber population
   - Query database for orphaned verses: `SELECT * FROM verses WHERE page_number IS NULL`
   - Check glossary count: `SELECT COUNT(*) FROM glossary_terms` (expect ~500 terms)
   - Check index count: `SELECT COUNT(*) FROM index_entries` (expect ~5000 entries)

2. **Prepare for Phase 8 (Translation):**
   - Review glossary terms for translation consistency
   - Validate index cross-references
   - Plan translation workflow (separate from ingestion)

3. **Monitor Production:**
   - Set up database backups
   - Monitor disk usage growth
   - Track ingestion duration trends

---

## Support

For issues or questions:

1. Check logs in `target/logs/` or console output
2. Review this guide's Troubleshooting section
3. Consult `PHASE7_COMPLETION_REPORT.md` for known issues
4. Check Phase 7 test files for usage examples

**Last Updated:** January 31, 2026  
**Version:** 1.0  
**Phase:** 7 - Page-Based Ingestion Workflow
