# OahspeIngestionService Usage Guide

## Overview

The `OahspeIngestionService` is the core orchestrator component that consumes `OahspeEvent` objects from the parser and builds the complete entity hierarchy (Book → Chapter → Verse → Note ↔ Image).

**Purpose:** Transform streaming parser events into persisted entities with proper relationships and state management.

**Input:** `List<OahspeEvent>` from [OahspeParser](PARSER_USAGE_GUIDE.md)  
**Output:** Persisted entity hierarchy in database with bidirectional relationships

---

## Architecture

### Dependency Injection

```java
@Service
@RequiredArgsConstructor
public class OahspeIngestionService {
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final VerseRepository verseRepository;
    private final NoteRepository noteRepository;
    private final ImageRepository imageRepository;
    private final ImageNoteLinker imageNoteLinker;
```

The service depends on 5 Spring Data JPA repositories and the `ImageNoteLinker` component for managing image-note relationships.

### State Management

The service maintains contextual state across event processing:

```java
    private Book currentBook;
    private Chapter currentChapter;
    private Verse currentVerse;
    private Note currentNote;
    private int currentPageNumber;
```

**Lifecycle:**
1. `currentBook` created on `BookStart` event
2. `currentChapter` created on `ChapterStart` event
3. `currentVerse` created on each `Verse` event with key
4. `currentNote` created on each `Note` event with key
5. All reset on `finishIngestion()`

---

## Event Processing

### Event Type Dispatch

The service handles 6 event types via pattern matching:

```java
public void ingestEvents(List<OahspeEvent> events, int pageNumber) {
    this.currentPageNumber = pageNumber;
    for (OahspeEvent event : events) {
        switch (event) {
            case OahspeEvent.BookStart book -> handleBookStart(book);
            case OahspeEvent.ChapterStart chapter -> handleChapterStart(chapter);
            case OahspeEvent.Verse verse -> handleVerse(verse);
            case OahspeEvent.Note note -> handleNote(note);
            case OahspeEvent.ImageRef image -> handleImageRef(image);
            case OahspeEvent.PageBreak page -> log.trace("Page {} complete", page.pageNumber());
        }
    }
}
```

### Event Handlers

#### BookStart Handler
Creates a new book entity and persists it immediately.
```java
private void handleBookStart(OahspeEvent.BookStart event) {
    currentBook = Book.builder().title(event.title()).build();
    currentBook = bookRepository.save(currentBook);
    // ... reset other state
}
```

#### ChapterStart Handler
Creates a chapter linked to current book and persists it immediately.
```java
private void handleChapterStart(OahspeEvent.ChapterStart event) {
    currentChapter = Chapter.builder()
        .title(event.title())
        .book(currentBook)
        .build();
    if (currentBook != null) currentBook.getChapters().add(currentChapter);
    currentChapter = chapterRepository.save(currentChapter);
    // ... reset verse/note state
}
```

#### Verse Handler
Handles both initial verse creation and continuation lines:
```java
private void handleVerse(OahspeEvent.Verse event) {
    if (event.verseKey() != null) {
        // New verse: create and persist
        currentVerse = Verse.builder()
            .verseKey(event.verseKey())
            .text(event.text())
            .chapter(currentChapter)
            .build();
        if (currentChapter != null) currentChapter.getVerses().add(currentVerse);
        currentVerse = verseRepository.save(currentVerse);
        currentNote = null;
    } else {
        // Continuation line: append to current verse
        if (currentVerse != null) {
            currentVerse.setText(currentVerse.getText() + " " + event.text());
        }
    }
}
```

**Note:** Verses with `verseKey=null` represent continuation lines from the previous verse and are automatically concatenated.

#### Note Handler
Similar pattern: create on key-bearing events, append on continuation.
```java
private void handleNote(OahspeEvent.Note event) {
    if (event.noteKey() != null) {
        currentNote = Note.builder()
            .noteKey(event.noteKey())
            .text(event.text())
            .verse(currentVerse)
            .build();
        if (currentVerse != null) currentVerse.getNotes().add(currentNote);
    } else if (currentNote != null) {
        currentNote.setText(currentNote.getText() + " " + event.text());
    }
}
```

