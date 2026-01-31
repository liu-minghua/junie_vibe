# Phase 7.1 Tasklist: Handle Orphaned Content (NULL Chapter ID)

**Status:** In Progress  
**Priority:** High  
**Objective:** Fix NULL chapter_id constraint violations for content before first book/chapter

## Problem Statement

Pages with verse content before any chapter is declared fail with:
```
NULL not allowed for column "CHAPTER_ID"
```

This occurs with introduction/preface content in the Oahspe PDF.

## Solution: Create Pseudo-Chapter

When a verse is encountered without a current chapter:
1. Auto-create "Introduction" chapter
2. Attach it to an "Introduction" book (or current book if exists)
3. Attach orphaned verses to this chapter
4. Track in context for reporting

## Tasks

- [ ] **Task 7.1.1:** Modify `OahspeIngestionService.handleVerse()`
  - [ ] Add check for `currentChapter == null`
  - [ ] Create helper method `createIntroductionChapter()`
  - [ ] Auto-create book if needed
  - [ ] Set as current chapter
  - [ ] Log creation for visibility

- [ ] **Task 7.1.2:** Create helper methods
  - [ ] `createOrGetIntroductionBook()` - Get/create "Introduction" book
  - [ ] `createIntroductionChapter(Book book)` - Create pseudo-chapter
  - [ ] Use book number 0, chapter number 0 for introduction

- [ ] **Task 7.1.3:** Add tracking to IngestionContext
  - [ ] Add `introductionChapterCreated` boolean flag
  - [ ] Add `orphanedVersesCount` counter
  - [ ] Update `toString()` to include these metrics

- [ ] **Task 7.1.4:** Write unit tests
  - [ ] Test verse without chapter creates introduction
  - [ ] Test multiple orphaned verses use same introduction chapter
  - [ ] Test transition from introduction to first real chapter
  - [ ] Test metrics tracking

- [ ] **Task 7.1.5:** Run verification
  - [ ] Run all existing tests (should still pass)
  - [ ] Run test with PDF containing orphaned content
  - [ ] Verify introduction chapter created in database
  - [ ] Verify verses attached correctly

## Files to Modify

| File | Changes |
|------|---------|
| `OahspeIngestionService.java` | Add orphaned content handling logic |
| `IngestionContext.java` | Add introduction tracking fields |
| `OahspeIngestionServiceTest.java` | Add test cases for orphaned content |

## Acceptance Criteria

- [ ] No NULL chapter_id exceptions for orphaned content
- [ ] Introduction book/chapter auto-created with clear naming
- [ ] All orphaned verses attached to introduction chapter
- [ ] Context tracks introduction creation
- [ ] All existing tests pass
- [ ] New tests cover edge cases

## Testing Strategy

1. **Unit Tests:** Mock scenarios with orphaned verses
2. **Integration Test:** Use test PDF with content before first book
3. **Manual Test:** Run with OAHSPE_Standard_Edition.pdf pages 1-45

## Expected Outcome

After Phase 7.1:
- ✅ Process pages 1-42 without NULL chapter_id errors
- ✅ Introduction chapter visible in database
- ✅ Orphaned verses preserved
- ✅ Clear logging of pseudo-chapter creation

## Next Phase

After 7.1 completion → Phase 7.2: Handle duplicate image keys
