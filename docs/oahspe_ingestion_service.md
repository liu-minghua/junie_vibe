Here you go, Minghua — a polished, production‑ready **Markdown documentation file** for the **Oahspe Ingestion Service**, designed to sit right after the parser documentation you already generated. It explains the architecture, responsibilities, flow, and integration points with your entities, repositories, and helper components.

You can save this as:

**`Oahspe_Ingestion_Service.md`**

---

# **Oahspe Ingestion Service Documentation**

This document describes the **OahspeIngestionService**, the core component that consumes parser events, builds the hierarchical JPA entity graph, links notes and images, and persists the entire structure into the database.

It sits between:

- **OahspeParser** (text → events)
- **Spring Data JPA** (entities → database)

and orchestrates the full ingestion workflow.

---

## **1. Purpose of the Ingestion Service**

The ingestion service is responsible for:

1. Reading PDF pages (text + images)
2. Passing text lines to the `OahspeParser`
3. Receiving structured events
4. Building the entity graph:
    - Book
    - Chapter
    - Verse
    - Note
    - Image
5. Linking notes ↔ images using `ImageNoteLinker`
6. Persisting the entire structure using cascade rules

This service ensures the ingestion is:

- Deterministic
- Idempotent
- Restart‑safe
- Modular
- Testable

---

## **2. High‑Level Architecture**

```
PDF → PDFBox Extractor → OahspeParser → OahspeIngestionService → JPA Entities → Database
```

The ingestion service is the **central coordinator**.

---

## **3. Responsibilities**

### **3.1 Maintain Current Context**
The service tracks the current:

- `Book`
- `Chapter`
- `Verse`
- `Note`

as it processes events.

### **3.2 Build Entities**
For each event:

- `BookStart` → create a new Book
- `ChapterStart` → create a new Chapter
- `Verse` → create or append to a Verse
- `Note` → create or append to a Note
- `ImageRef` → create an Image and link it to the current Note

### **3.3 Persist the Graph**
After finishing a book:

```java
bookRepository.save(book);
```

Cascade rules persist everything else.

---

## **4. Ingestion Service Class**

Below is the conceptual structure of the ingestion service.

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

    private Book currentBook;
    private Chapter currentChapter;
    private Verse currentVerse;
    private Note currentNote;

    public void ingestEvents(List<OahspeEvent> events) {

        for (OahspeEvent event : events) {

            if (event instanceof OahspeEvent.BookStart b) {
                startBook(b.title());
            }

            else if (event instanceof OahspeEvent.ChapterStart c) {
                startChapter(c.title());
            }

            else if (event instanceof OahspeEvent.Verse v) {
                processVerse(v.verseKey(), v.text());
            }

            else if (event instanceof OahspeEvent.Note n) {
                processNote(n.noteKey(), n.text());
            }

            else if (event instanceof OahspeEvent.ImageRef img) {
                processImage(img.imageKey(), img.caption());
            }
        }
    }
}
```

---

## **5. Entity Construction Logic**

### **5.1 Book Creation**

```java
private void startBook(String title) {
    currentBook = Book.builder()
            .title(title)
            .build();
}
```

### **5.2 Chapter Creation**

```java
private void startChapter(String title) {
    currentChapter = Chapter.builder()
            .title(title)
            .book(currentBook)
            .build();

    currentBook.getChapters().add(currentChapter);
}
```

### **5.3 Verse Processing**

```java
private void processVerse(String key, String text) {

    if (key != null) {
        currentVerse = Verse.builder()
                .verseKey(key)
                .text(text)
                .chapter(currentChapter)
                .build();

        currentChapter.getVerses().add(currentVerse);
    } else {
        // continuation line
        currentVerse.setText(currentVerse.getText() + " " + text);
    }
}
```

### **5.4 Note Processing**

```java
private void processNote(String key, String text) {

    if (key != null) {
        currentNote = Note.builder()
                .noteKey(key)
                .text(text)
                .verse(currentVerse)
                .build();

        currentVerse.getNotes().add(currentNote);
    } else {
        // continuation line
        currentNote.setText(currentNote.getText() + " " + text);
    }
}
```

### **5.5 Image Processing**

```java
private void processImage(String imageKey, String caption) {

    Image image = Image.builder()
            .imageKey(imageKey)
            .title(caption)
            .description(caption)
            .build();

    imageNoteLinker.linkImageToNote(currentNote, image);
}
```

---

## **6. Persisting the Book**

After all events for a book are processed:

```java
bookRepository.save(currentBook);
```

This persists:

- Book
- Chapters
- Verses
- Notes
- Images
- Join table `note_images`

thanks to your cascade settings.

---

## **7. Idempotency Strategy**

The ingestion service uses:

- `verseKey` (unique)
- `noteKey` (unique)
- `imageKey` (unique)

to avoid duplicate inserts.

Before creating a new entity:

```java
verseRepository.findByVerseKey(key)
noteRepository.findByNoteKey(key)
imageRepository.findByImageKey(key)
```

This allows:

- Restarting ingestion
- Partial ingestion
- Updating missing pieces

---

## **8. Error Handling**

The ingestion service should:

- Log malformed verse keys
- Log missing chapter markers
- Log orphaned notes
- Log image references without notes

These do not stop ingestion but help refine parsing rules.

---

## **9. Benefits of This Design**

- **Modular**: Parser, linker, and ingestion are separate.
- **Testable**: Each component can be unit‑tested.
- **Deterministic**: Same PDF → same database structure.
- **Idempotent**: Unique keys prevent duplicates.
- **Extensible**: Chinese translation fields can be filled later.

---

If you'd like, I can now generate the **full ingestion runner** (`OahspeIngestionRunner`) that ties everything together and processes the entire PDF from start to finish.