# Flyway Migration V001 Quick Reference

**File:** `src/main/resources/db/migration/V001__Initial_PageContent_Classification_Metadata.sql`  
**Applies To:** `page_contents` table  
**Type:** Additive (schema expansion)  
**Risk:** LOW  
**Size:** 10 new columns + 2 indexes  
**Estimated Time:** <5 seconds

---

## What Changed

### New Columns (10 total)

#### Step 1: Cheap Classification Metrics (7 columns)
Populated during PDF extraction from PDFBox:

| Column | Type | Null | Description |
|--------|------|------|-------------|
| `text_length` | INT | YES | Length of extracted text |
| `line_count` | INT | YES | Number of text lines |
| `verse_count` | INT | YES | Number of verses detected |
| `has_footnote_markers` | BOOLEAN | YES | Footnotes present? |
| `has_illustration_keywords` | BOOLEAN | YES | Illustration keywords present? |
| `has_saphah_keywords` | BOOLEAN | YES | Saphah keywords present? |
| `contains_images` | BOOLEAN | YES | Images detected? |

#### Step 2: Classification Results (3 columns)
Calculated by analyzer based on Step 1:

| Column | Type | Null | Default | Description |
|--------|------|------|---------|-------------|
| `needs_geometry` | BOOLEAN | NO | FALSE | Geometry extraction needed? |
| `is_book_content` | BOOLEAN | YES | - | Book content vs reference? |

### New Indexes (2 total)

| Index | Column | Purpose |
|-------|--------|---------|
| `idx_needs_geometry` | `needs_geometry` | Find pages needing geometry work |
| `idx_is_book_content` | `is_book_content` | Filter by content type |

---

## Migration Strategy

### Before Running Migration

1. **Backup database**
   ```bash
   cp data/oahspe-db.mv.db data/oahspe-db.backup-v000.mv.db
   ```

2. **Verify clean state**
   ```bash
   mvn clean compile
   ```

### Running Migration

Migrations run automatically on application startup:

```bash
mvn spring-boot:run
```

**OR** explicitly with Flyway:

```bash
mvn flyway:migrate
```

### After Migration

1. **Verify columns exist**
   ```sql
   DESCRIBE page_contents;
   -- Should show 9 new columns
   ```

2. **Verify indexes exist**
   ```sql
   SHOW INDEXES FROM page_contents;
   -- Should show idx_needs_geometry and idx_is_book_content
   ```

3. **Load pages**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--load-pages data/OAHSPE.pdf"
   ```

4. **Verify population**
   ```sql
   SELECT COUNT(*), COUNT(text_length), COUNT(needs_geometry) 
   FROM page_contents;
   -- Should show: 1831, 1831, 1831
   ```

---

## Why These Changes

### Design Principle

**"Geometry is expensive, only do it as necessary."**

Problem: PDF geometry extraction is slow but not needed for all pages
Solution: Classify pages using cheap metrics, only extract geometry when needed

### Step 1: Cheap Extraction

All 1831 pages get simple text analysis during PDF loading:
- Text length, line count, verse count
- Keyword detection (footnotes, illustrations, Saphah)
- Image presence (cheap detection only, no geometry)

**Cost:** Negligible (~text operations only)

### Step 2: Classification

After Step 1 extraction, decide if geometry is needed:
- Pages with images → maybe geometry needed
- Pages with Saphah → yes, geometry needed
- Pages with complex formatting → maybe geometry needed
- Simple book pages → no geometry needed (default)

**Cost:** Zero (just boolean evaluation)

### Result

- **Without geometry extraction:** Fast loading (~30-60 seconds for all 1831 pages)
- **With selective geometry:** Can extract for only 300-400 pages when truly needed
- **Total benefit:** 5-10x faster processing when geometry not needed

---

## Key Design Decisions

### 1. Default needs_geometry = FALSE

```sql
ALTER TABLE page_contents ADD COLUMN needs_geometry BOOLEAN DEFAULT FALSE;
```

**Rationale:** Conservative approach
- Safer to skip geometry and request it later
- Easier to add geometry for specific pages
- Harder to know upfront which pages need geometry

### 2. All Step 1 Columns Nullable

```sql
ALTER TABLE page_contents ADD COLUMN text_length INT;  -- NULL if not populated
```

**Rationale:** Backward compatibility
- Existing PageContent rows can have NULL
- Optional: re-populate on next load
- Gracefully degrades if analyzer unavailable

### 3. Separate Step 1 and Step 2

```sql
-- Step 1: Raw metrics
ALTER TABLE page_contents ADD COLUMN text_length INT;
ALTER TABLE page_contents ADD COLUMN line_count INT;

-- Step 2: Decisions based on metrics
ALTER TABLE page_contents ADD COLUMN needs_geometry BOOLEAN DEFAULT FALSE;
```

**Rationale:** Clear separation of concerns
- Step 1: Extraction logic (in PDFBox integration)
- Step 2: Classification logic (in Analyzer)
- Easy to modify classification rules without re-extracting

---

## Testing After Migration

### Quick Sanity Check

```bash
# 1. Start application
mvn spring-boot:run