#### ImageRef Handler
Creates image and links it to current note via `ImageNoteLinker`:
```java
private void handleImageRef(OahspeEvent.ImageRef event) {
    Image image = Image.builder()
        .imageKey(event.imageKey())
        .title(event.caption())
        .description(event.caption())
        .sourcePage(currentPageNumber)
        .build();
    Image savedImage = imageRepository.save(image);
    if (currentNote != null) 
        imageNoteLinker.linkImageToNote(currentNote, savedImage);
}
```

---

## Persistence Methods

### saveCurrentBook()
```java
@Transactional
public void saveCurrentBook() {
    if (currentBook != null) bookRepository.save(currentBook);
}
```

Persists the current book. Called at the end of each page/batch to ensure all updates are flushed to the database.

### finishIngestion()
```java
@Transactional
public void finishIngestion() {
    saveCurrentBook();
    currentBook = null;
    currentChapter = null;
    currentVerse = null;
    currentNote = null;
}
```

Completes an ingestion session: saves current book and resets all state for the next book. **Must be called between separate books.**

---

## Basic Usage Example

### Single Verse Ingestion

```java
// Inject the service
@Autowired
private OahspeIngestionService service;

// Process events
List<OahspeEvent> events = Arrays.asList(
    new OahspeEvent.BookStart("Oahspe"),
    new OahspeEvent.ChapterStart("Chapter 1"),
    new OahspeEvent.Verse("1:1", "This is a verse.")
);

service.ingestEvents(events, 1);  // page number = 1
service.saveCurrentBook();         // persist to database
```

### Multi-Chapter Book Ingestion

```java
List<OahspeEvent> events = Arrays.asList(
    new OahspeEvent.BookStart("Oahspe"),
    new OahspeEvent.ChapterStart("Chapter 1"),
    new OahspeEvent.Verse("1:1", "First verse"),
    new OahspeEvent.Verse("1:2", "Second verse"),
    new OahspeEvent.ChapterStart("Chapter 2"),
    new OahspeEvent.Verse("2:1", "Chapter 2 verse")
);

service.ingestEvents(events, 1);
service.saveCurrentBook();
```

### Notes with Images

```java
List<OahspeEvent> events = Arrays.asList(
    new OahspeEvent.BookStart("Oahspe"),
    new OahspeEvent.ChapterStart("Chapter 1"),
    new OahspeEvent.Verse("1:1", "Verse with image"),
    new OahspeEvent.Note("1:1a", "Note attached to verse"),
    new OahspeEvent.ImageRef("IMG001", "Portrait of Oahspe")
);

service.ingestEvents(events, 1);
service.saveCurrentBook();
// Result: Image IMG001 is linked to Note 1:1a via many-to-many
```

### Continuation Lines

```java
List<OahspeEvent> events = Arrays.asList(
    new OahspeEvent.BookStart("Oahspe"),
    new OahspeEvent.ChapterStart("Chapter 1"),
    new OahspeEvent.Verse("1:1", "This is a long verse "),
    new OahspeEvent.Verse(null, "that continues on the next line.")
);

service.ingestEvents(events, 1);
service.saveCurrentBook();
// Result: Single verse with text "This is a long verse that continues on the next line."
```

### Multiple Books

```java
// Book 1
List<OahspeEvent> book1Events = Arrays.asList(
    new OahspeEvent.BookStart("Book One"),
    new OahspeEvent.ChapterStart("Chapter 1"),
    new OahspeEvent.Verse("1:1", "Book one")
);

service.ingestEvents(book1Events, 1);
service.saveCurrentBook();
service.finishIngestion();  // IMPORTANT: reset state

// Book 2
List<OahspeEvent> book2Events = Arrays.asList(
    new OahspeEvent.BookStart("Book Two"),
    new OahspeEvent.ChapterStart("Chapter 1"),
    new OahspeEvent.Verse("1:1", "Book two")
);

service.ingestEvents(book2Events, 5);
service.saveCurrentBook();
```

