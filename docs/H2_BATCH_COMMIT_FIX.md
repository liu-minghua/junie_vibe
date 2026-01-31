# H2 File-Based Database Batch Commit Fix

## Problem Statement
H2 file-based database batch commits were not working properly, resulting in data not being saved to the database.

## Root Cause Analysis

The issue was identified in the `OahspeIngestionService` class with three specific problems:

### 1. Missing Transaction Management
The `ingestEvents()` method was **not marked with `@Transactional`**, causing:
- Each individual `save()` call to create its own transaction
- With file-based H2 databases, transactions may not be properly committed without explicit transaction boundaries
- Batch operations were effectively running as multiple isolated transactions instead of one atomic operation

### 2. Notes Not Being Persisted
In the `handleNote()` method, `Note` entities were only being added to the verse's notes collection but **never saved to the database**:
```java
// BEFORE (broken):
currentNote = Note.builder()
    .noteKey(event.noteKey())
    .text(event.text())
    .verse(currentVerse)
    .pageNumber(currentPageNumber)
    .build();
if (currentVerse != null) currentVerse.getNotes().add(currentNote);
// Missing save! Note was never persisted to database
```

### 3. Verse Text Updates Not Being Saved
In the `handleVerse()` method, when processing continuation lines (verses that span multiple lines), the updated text was **not being saved**:
```java
// BEFORE (broken):
else if (currentVerse != null) {
    currentVerse.setText(currentVerse.getText() + " " + event.text());
    // Missing save! Updated verse text was never persisted
}
```

## Solution

Three minimal surgical changes were made to `OahspeIngestionService.java`:

### Change 1: Add @Transactional to ingestEvents()
```java
@Transactional
public void ingestEvents(List<OahspeEvent> events, int pageNumber) {
    // ... existing code ...
}
```

**Impact**: All entity operations within a batch of events now run in a single transaction, ensuring atomic commits to the file-based database.

### Change 2: Save Note Entities
```java
private void handleNote(OahspeEvent.Note event) {
    if (event.noteKey() != null) {
        currentNote = Note.builder()
            .noteKey(event.noteKey())
            .text(event.text())
            .verse(currentVerse)
            .pageNumber(currentPageNumber)
            .build();
        if (currentVerse != null) currentVerse.getNotes().add(currentNote);
        currentNote = noteRepository.save(currentNote);  // ← ADDED
    } else if (currentNote != null) {
        // Continuation line - update and save
        currentNote.setText(currentNote.getText() + " " + event.text());
        currentNote = noteRepository.save(currentNote);  // ← ADDED
    }
}
```

**Impact**: Notes are now properly persisted to the database.

### Change 3: Save Verse Text Updates
```java
private void handleVerse(OahspeEvent.Verse event) {
    // ... existing code for new verses ...
    } else if (currentVerse != null) {
        // Continuation line - update and save
        currentVerse.setText(currentVerse.getText() + " " + event.text());
        currentVerse = verseRepository.save(currentVerse);  // ← ADDED
    }
}
```

**Impact**: Verse text continuation lines are now properly persisted to the database.

## Verification

### Manual Testing with File-Based H2
To verify the fix works with a file-based H2 database:

1. **Configure file-based H2** in `application-persistent.properties`:
   ```properties
   spring.datasource.url=jdbc:h2:file:./data/oahspe-db;AUTO_SERVER=TRUE
   spring.jpa.hibernate.ddl-auto=update
   ```

2. **Run ingestion** with the persistent profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=persistent --workflow data/OAHSPE.pdf"
   ```

3. **Verify data persistence**:
   - Stop the application
   - Start the application again with the same profile
   - Connect to H2 console at http://localhost:8080/h2-console
   - Query the database:
     ```sql
     SELECT COUNT(*) FROM book;
     SELECT COUNT(*) FROM chapter;
     SELECT COUNT(*) FROM verse;
     SELECT COUNT(*) FROM note;
     
     -- Verify continuation lines are saved
     SELECT verse_key, text FROM verse WHERE LENGTH(text) > 100 LIMIT 5;
     
     -- Verify notes are saved
     SELECT note_key, text FROM note LIMIT 10;
     ```

### Expected Results
- Books, chapters, verses, and notes should all be persisted
- Verse text should include continuation lines (longer text)
- Notes should exist in the database (not just empty collection)
- Data should survive application restart (proving file-based persistence works)

### Unit Test Verification
The existing unit tests in `OahspeIngestionServiceOrphanedContentTest` verify the save operations:
- `verify(verseRepository, times(1)).save(verseCaptor.capture())` - Verifies verse saves
- Similar patterns can be added for note saves

## Technical Details

### Why @Transactional Matters for File-Based H2

**In-Memory H2** (default for tests):
- Auto-commits most operations
- Data is volatile, so partial commits don't matter
- Works without explicit transactions

**File-Based H2** (production):
- Requires explicit transaction commits to flush to disk
- Without `@Transactional`, each save() creates a mini-transaction
- Some operations may not commit properly, especially in batch operations
- Transaction boundaries ensure all-or-nothing semantics

### Entity Cascade Behavior

The fix explicitly saves child entities (notes, verse updates) because:
1. JPA cascade settings may not persist changes to existing entities
2. Continuation lines modify existing verses but don't create new ones
3. Explicit saves ensure changes are tracked in the persistence context

## Related Files
- `src/main/java/edu/minghualiu/oahspe/ingestion/OahspeIngestionService.java` - Main fix
- `src/main/resources/application-persistent.properties` - File-based H2 configuration
- `src/test/java/edu/minghualiu/oahspe/ingestion/OahspeIngestionServiceOrphanedContentTest.java` - Unit tests

## Migration Notes
No database migration required - this is a code-only fix that improves persistence behavior.