# 2. Load pages
# (in another terminal)
mvn spring-boot:run -Dspring-boot.run.arguments="--load-pages data/OAHSPE.pdf"

# 3. Verify in SQL
SELECT 
  COUNT(*) as total_pages,
  COUNT(text_length) as with_text_length,
  COUNT(needs_geometry) as with_needs_geometry,
  SUM(CASE WHEN needs_geometry = TRUE THEN 1 ELSE 0 END) as pages_needing_geometry
FROM page_contents;

-- Expected output:
-- total_pages: 1831
-- with_text_length: 1831
-- with_needs_geometry: 1831
-- pages_needing_geometry: 300-400
```

### Detailed Validation

```sql
-- 1. Verify all columns exist
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'page_contents'
AND column_name IN (
  'text_length', 'line_count', 'verse_count',
  'has_footnote_markers', 'has_illustration_keywords', 'has_saphah_keywords',
  'contains_images', 'needs_geometry', 'is_book_content'
);
-- Expected: 9 rows

-- 2. Verify indexes exist
SELECT index_name 
FROM information_schema.statistics 
WHERE table_name = 'page_contents'
AND index_name IN ('idx_needs_geometry', 'idx_is_book_content');
-- Expected: 2 rows

-- 3. Verify sample data
SELECT 
  page_number, category, 
  text_length, line_count, verse_count,
  contains_images, needs_geometry, is_book_content
FROM page_contents 
WHERE page_number IN (1, 4, 7, 1000, 1668, 1691, 1831)
ORDER BY page_number;

-- Expected: Different classification for different page types
-- Pages 1-3: COVER, simple, needs_geometry=FALSE
-- Page 4: TOC, simple, needs_geometry=FALSE  
-- Pages 7-1668: OAHSPE_BOOKS, mixed, needs_geometry=TRUE/FALSE
-- Pages 1668-1690: GLOSSARIES, simple, needs_geometry=FALSE
-- Pages 1691-1831: INDEX, simple, needs_geometry=FALSE
```

---

## Rollback (If Needed)

### Quick Rollback

If something goes wrong, Flyway can't automatically undo, but you can:

1. **Restore from backup**
   ```bash
   cp data/oahspe-db.backup-v000.mv.db data/oahspe-db.mv.db
   ```

2. **Or manually undo** (NOT recommended, use backup instead)
   ```sql
   -- Drop indexes
   DROP INDEX idx_needs_geometry;
   DROP INDEX idx_is_book_content;
   
   -- Drop columns
   ALTER TABLE page_contents DROP COLUMN needs_geometry;
   ALTER TABLE page_contents DROP COLUMN is_book_content;
   ALTER TABLE page_contents DROP COLUMN contains_images;
   ALTER TABLE page_contents DROP COLUMN has_saphah_keywords;
   ALTER TABLE page_contents DROP COLUMN has_illustration_keywords;
   ALTER TABLE page_contents DROP COLUMN has_footnote_markers;
   ALTER TABLE page_contents DROP COLUMN verse_count;
   ALTER TABLE page_contents DROP COLUMN line_count;
   ALTER TABLE page_contents DROP COLUMN text_length;
   ```

**Recommendation:** Always use backup restore instead of manual undo

---

## Monitoring

### After First Deployment

Check database health:

```sql
-- 1. Verify row count unchanged
SELECT COUNT(*) FROM page_contents;

-- 2. Check for data corruption
SELECT COUNT(*) FROM page_contents 
WHERE page_number IS NULL OR category IS NULL;
-- Should be 0

-- 3. Verify new columns not interfering
SELECT * FROM page_contents LIMIT 1;
-- Should select all columns including new ones
```

### Performance Check

```sql
-- 1. Verify index usage
SELECT 
  'idx_needs_geometry' as index_name,
  COUNT(*) as pages_needing_geometry
FROM page_contents 
WHERE needs_geometry = TRUE;

-- 2. Check for slow queries
-- (Run classification query, should be <10ms)
SELECT COUNT(*) FROM page_contents 
WHERE needs_geometry = TRUE;
-- Expected execution time: <10ms
-- Without index: 50-100ms
```

---

## Related Documentation

- **Entity Definition:** [PageContent.java](../src/main/java/edu/minghualiu/oahspe/entities/PageContent.java)
- **Usage Guide:** [Phase 7 Usage Guide - Classification Strategy](../docs/PHASE7_USAGE_GUIDE.md#classification-strategy)
- **Full Documentation:** [DATABASE_MIGRATIONS.md](../docs/DATABASE_MIGRATIONS.md)

---

## Contact & Support

Questions about this migration?

1. Check [DATABASE_MIGRATIONS.md](../docs/DATABASE_MIGRATIONS.md) for detailed info
2. Review migration SQL: `V001__Initial_PageContent_Classification_Metadata.sql`
3. Consult [PageContent.java](../src/main/java/edu/minghualiu/oahspe/entities/PageContent.java) javadoc
4. Check git log for implementation details

---

**Last Updated:** February 2, 2026  
**Status:** Ready for Production Deployment
