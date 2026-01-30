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

The ingestion system is built in 5 phases:

### **Phase 1: Core Parser Implementation** ⬅️ START HERE
Foundation layer that converts raw PDF text into structured events.
- OahspeParser state machine
- Event model definition
- Unit test suite (12 test cases)
- **Status:** Task list ready → Ready for implementation

### **Phase 2: Ingestion Service Layer**
Central coordinator that consumes parser events and builds entity graph.
- OahspeIngestionService implementation
- ImageNoteLinker verification
- Integration tests

### **Phase 3: Orchestration Layer**
Top-level orchestrator that drives end-to-end pipeline.
- OahspeIngestionRunner implementation
- PDF extraction and processing
- Image handling

### **Phase 4: Testing Infrastructure & Validation**
Test data builders and comprehensive validation.
- Test data builders (fluent API)
- Idempotency tests
- System tests

### **Phase 5: Configuration & Execution**
Configuration setup and final validation with actual PDF.
- Application properties
- Dependencies verification
- Full pipeline validation

## 📍 Current Status

### ✅ Completed
- All 5 entity classes (Book, Chapter, Verse, Note, Image)
- All 5 repository interfaces
- ImageNoteLinker component
- Complete architecture documentation
- Complete testing strategy
- Phase 1 detailed task list

### ⏳ In Progress / Ready to Start
- **Phase 1 Implementation:** OahspeParser (awaiting development)

### ❌ Not Started
- Phases 2-5

## 🚀 Getting Started

1. **Read the strategic overview:** [order_of_action.md](planning/order_of_action.md)
2. **Understand the architecture:** Start with [workflow.md](architecture/oahspe_ingestion_workflow.md)
3. **Begin Phase 1 implementation:** Follow [phase1_implementation_tasklist.md](planning/phase1_implementation_tasklist.md)
4. **Reference test requirements:** Consult [testplan.md](testing/oahspe_ingestion_testplan.md)

## 📚 Key Concepts

### Event Model
The parser emits strongly-typed events:
- `BookStart` - Book title detected
- `ChapterStart` - Chapter header detected
- `Verse` - Verse line detected (can be null key for continuation)
- `Note` - Note line detected (can be null key for continuation)
- `ImageRef` - Image reference detected
- `PageBreak` - Page transition marker

### Parser State Machine
```
OUTSIDE_BOOK → IN_BOOK → IN_CHAPTER → IN_VERSE
                              ↓
                           IN_NOTE
```

### Core Principles
1. **Deterministic**: Same PDF → Same database structure every time
2. **Idempotent**: Running twice = no duplicates (unique keys: verseKey, noteKey, imageKey)
3. **Modular**: Each component independently testable
4. **Traceable**: Every entity linked to source page number
5. **Restart-Safe**: Failed runs can be retried without corruption

## 📖 Document Descriptions

### Architecture Documents
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
  - 5-phase implementation roadmap
  - Current status (what's built vs. todo)
  - Phase dependencies
  - Success criteria
  - Critical path

- **phase1_implementation_tasklist.md** (500+ lines)
  - 23 detailed tasks
  - Acceptance criteria for each task
  - Time estimates
  - Test case specifications
  - Completion checklist

## 🔗 References

- Entity classes: `src/main/java/edu/minghualiu/oahspe/entities/`
- Repository interfaces: `src/main/java/edu/minghualiu/oahspe/repositories/`
- Existing ImageNoteLinker: `src/main/java/edu/minghualiu/oahspe/ingestion/ImageNoteLinker.java`

## 💡 Tips for Using This Documentation

1. **New developers:** Start with `order_of_action.md` for the big picture
2. **Architects:** Read `workflow.md` and all architecture docs for deep understanding
3. **Implementers:** Use `phase1_implementation_tasklist.md` as your checklist
4. **QA/Testers:** Reference `testplan.md` and `test_data.md` for test requirements
5. **Search:** Use Ctrl+F to find specific topics (regex patterns, test cases, etc.)

---

**Last Updated:** January 30, 2026  
**Status:** Documentation complete, ready for Phase 1 implementation
