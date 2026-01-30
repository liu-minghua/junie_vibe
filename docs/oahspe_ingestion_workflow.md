Absolutely, Minghua — here is a clean, polished **Markdown documentation file** that captures the entire Oahspe ingestion workflow, including the parser state machine, event model, and architectural rationale. You can drop this directly into your project as `Oahspe_Ingestion_Workflow.md`.

---

# **Oahspe PDF Ingestion Workflow Documentation**

This document describes the full ingestion architecture for converting the 48 MB Oahspe PDF into a structured relational model using:

- **Book**
- **Chapter**
- **Verse**
- **Note**
- **Image**

It covers the parser state machine, event model, regex patterns, and the overall flow from PDF → Entities → Database.

---

## **1. Overview**

The ingestion pipeline converts raw PDF content into a structured, queryable database. It consists of three major layers:

1. **Extraction Layer**  
   Reads PDF pages, extracts text and images.

2. **Parsing Layer (State Machine)**  
   Converts raw text into structured events such as `BookStart`, `ChapterStart`, `Verse`, `Note`, and `ImageRef`.

3. **Mapping & Persistence Layer**  
   Builds the JPA entity graph and persists it using Spring Data JPA.

This architecture ensures:

- Deterministic parsing
- Idempotent ingestion
- Clean separation of concerns
- Full traceability (page numbers, image metadata)

---

## **2. Event Model**

The parser emits strongly typed events representing structural elements of Oahspe.

```java
public interface OahspeEvent {

    record BookStart(String title) implements OahspeEvent {}
    record ChapterStart(String title) implements OahspeEvent {}
    record Verse(String verseKey, String text) implements OahspeEvent {}
    record Note(String noteKey, String text) implements OahspeEvent {}
    record ImageRef(String imageKey, String caption) implements OahspeEvent {}
    record PageBreak(int pageNumber) implements OahspeEvent {}
}
```

These events drive the ingestion service.

---

## **3. Parser State Machine**

The parser maintains a simple state machine:

```java
public enum ParserState {
    OUTSIDE_BOOK,
    IN_BOOK,
    IN_CHAPTER,
    IN_VERSE,
    IN_NOTE
}
```

The state determines how continuation lines are interpreted.

---

## **4. Regex Patterns**

These patterns detect Oahspe’s structural markers.

```java
private static final Pattern BOOK_PATTERN =
        Pattern.compile("^(Book of .+|.*?之.*?书)$");

private static final Pattern CHAPTER_PATTERN =
        Pattern.compile("^(Chapter\\s+\\d+|第[一二三四五六七八九十百]+章)$");

private static final Pattern VERSE_PATTERN =
        Pattern.compile("^(\\d+\\/\\d+\\.\\d+)\\s*(.*)$");

private static final Pattern NOTE_PATTERN =
        Pattern.compile("^\\(?([0-9]+)\\)?\\s*(.*)$");

private static final Pattern IMAGE_PATTERN =
        Pattern.compile("^i(\\d{3})\\s+(.*)$");
```

These patterns support both English and Chinese headings.

---

## **5. OahspeParser Implementation**

The parser processes each page line‑by‑line and emits events.

```java
public class OahspeParser {

    private ParserState state = ParserState.OUTSIDE_BOOK;

    public List<OahspeEvent> parse(List<String> lines, int pageNumber) {
        List<OahspeEvent> events = new ArrayList<>();

        events.add(new OahspeEvent.PageBreak(pageNumber));

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Book detection
            Matcher bookMatcher = BOOK_PATTERN.matcher(line);
            if (bookMatcher.matches()) {
                state = ParserState.IN_BOOK;
                events.add(new OahspeEvent.BookStart(line));
                continue;
            }

            // Chapter detection
            Matcher chapterMatcher = CHAPTER_PATTERN.matcher(line);
            if (chapterMatcher.matches()) {
                state = ParserState.IN_CHAPTER;
                events.add(new OahspeEvent.ChapterStart(line));
                continue;
            }

            // Verse detection
            Matcher verseMatcher = VERSE_PATTERN.matcher(line);
            if (verseMatcher.matches()) {
                state = ParserState.IN_VERSE;
                events.add(new OahspeEvent.Verse(
                        verseMatcher.group(1),
                        verseMatcher.group(2)
                ));
                continue;
            }

            // Note detection
            Matcher noteMatcher = NOTE_PATTERN.matcher(line);
            if (noteMatcher.matches()) {
                state = ParserState.IN_NOTE;
                events.add(new OahspeEvent.Note(
                        noteMatcher.group(1),
                        noteMatcher.group(2)
                ));
                continue;
            }

            // Image reference detection
            Matcher imgMatcher = IMAGE_PATTERN.matcher(line);
            if (imgMatcher.matches()) {
                events.add(new OahspeEvent.ImageRef(
                        "IMG" + imgMatcher.group(1),
                        imgMatcher.group(2)
                ));
                continue;
            }

            // Continuation lines
            switch (state) {
                case IN_VERSE -> events.add(new OahspeEvent.Verse(null, line));
                case IN_NOTE -> events.add(new OahspeEvent.Note(null, line));
            }
        }

        return events;
    }
}
```

---

## **6. Image–Note Linking Helper**

This helper ensures images are persisted before linking and prevents transient‑entity exceptions.

```java
@Component
@RequiredArgsConstructor
public class ImageNoteLinker {

    private final ImageRepository imageRepository;
    private final NoteRepository noteRepository;

    @Transactional
    public void linkImageToNote(Note note, Image image) {

        Image persistedImage = imageRepository.findByImageKey(image.getImageKey())
                .orElseGet(() -> imageRepository.save(image));

        Note persistedNote = (note.getId() == 0)
                ? noteRepository.save(note)
                : note;

        if (!persistedNote.getImages().contains(persistedImage)) {
            persistedNote.getImages().add(persistedImage);
        }

        if (!persistedImage.getNotes().contains(persistedNote)) {
            persistedImage.getNotes().add(persistedNote);
        }

        noteRepository.save(persistedNote);
    }
}
```

---

## **7. Ingestion Flow Summary**

### **Step 1 — Extract PDF Content**
- Extract text per page
- Extract images with metadata (page number, filename, content type)

### **Step 2 — Parse Text**
- Feed each page’s text into `OahspeParser`
- Receive structured events

### **Step 3 — Build Entity Graph**
- Create Book → Chapter → Verse → Note → Image objects
- Use `ImageNoteLinker` for image associations

### **Step 4 — Persist**
Because of cascade rules:

```java
bookRepository.save(book);
```

This persists the entire hierarchy.

---

## **8. Benefits of This Architecture**

- **Deterministic**: Same PDF always produces the same structure.
- **Idempotent**: Unique keys prevent duplicates.
- **Modular**: Parser, extractor, and linker are independent.
- **Testable**: Each component can be unit‑tested.
- **Extensible**: Supports Chinese translation fields without modifying ingestion.

---

If you'd like, I can generate the next documentation section describing the **OahspeIngestionService**, which consumes parser events and builds the JPA entity graph.