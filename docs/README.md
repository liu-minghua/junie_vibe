# Oahspe PDF Ingestion Documentation

This folder contains comprehensive documentation for the Oahspe PDF ingestion system, organized by purpose and phase.

## 📋 Documentation Structure

### 🏗️ **Architecture & Design** (`/architecture/`)
Core architectural design and component documentation.

- **[workflow.md](architecture/oahspe_ingestion_workflow.md)** - Overall ingestion pipeline architecture, event model, parser state machine, and regex patterns
- **[ingestion_service.md](architecture/oahspe_ingestion_service.md)** - OahspeIngestionService component design, entity graph building, persistence strategy
- **[ingestion_runner.md](architecture/oahspe_ingestion_runner.md)** - OahspeIngestionRunner orchestration layer, PDF extraction, and image handling

### 🧪 **Testing** (`/testing/`)
Testing strategy, test plans, and test data utilities.

- **[testplan.md](testing/oahspe_ingestion_testplan.md)** - Comprehensive end-to-end test plan covering unit, integration, system, and idempotency tests
- **[test_data.md](testing/oahspe_ingestion_test_data.md)** - Test data builders and synthetic PDF generation utilities
- **[test_suite.md](testing/oahspe_ingestion_test_suite.md)** - Additional test suite documentation and utilities

### 📊 **Planning & Roadmap** (`/planning/`)
Strategic planning, implementation roadmaps, and task lists.

- **[order_of_action.md](planning/order_of_action.md)** - Best order of action to achieve PDF ingestion goals (5-phase strategic roadmap)
- **[phase1_implementation_tasklist.md](planning/phase1_implementation_tasklist.md)** - Detailed task breakdown for Phase 1: Core Parser Implementation

## 🔄 Implementation Phases

The Oahspe system is built in 8 phases:

### **Phase 1-6: Legacy PDF Ingestion** ✅ COMPLETED
Foundation layers that convert raw PDF text into structured database.
- ✅ Phase 1: Core Parser Implementation (OahspeParser state machine)
- ✅ Phase 2: Ingestion Service Layer (Entity graph building)
- ✅ Phase 3: Orchestration Layer (PDF extraction & processing)
- ✅ Phase 4: Testing Infrastructure (Test data builders, validation framework)
- ✅ Phase 5: Lombok Integration (DTOs, entity refactoring)
  - ⚠️ REST API removed in Phase 7 (not needed - validation is built into workflow)
- ✅ Phase 6: Manual Testing & Validation (Full pipeline verification)
- **Status:** Production-ready, 100% test coverage

### **Phase 7: Page-Based Ingestion Workflow** ✅ COMPLETED
Two-workflow architecture with page-level tracking and verification gates.
- ✅ 11 new entities (PageContent, GlossaryTerm, IndexEntry, WorkflowState, etc.)
- ✅ 4 enhanced entities (Book, Chapter, Verse, Note) with pageNumber tracking
- ✅ 9 repositories for Phase 7 entities
- ✅ 7 service implementations (PageLoader, PageIngestionLinker, WorkflowOrchestrator)
- ✅ 2 parser services (GlossaryParser, IndexParser)
- ✅ Enhanced CLI with 6 workflow commands (--workflow, --load-pages, --ingest-pages, etc.)
- ✅ 69 comprehensive tests (54 unit + 15 integration, 167/167 passing)
- ✅ 3 documentation guides (usage, completion report, retrospective)
- **Status:** Production-ready, ~5700 LOC, 3-6 min workflow for 1831 pages

### **Phase 8: TOC-Based Ingestion with PDF Foundation Layer** ⬅️ **ACTIVE**
Intelligent book-by-book ingestion using Table of Contents + Geometry-Aware TextFragments.

