-- Database Verification Queries
-- Run these after successful ingestion to verify data integrity

-- =============================================================================
-- SUMMARY COUNTS
-- =============================================================================

-- Overall summary
SELECT 
  (SELECT COUNT(*) FROM books) as total_books,
  (SELECT COUNT(*) FROM chapters) as total_chapters,
  (SELECT COUNT(*) FROM verses) as total_verses,
  (SELECT COUNT(*) FROM notes) as total_notes,
  (SELECT COUNT(*) FROM images) as total_images;

-- =============================================================================
-- IMAGE VERIFICATION
-- =============================================================================

-- Image extraction summary
SELECT COUNT(*) as total_images, 
       MIN(source_page) as first_page, 
       MAX(source_page) as last_page,
       COUNT(DISTINCT source_page) as pages_with_images,
       SUM(LENGTH(data)) as total_bytes,
       AVG(LENGTH(data)) as avg_bytes_per_image
FROM images;

-- Images by page
SELECT source_page, 
       COUNT(*) as image_count,
       GROUP_CONCAT(image_key) as image_keys
FROM images
GROUP BY source_page
ORDER BY source_page;

-- Image details (first 20)
SELECT image_key, 
       source_page, 
       content_type, 
       LENGTH(data) as size_bytes,
       original_filename
FROM images
ORDER BY source_page, image_key
LIMIT 20;

-- Check for duplicate image keys
SELECT image_key, COUNT(*) as count
FROM images
GROUP BY image_key
HAVING COUNT(*) > 1;

-- Check for images without data
SELECT COUNT(*) as empty_images
FROM images
WHERE data IS NULL OR LENGTH(data) = 0;

-- =============================================================================
-- BOOK/CHAPTER/VERSE HIERARCHY
-- =============================================================================

-- Book summary
SELECT b.book_name, 
       b.book_number,
       COUNT(DISTINCT c.id) as chapter_count,
       COUNT(v.id) as verse_count
FROM books b
LEFT JOIN chapters c ON c.book_id = b.id
LEFT JOIN verses v ON v.chapter_id = c.id
GROUP BY b.id, b.book_name, b.book_number
ORDER BY b.book_number;

-- Chapters by book
SELECT b.book_name, 
       c.chapter_number, 
       c.chapter_name,
       COUNT(v.id) as verse_count
FROM books b
LEFT JOIN chapters c ON c.book_id = b.id
LEFT JOIN verses v ON v.chapter_id = c.id
GROUP BY b.id, b.book_name, c.id, c.chapter_number, c.chapter_name
ORDER BY b.book_number, c.chapter_number;

-- =============================================================================
-- DATA INTEGRITY CHECKS
-- =============================================================================

-- Orphaned verses (NULL chapter_id) - SHOULD BE 0
SELECT COUNT(*) as orphaned_verses
FROM verses
WHERE chapter_id IS NULL;

-- Orphaned chapters (NULL book_id) - SHOULD BE 0
SELECT COUNT(*) as orphaned_chapters
FROM chapters
WHERE book_id IS NULL;

-- Orphaned notes (NULL verse_id) - MAY BE > 0
SELECT COUNT(*) as orphaned_notes
FROM notes
WHERE verse_id IS NULL;

-- Verses with missing text
SELECT COUNT(*) as verses_without_text
FROM verses
WHERE text IS NULL OR TRIM(text) = '';

-- =============================================================================
-- CONTENT SAMPLES
-- =============================================================================

-- First 5 verses
SELECT b.book_name, c.chapter_number, v.verse_key, v.text
FROM verses v
JOIN chapters c ON v.chapter_id = c.id
JOIN books b ON c.book_id = b.id
ORDER BY b.book_number, c.chapter_number, v.verse_key
LIMIT 5;

-- Sample notes
SELECT v.verse_key, n.note_text
FROM notes n
LEFT JOIN verses v ON n.verse_id = v.id
LIMIT 5;

-- =============================================================================
-- PERFORMANCE METRICS
-- =============================================================================

-- Database size estimate
SELECT 
  'Books' as table_name, COUNT(*) as row_count FROM books
UNION ALL
SELECT 'Chapters', COUNT(*) FROM chapters
UNION ALL
SELECT 'Verses', COUNT(*) FROM verses
UNION ALL
SELECT 'Notes', COUNT(*) FROM notes
UNION ALL
SELECT 'Images', COUNT(*) FROM images;

-- Image storage analysis
SELECT 
  CASE 
    WHEN LENGTH(data) < 1024 THEN '< 1 KB'
    WHEN LENGTH(data) < 10240 THEN '1-10 KB'
    WHEN LENGTH(data) < 102400 THEN '10-100 KB'
    WHEN LENGTH(data) < 1048576 THEN '100 KB - 1 MB'
    ELSE '> 1 MB'
  END as size_range,
  COUNT(*) as image_count
FROM images
GROUP BY size_range
ORDER BY MIN(LENGTH(data));