---

## State Management Best Practices

### 1. Always Call finishIngestion() Between Books
Failure to call `finishIngestion()` will cause the next book to inherit the previous book's context:

```java
// ❌ WRONG: No state reset
service.ingestEvents(book1Events, 1);
service.saveCurrentBook();
service.ingestEvents(book2Events, 5);  // Inherits book1's context!

// ✅ CORRECT: Reset state between books
service.ingestEvents(book1Events, 1);
service.saveCurrentBook();
service.finishIngestion();  // Reset all context
service.ingestEvents(book2Events, 5);  // Fresh context
```

### 2. Page Numbers Must Be Tracked
The `pageNumber` parameter is stored with each image reference:

```java
// Page 1 ingestion
service.ingestEvents(page1Events, 1);

// Page 2 ingestion (page 1 images had sourcePage=1)
service.ingestEvents(page2Events, 2);

service.saveCurrentBook();
```

### 3. Continuation Line Formatting
Verses split across lines are indicated by `verseKey=null`:

```java
// From parser:
new OahspeEvent.Verse("1:1", "And God said ")
new OahspeEvent.Verse(null, "Let there be light")

// Result in database:
verse.text = "And God said  Let there be light"
```

---

## Error Handling

The service may throw the following exceptions:

### DataIntegrityViolationException
Thrown when uniqueness constraints are violated:
- Duplicate `verseKey` in same chapter
- Duplicate `noteKey` globally
- Duplicate `imageKey` globally

**Mitigation:** Ensure parser provides unique keys per context.

### TransientPropertyValueException
Thrown when attempting to persist an entity with unsaved references. The service handles this by persisting entities in dependency order (Book → Chapter → Verse → Note).

---

## Transaction Boundaries

```java
// Single transaction per event batch
public void ingestEvents(List<OahspeEvent> events, int pageNumber)
    // Each repository.save() within this method is auto-enlisted in transaction

// Each persistence method is its own transaction
@Transactional
public void saveCurrentBook()

@Transactional
public void finishIngestion()
```

For atomic multi-batch operations, wrap calls:

```java
@Transactional
public void ingestBook(List<OahspeEvent> allEvents) {
    // Process multiple page batches
    for (int page = 1; page <= 100; page++) {
        List<OahspeEvent> pageEvents = getPageEvents(page);
        service.ingestEvents(pageEvents, page);
    }
    service.saveCurrentBook();
}
```

---

## Performance Considerations

### Eager Persistence
The service immediately persists Book/Chapter/Verse entities to avoid transient reference issues. This means:
- More database round-trips
- Better transaction safety
- Easier debugging

### Image Linking Idempotency
The `ImageNoteLinker` component handles duplicate image references safely:

```java
// First occurrence: creates image
new OahspeEvent.ImageRef("IMG001", "Image")
service.ingestEvents(...);

// Second occurrence: finds existing by imageKey
new OahspeEvent.ImageRef("IMG001", "Image")
service.ingestEvents(...);
// Result: Single Image entity, not duplicated
```

---

## Integration with OahspeParser

See [PARSER_USAGE_GUIDE.md](PARSER_USAGE_GUIDE.md) for parser output format.

```
PDF Text
   ↓
OahspeParser.parsePage(text) → List<OahspeEvent>
   ↓
OahspeIngestionService.ingestEvents(events, pageNum) → Database
```

---

## Testing

See [OahspeIngestionServiceIT](../src/test/java/edu/minghualiu/oahspe/ingestion/service/OahspeIngestionServiceIT.java) for 6 comprehensive integration tests covering:
- Parser-to-service integration
- Continuation line handling
- Image linking
- Full page ingestion with mixed entities
- Idempotent image linking
- Multiple books ingestion