**NEW in Phase 8:** Fixes critical two-column PDF layout issue
- ✅ Phase 0.5: PDF Foundation Layer (extract TextFragments with geometry)
- ⏳ Phase 1-6: TOC-Based Workflow (as documented)
- **Status:** Design Complete, Integration Complete - Ready for Implementation
- **Key Files:**
  - [PHASE8_TOC_BASED_INGESTION_DESIGN.md](PHASE8_TOC_BASED_INGESTION_DESIGN.md) - Complete architecture
  - [PHASE8_PDF_FOUNDATION_INTEGRATION.md](PHASE8_PDF_FOUNDATION_INTEGRATION.md) - PDF Foundation Layer integration
  - [PHASE8_IMPLEMENTATION_CHECKLIST.md](PHASE8_IMPLEMENTATION_CHECKLIST.md) - Step-by-step tasks
  - [PHASE8_QUICK_REFERENCE.md](PHASE8_QUICK_REFERENCE.md) - Developer cheat sheet

**Critical Fix:** Previous rawText concatenation approach fails on two-column layout. Phase 0.5 extracts geometry-aware TextFragments (x, y, width, height, font properties) to correctly handle:
- Verses broken across left/right columns
- Verses continued across pages
- Footnotes interleaved at column bottom
- Perfect reading order reconstruction

## 🚀 Getting Started

### For Phase 8 Development (CURRENT)
1. **Read Phase 8 Foundation Layer:** [PHASE8_PDF_FOUNDATION_INTEGRATION.md](PHASE8_PDF_FOUNDATION_INTEGRATION.md)
2. **Review complete architecture:** [PHASE8_TOC_BASED_INGESTION_DESIGN.md](PHASE8_TOC_BASED_INGESTION_DESIGN.md)
3. **Check implementation tasks:** [PHASE8_IMPLEMENTATION_CHECKLIST.md](PHASE8_IMPLEMENTATION_CHECKLIST.md)
4. **Use quick reference:** [PHASE8_QUICK_REFERENCE.md](PHASE8_QUICK_REFERENCE.md)

### Phase 8 Workflow (Overview)
```bash
# Phase 0.5: Extract TextFragments with geometry (REQUIRED)
java -jar oahspe.jar --extract-geometry
# Creates ~100K TextFragments with x,y,font properties

# Phase 1: Extract TOC
java -jar oahspe.jar --toc-extract

# Phase 2: Register books
java -jar oahspe.jar --toc-register

# Phase 3: Assign pages
java -jar oahspe.jar --assign-pages

# Phase 4: Parse with TextFragments (NOT rawText)
java -jar oahspe.jar --parse-book 1      # Test single book
java -jar oahspe.jar --parse-all-books   # Parse all 38 books

# Phase 5: Aggregate
java -jar oahspe.jar --aggregate-all

# Phase 6: Verify
java -jar oahspe.jar --verify
```

### For Phase 7 Operations (Reference)
1. **Load pages from PDF:**
   ```bash
   java -jar oahspe.jar --load-pages /path/to/oahspe.pdf
   ```
2. **Run full workflow:**
   ```bash
   java -jar oahspe.jar --workflow /path/to/oahspe.pdf
   ```
3. **Verify verification gates passed** (3 gates: page loading, cleanup, ingestion)
4. **Check database:** ~500 glossary terms, ~5000 index entries, 1831 pages loaded

## 📚 Key Concepts

### Phase 8: TOC-Based Ingestion Architecture

#### Three-Layer Architecture
1. **Structural Discovery**
   - Extract book metadata from Table of Contents (page 4)
   - Identify book boundaries and page ranges
   - Single source of truth for book structure

2. **Content Organization**
   - Group pages by book
   - Validate page continuity
   - Assign pages to books for tracking

3. **Intelligent Processing**
   - Parse chapters, verses, notes with complete context
   - Use geometry-aware TextFragments (x, y, font properties)
   - Calculate aggregates (counts, combined text)
   - Verify at each step

