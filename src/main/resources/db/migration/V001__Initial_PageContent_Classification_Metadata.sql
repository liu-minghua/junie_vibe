-- Migration: V001__Initial_PageContent_Classification_Metadata
-- Date: 2026-02-02
-- Author: Junie Vibe Team
-- Description: Add lightweight classification metadata to page_contents table
--              to support two-step classification strategy for determining 
--              when expensive PDF geometry extraction is needed.

-- =====================================================================
-- MIGRATION SUMMARY
-- =====================================================================
-- 
-- Purpose:
--   Enhance PageContent entity with Step 1 and Step 2 classification fields
--   to implement a lightweight, two-step approach for identifying pages that
--   require expensive PDF geometry extraction.
--
-- Rationale:
--   - Geometry extraction (coordinates, layouts, spacing) is expensive
--   - Not all pages need full geometry analysis
--   - Cheap text-based metrics can predict when geometry is needed
--   - Default to NO geometry extraction (needsGeometry = false)
--   - Only extract geometry when Step 1 metrics indicate necessity
--
-- Design Philosophy:
--   Step 1: Cheap Extraction (Performed on ALL pages)
--     - Negligible performance cost (text operations only)
--     - Collected during initial PDF extraction
--     - Provides data for Step 2 classification
--
--   Step 2: Classification Results (Calculated once during loading)
--     - needsGeometry: Whether this page needs geometry extraction
--     - isBookContent: Whether this is book content vs reference material
--     - Used to route pages to appropriate processing pipelines
--
-- Impact:
--   - Minimal: Adds 10 optional columns (all nullable)
--   - No schema breaking changes
--   - Backward compatible with existing PageContent
--   - New indexes on classification results for efficient queries
--
-- =====================================================================

-- Add Step 1 classification metadata (cheap extraction results)
ALTER TABLE page_contents ADD COLUMN text_length INT;
ALTER TABLE page_contents ADD COLUMN line_count INT;
ALTER TABLE page_contents ADD COLUMN verse_count INT;
ALTER TABLE page_contents ADD COLUMN has_footnote_markers BOOLEAN;
ALTER TABLE page_contents ADD COLUMN has_illustration_keywords BOOLEAN;
ALTER TABLE page_contents ADD COLUMN has_saphah_keywords BOOLEAN;

-- Add cheap image detection (does NOT store geometry)
-- True if PDFBox detects images on this page
ALTER TABLE page_contents ADD COLUMN contains_images BOOLEAN;

-- Add Step 2 classification results (calculated from Step 1 metrics)
-- needsGeometry: Whether PDF geometry extraction is necessary (default: false)
-- This is the key field for optimizing extraction cost
ALTER TABLE page_contents ADD COLUMN needs_geometry BOOLEAN DEFAULT FALSE;

-- isBookContent: Whether page is book content vs glossary/index
ALTER TABLE page_contents ADD COLUMN is_book_content BOOLEAN;

-- =====================================================================
-- INDEXES for efficient classification-based queries
-- =====================================================================

-- Index on needs_geometry to quickly find pages requiring geometry work
CREATE INDEX idx_needs_geometry ON page_contents(needs_geometry);

-- Index on is_book_content to filter book content vs reference material
CREATE INDEX idx_is_book_content ON page_contents(is_book_content);

-- =====================================================================
-- MIGRATION NOTES
-- =====================================================================
--
-- Column Details:
--
-- Step 1 Metrics (from PDF extraction):
--   - text_length: Length of extracted raw text (INT, nullable)
--   - line_count: Number of text lines detected (INT, nullable)
--   - verse_count: Number of verses detected (INT, nullable)
--   - has_footnote_markers: Flag if footnote markers found (BOOLEAN, nullable)
--   - has_illustration_keywords: Flag if illustration keywords found (BOOLEAN, nullable)
--   - has_saphah_keywords: Flag if Saphah keywords found (BOOLEAN, nullable)
--   - contains_images: PDFBox image detection result (BOOLEAN, nullable)
--
-- Step 2 Results (calculated by analyzer):
--   - needs_geometry: Whether to perform expensive geometry extraction (BOOLEAN, default: FALSE)
--     * TRUE: Page with complex layouts, positioned images, special formatting
--     * FALSE: Simple text pages (Glossaries, Index, etc.)
--   - is_book_content: Whether page is book content (BOOLEAN, nullable)
--     * TRUE: Pages 7-1668 (OAHSPE_BOOKS)
--     * FALSE: Pages 1669-1831 (GLOSSARIES, INDEX)
--
-- Indexes:
--   - idx_needs_geometry: Supports queries filtering by needsGeometry status
--     * RATIONALE: Frequently used to identify pages needing geometry work
--     * USAGE: SELECT * FROM page_contents WHERE needs_geometry = TRUE
--   - idx_is_book_content: Supports queries filtering by content type
--     * RATIONALE: Supports page classification and routing
--     * USAGE: SELECT * FROM page_contents WHERE is_book_content = TRUE
--
-- =====================================================================
-- EXAMPLES OF TYPICAL QUERIES
-- =====================================================================
--
-- 1. Find all pages needing geometry extraction:
--    SELECT page_number, category FROM page_contents 
--    WHERE needs_geometry = TRUE;
--
-- 2. Find all book content pages:
--    SELECT page_number, category FROM page_contents 
--    WHERE is_book_content = TRUE 
--    ORDER BY page_number;
--
-- 3. Analyze classification results:
--    SELECT 
--      COUNT(*) as total_pages,
--      SUM(CASE WHEN needs_geometry = TRUE THEN 1 ELSE 0 END) as pages_needing_geometry,
--      SUM(CASE WHEN is_book_content = TRUE THEN 1 ELSE 0 END) as book_content_pages
--    FROM page_contents;
--
-- 4. Find pages with images but no geometry requirement:
--    SELECT page_number, category FROM page_contents 
--    WHERE contains_images = TRUE 
--    AND needs_geometry = FALSE;
--
-- =====================================================================
-- ROLLBACK STRATEGY
-- =====================================================================
--
-- If this migration needs to be rolled back:
--
--   1. Drop the indexes:
--      DROP INDEX IF EXISTS idx_needs_geometry;
--      DROP INDEX IF EXISTS idx_is_book_content;
--
--   2. Drop the columns:
--      ALTER TABLE page_contents DROP COLUMN needs_geometry;
--      ALTER TABLE page_contents DROP COLUMN is_book_content;
--      ALTER TABLE page_contents DROP COLUMN contains_images;
--      ALTER TABLE page_contents DROP COLUMN has_saphah_keywords;
--      ALTER TABLE page_contents DROP COLUMN has_illustration_keywords;
--      ALTER TABLE page_contents DROP COLUMN has_footnote_markers;
--      ALTER TABLE page_contents DROP COLUMN verse_count;
--      ALTER TABLE page_contents DROP COLUMN line_count;
--      ALTER TABLE page_contents DROP COLUMN text_length;
--
-- =====================================================================
