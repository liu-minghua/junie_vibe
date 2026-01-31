# Phase 6.4: Testing & Validation

## Objective
End-to-end validation and production readiness.

## Tasks

- [ ] **Task 6.4.1:** Create integration test
  - [ ] Create test PDF with embedded images (or use existing)
  - [ ] Write `OahspeIngestionRunnerImageIT.java`
  - [ ] Test full pipeline extracts both text and images
  - [ ] Verify `IngestionContext.totalImagesExtracted` is populated

- [ ] **Task 6.4.2:** Test with real Oahspe PDF
  - [ ] Run ingestion on 48MB Oahspe PDF
  - [ ] Verify images extracted to database
  - [ ] Check memory usage (should be < 512MB)
  - [ ] Measure processing time

- [ ] **Task 6.4.3:** Verify restart safety
  - [ ] Run ingestion twice on same PDF
  - [ ] Verify no duplicate images in database
  - [ ] Verify idempotent behavior

- [ ] **Task 6.4.4:** Final verification
  - [ ] Run `mvn clean test` - all tests pass
  - [ ] Query database for extracted images
  - [ ] Spot-check image data integrity

- [ ] **Task 6.4.5:** Commit and push
  - [ ] Stage all changes
  - [ ] Create comprehensive commit message
  - [ ] Push to remote repository

## Files to Create
| File | Location |
|------|----------|
| `OahspeIngestionRunnerImageIT.java` | `src/test/java/.../ingestion/runner/` (optional) |

## Dependencies
- Phase 6.1, 6.2, 6.3 all complete

## Success Criteria
- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] Real PDF extracts images successfully
- [ ] No duplicate images on restart
- [ ] Memory usage acceptable
- [ ] Changes committed and pushed

## Estimated Time
~20-30 minutes

## Commit Message Template
```
Phase 6: Add PDF image extraction feature

FEATURE SUMMARY
- Create PDFImageExtractor component for image extraction
- Integrate image extraction into OahspeIngestionRunner pipeline
- Add image tracking metrics to IngestionContext
- Implement idempotent save with unique image keys

FILES CREATED
- PDFImageExtractor.java: Core image extraction using PDFBox
- PDFImageExtractorTest.java: Unit tests for image extraction

FILES MODIFIED
- OahspeIngestionRunner.java: Add image extraction stage
- IngestionContext.java: Add totalImagesExtracted tracking
- Image.java: Increase imageKey column length

TESTING
- All unit tests pass
- Tested with 48MB Oahspe PDF
- Verified restart-safe idempotent behavior

BENEFITS
- Critical for accurate English→Chinese translation
- Preserves visual context from original document
- Page-by-page extraction is memory efficient
```