#### PDF Foundation Layer (Phase 0.5)
Critical new layer that fixes two-column PDF parsing:
```
rawText (BROKEN):
  Verses split across columns
  Footnotes at wrong position
  Reading order destroyed
  
TextFragments (FIXED):
  x, y, width, height coordinates
  Font properties (size, bold, italic)
  Column detection by spatial clustering
  Reading order: left-column→right-column, top→bottom
  Footnote identification by y-coordinate
  
Result: Perfect extraction despite layout complexity
```
- **Gate 2:** Cleanup validation (prevents accidental data loss)
- **Gate 3:** Ingestion completeness (requires all required categories processed)

#### Page Categories
| Category | Pages | Required | Contains |
|----------|-------|----------|----------|
| COVER | 1-3 | No | Cover pages |
| TABLE_OF_CONTENTS | 4 | No | Book/chapter listing |
| IMAGE_LIST | 5-6 | No | List of images |
| OAHSPE_BOOKS | 7-1668 | Yes | Books, chapters, verses, notes (structured content) |
| GLOSSARIES | 1668-1690 | Yes | ~500 glossary terms (page 1668: below horizontal line) |
| INDEX | 1691-1831 | Yes | ~5000 index entries |

### Phase 8: Translation Workflow (Coming Soon)

#### Translation Pipeline
1. **Glossary First:** Translate ~500 glossary terms (establishes terminology)
2. **Titles:** Translate book/chapter titles (structural elements)
3. **Verses:** Translate verse content (main body)
4. **Notes:** Translate notes and annotations
5. **QA:** Model comparison and consistency validation

#### Translation Models
- **Claude Sonnet 3.5:** Primary translation engine
- **GPT-4:** Comparison and validation
- **Translation Memory:** Consistency enforcement for glossary terms

### Legacy Concepts (Phase 1-6)

#### Event Model
The parser emits strongly-typed events:
- `BookStart` - Book title detected
- `ChapterStart` - Chapter header detected
- `Verse` - Verse line detected (can be null key for continuation)
- `Note` - Note line detected (can be null key for continuation)
- `ImageRef` - Image reference detected
- `PageBreak` - Page transition marker

#### Parser State Machine
```
OUTSIDE_BOOK → IN_BOOK → IN_CHAPTER → IN_VERSE
                              ↓
                           IN_NOTE
```

#### Core Principles
1. **Deterministic**: Same PDF → Same database structure every time
2. **Idempotent**: Running twice = no duplicates (unique keys: verseKey, noteKey, imageKey)
3. **Modular**: Each component independently testable
4. **Traceable**: Every entity linked to source page number
5. **Restart-Safe**: Failed runs can be retried without corruption

## 📖 Document Descriptions

### Phase 7 Documents (Current)
- **PHASE7_USAGE_GUIDE.md** (~1500 lines)
  - Complete operator manual for Phase 7 workflow
  - 6 CLI commands with examples
  - Database schema documentation
  - Page category breakdown
  - 3 verification gates with failure handling
  - Troubleshooting guide (5 common issues)
  - Performance expectations (3-6 min workflow)
  - Best practices and FAQ

- **PHASE7_COMPLETION_REPORT.md** (~1500 lines)
  - Comprehensive implementation summary
  - Implementation statistics (39 files, ~5700 LOC)
  - Test results (167/167 passing, 100%)
  - Database schema (5 new tables, 4 enhanced)
  - Performance metrics
  - Known issues (4 limitations)
  - Lessons learned (5 key insights)
  - Phase 8 readiness assessment

- **PHASE7_RETROSPECTIVE.md** (~1400 lines)
  - What went well (5 successes)
  - What could be improved (5 areas)
  - Challenges encountered (5 issues)
  - Key learnings (5 lessons)
  - Recommendations for Phase 8 (8 action items)
  - Action items summary

- **PHASE7_IMPLEMENTATION_PLAN.md** (~800 lines)
  - Task breakdown (8 task groups)
  - Implementation tracking
  - Completion status for all 39 files
  - Time estimates vs actuals

