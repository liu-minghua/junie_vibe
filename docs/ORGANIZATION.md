# Documentation Organization Map

This file shows the recommended folder structure for organizing Oahspe ingestion documentation.

## Recommended Directory Structure

```
docs/
├── README.md                                     (Documentation index & overview)
│
├── architecture/                                 (Core design documents)
│   ├── oahspe_ingestion_workflow.md             (Event model, state machine, regex)
│   ├── oahspe_ingestion_service.md              (Service layer design)
│   └── oahspe_ingestion_runner.md               (Runner orchestration)
│
├── testing/                                      (Testing strategy & test data)
│   ├── oahspe_ingestion_testplan.md             (Complete test plan)
│   ├── oahspe_ingestion_test_data.md            (Test builders & synthetic data)
│   └── oahspe_ingestion_test_suite.md           (Additional test utilities)
│
└── planning/                                     (Strategic planning & implementation)
    ├── order_of_action.md                       (5-phase roadmap)
    ├── phase1_implementation_tasklist.md        (Phase 1 detailed tasks - 23 items)
    ├── phase2_implementation_tasklist.md        (Phase 2 - TO BE CREATED)
    ├── phase3_implementation_tasklist.md        (Phase 3 - TO BE CREATED)
    ├── phase4_implementation_tasklist.md        (Phase 4 - TO BE CREATED)
    └── phase5_implementation_tasklist.md        (Phase 5 - TO BE CREATED)
```

## File Organization Notes

### Current State
**Files Already in Root (`docs/`):**
- `oahspe_ingestion_runner.md` — belongs in `architecture/`
- `oahspe_ingestion_service.md` — belongs in `architecture/`
- `oahspe_ingestion_test_data.md` — belongs in `testing/`
- `oahspe_ingestion_test_suite.md` — belongs in `testing/`
- `oahspe_ingestion_testplan.md` — belongs in `testing/`
- `oahspe_ingestion_workflow.md` — belongs in `architecture/`
- `order_of_action.md` — belongs in `planning/`
- `README.md` — NEW, stays in root (index)

### Migration Plan
Since we can't move files directly, here's the recommended approach:

1. **New files created in subdirectories:**
   - ✅ `phase1_implementation_tasklist.md` created in `planning/`
   - ✅ `README.md` created in root (with links to all docs)

2. **Next: Rename/move existing files** (manual step):
   ```bash
   # Create subdirectories
   mkdir docs/architecture
   mkdir docs/testing
   mkdir docs/planning
   
   # Move files
   mv docs/oahspe_ingestion_runner.md docs/architecture/
   mv docs/oahspe_ingestion_service.md docs/architecture/
   mv docs/oahspe_ingestion_workflow.md docs/architecture/
   
   mv docs/oahspe_ingestion_testplan.md docs/testing/
   mv docs/oahspe_ingestion_test_data.md docs/testing/
   mv docs/oahspe_ingestion_test_suite.md docs/testing/
   
   mv docs/order_of_action.md docs/planning/
   ```

3. **Update internal links** in README.md and all markdown files to reflect new structure

## Documentation Access

### By Role

**For New Developers:**
1. Start: [README.md](README.md) - Overview
2. Read: [order_of_action.md](planning/order_of_action.md) - Strategic roadmap
3. Study: [oahspe_ingestion_workflow.md](architecture/oahspe_ingestion_workflow.md) - Architecture

**For Implementers:**
1. Read: Phase-specific task list (e.g., [phase1_implementation_tasklist.md](planning/phase1_implementation_tasklist.md))
2. Implement: Task by task
3. Reference: Architecture docs as needed

**For QA/Testers:**
1. Read: [oahspe_ingestion_testplan.md](testing/oahspe_ingestion_testplan.md)
2. Use: [oahspe_ingestion_test_data.md](testing/oahspe_ingestion_test_data.md) for test builders
3. Follow: Phase task lists for test schedules

**For Architects:**
1. Deep dive: All architecture documents
2. Reference: [order_of_action.md](planning/order_of_action.md) for integration points
3. Design: Phases 2+ as needed

### By Document Type

**Architecture & Design (3 files)**
- [oahspe_ingestion_workflow.md](architecture/oahspe_ingestion_workflow.md) — 256 lines
- [oahspe_ingestion_service.md](architecture/oahspe_ingestion_service.md) — 293 lines
- [oahspe_ingestion_runner.md](architecture/oahspe_ingestion_runner.md) — 450+ lines

**Testing (3 files)**
- [oahspe_ingestion_testplan.md](testing/oahspe_ingestion_testplan.md) — 367 lines
- [oahspe_ingestion_test_data.md](testing/oahspe_ingestion_test_data.md) — 365 lines
- [oahspe_ingestion_test_suite.md](testing/oahspe_ingestion_test_suite.md) — TBD

**Planning (2 files created)**
- [order_of_action.md](planning/order_of_action.md) — 340 lines
- [phase1_implementation_tasklist.md](planning/phase1_implementation_tasklist.md) — 500+ lines

## Navigation Tips

### Find Test Cases
**Location:** [oahspe_ingestion_testplan.md](testing/oahspe_ingestion_testplan.md)
- Unit tests: Section 3 (P1-P6)
- Integration tests: Section 4 (I1-I3)
- System tests: Section 5
- Search: `Test Case` or `###`

### Find Regex Patterns
**Location:** [oahspe_ingestion_workflow.md](architecture/oahspe_ingestion_workflow.md)
- Section: "4. Regex Patterns"
- All 5 patterns with examples
- Supports English & Chinese

### Find Task Lists
**Location:** [planning/](planning/)
- Phase 1: [phase1_implementation_tasklist.md](planning/phase1_implementation_tasklist.md)
- Future: phase2, phase3, phase4, phase5

### Find Architecture Details
**Location:** [architecture/](architecture/)
- Workflow: [oahspe_ingestion_workflow.md](architecture/oahspe_ingestion_workflow.md)
- Service: [oahspe_ingestion_service.md](architecture/oahspe_ingestion_service.md)
- Runner: [oahspe_ingestion_runner.md](architecture/oahspe_ingestion_runner.md)

## Document Cross-References

### workflow.md → linked to:
- testplan.md (test cases based on workflow)
- ingestion_service.md (service consumes parser events)
- phase1_implementation_tasklist.md (regex patterns from workflow)

### ingestion_service.md → linked to:
- workflow.md (event model)
- test_data.md (entity builders)
- testplan.md (integration tests)

### ingestion_runner.md → linked to:
- workflow.md (calls parser)
- test_data.md (PDF generation)
- testplan.md (system tests)

### testplan.md → linked to:
- workflow.md (test cases match workflow design)
- test_data.md (test data builders)
- phase1_implementation_tasklist.md (parser tests)

### order_of_action.md → linked to:
- All architecture docs (explains how they fit)
- All phase task lists

### phase1_implementation_tasklist.md → linked to:
- workflow.md (regex patterns section)
- testplan.md (test cases P1-P6)

---

## Key Metrics

**Total Documentation:**
- Pages: 7 markdown files + README
- Lines: ~2800+ lines of detailed documentation
- Sections: 50+ major sections
- Test Cases: 20+ explicit test cases
- Tasks: 23 detailed Phase 1 tasks (with more in phases 2-5)

**Coverage:**
- ✅ Architecture: 3 documents covering all layers
- ✅ Testing: Complete test plan + test data
- ✅ Planning: Strategic roadmap + Phase 1 detailed tasks
- ❌ Implementation: Phase 1 code awaiting development

---

**Last Updated:** January 30, 2026  
**Status:** Documentation organization complete, ready for Phase 1 implementation