### Phase 6 Documents
- **PHASE6_COMPLETE_SUMMARY.md**
  - Manual testing results
  - Full pipeline validation
  - Production readiness assessment

- **PHASE6_MANUAL_TEST_RESULTS.md**
  - Detailed test execution results
  - Database verification queries
  - Data integrity validation

### Legacy Architecture Documents
- **workflow.md** (256 lines)
  - Ingestion pipeline architecture
  - Event model definition
  - Parser state machine
  - Regex patterns (English & Chinese support)
  - ImageNoteLinker helper
  - Complete flow diagram

- **ingestion_service.md** (293 lines)
  - Service layer responsibilities
  - Entity construction logic
  - Persistence strategy
  - Idempotency implementation
  - Integration patterns

- **ingestion_runner.md** (450+ lines)
  - Runner orchestration
  - PDF loading and extraction
  - Image extraction logic
  - Image key generation
  - Configuration examples
  - Error handling strategy

### Testing Documents
- **testplan.md** (367 lines)
  - Test scope and categories
  - Unit test cases (P1-P6)
  - Integration test cases (I1-I3)
  - System test scenarios
  - Data integrity tests
  - Idempotency and restart tests

- **test_data.md** (365 lines)
  - Test data builder classes
  - Fluent builder API
  - Synthetic PDF generation
  - Example test data

- **test_suite.md**
  - Supplementary test utilities

### Planning Documents
- **order_of_action.md** (340 lines)
  - Historical 5-phase implementation roadmap
  - Phase dependencies
  - Success criteria
  - Critical path

- **phase1_implementation_tasklist.md** through **phase7_implementation_tasklist.md**
  - Detailed task breakdowns for each phase
  - Acceptance criteria
  - Time estimates
  - Completion checklists

## 🔗 References

- Entity classes: `src/main/java/edu/minghualiu/oahspe/entities/`
- Repository interfaces: `src/main/java/edu/minghualiu/oahspe/repositories/`
- Existing ImageNoteLinker: `src/main/java/edu/minghualiu/oahspe/ingestion/ImageNoteLinker.java`

## 💡 Tips for Using This Documentation

1. **New developers:** Start with PHASE7_COMPLETION_REPORT.md for current state overview
2. **Operators:** Use PHASE7_USAGE_GUIDE.md for workflow commands and troubleshooting
3. **Architects:** Read PHASE7_RETROSPECTIVE.md for lessons learned and Phase 8 recommendations
4. **Phase 8 developers:** Review Phase 7 retrospective action items before starting
5. **Legacy system:** Reference architecture docs (workflow.md, ingestion_service.md) for historical context
6. **Search:** Use Ctrl+F to find specific topics (page categories, verification gates, etc.)

## 📊 Project Metrics

### Phase 7 Deliverables
- **Code:** 39 files, ~5700 lines of code
- **Tests:** 69 tests (54 unit + 15 integration)
- **Test Results:** 167/167 passing (100% success rate)
- **Documentation:** 3 comprehensive guides (~4400 lines)
- **Database:** 5 new tables, 4 enhanced tables
- **Performance:** 3-6 minutes for full workflow (1831 pages)
- **Data Extraction:** ~500 glossary terms, ~5000 index entries
- **Development Time:** ~27.5 hours (planned), ~29.5 hours (actual)

### Overall System Status
- **Total Phases Completed:** 7 of 8
- **System Coverage:** 100% test coverage for implemented phases
- **Production Readiness:** Phase 1-7 ready for production
- **Next Milestone:** Phase 8 Translation Workflow

---

**Last Updated:** January 31, 2026  
**Current Status:** Phase 7 complete, Phase 8 ready to start  
**Git Branch:** phase7-page-based-ingestion (pushed to origin)  
**Pull Request:** Ready to create at https://github.com/liu-minghua/junie_vibe/pull/new/phase7-page-based-ingestion
