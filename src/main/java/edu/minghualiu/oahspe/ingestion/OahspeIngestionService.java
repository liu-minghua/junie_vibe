package edu.minghualiu.oahspe.ingestion;

import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.ingestion.parser.OahspeEvent;
import edu.minghualiu.oahspe.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Oahspe Ingestion Service - Core orchestrator for event-to-entity transformation.
 *
 * <p>This service consumes {@link OahspeEvent} objects from the {@link edu.minghualiu.oahspe.ingestion.parser.OahspeParser}
 * and builds the complete entity hierarchy: Book → Chapter → Verse → Note ↔ Image.</p>
 *
 * <p><b>Architecture Pattern:</b> Event-driven state machine with contextual persistence.
 * <ul>
 *   <li>Maintains context state across event sequence (currentBook, currentChapter, currentVerse, currentNote)</li>
 *   <li>Dispatches events to specialized handlers via pattern matching</li>
 *   <li>Persists entities eagerly to maintain referential integrity</li>
 *   <li>Manages image-note relationships via {@link ImageNoteLinker} component</li>
 * </ul></p>
 *
 * <p><b>Typical Usage:</b>
 * <pre>
 * List&lt;OahspeEvent&gt; events = parser.parsePage(pdfText);
 * service.ingestEvents(events, pageNumber);
 * service.saveCurrentBook();  // persist after batch
 * service.finishIngestion();  // reset state before next book
 * </pre>
 * </p>
 *
 * @see OahspeEvent
 * @see ImageNoteLinker
 * @see <a href="../docs/INGESTION_SERVICE_USAGE.md">Service Usage Guide</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
    private int currentPageNumber;
    
    /**
     * Ingests a batch of events from the parser and updates entity context.
     *
     * <p>Processes events sequentially, dispatching each to its specialized handler.
     * Handles all six event types: BookStart, ChapterStart, Verse, Note, ImageRef, PageBreak.</p>
     *
     * <p><b>Continuation Lines:</b> Verse and Note events with null keys represent
     * continuation lines from the previous verse/note and are concatenated with a space separator.</p>
     *
     * <p><b>Persistence:</b> Book, Chapter, and Verse entities are persisted immediately
     * to maintain referential integrity. Call {@link #saveCurrentBook()} to flush remaining updates.</p>
     *
     * @param events List of OahspeEvent objects from the parser
     * @param pageNumber Current page number (stored with image references for tracking)
     *
     * @example
     * <pre>
     * List&lt;OahspeEvent&gt; events = parser.parsePage(pdfText);
     * service.ingestEvents(events, pageNumber);
     * </pre>
     *
     * @see OahspeEvent
     */
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
    
    private void handleBookStart(OahspeEvent.BookStart event) {
        currentBook = Book.builder().title(event.title()).build();
        currentBook = bookRepository.save(currentBook);
        currentChapter = null;
        currentVerse = null;
        currentNote = null;
        log.debug("Starting book: {}", event.title());
    }
    
    private void handleChapterStart(OahspeEvent.ChapterStart event) {
        currentChapter = Chapter.builder().title(event.title()).book(currentBook).build();
        if (currentBook != null) currentBook.getChapters().add(currentChapter);
        currentChapter = chapterRepository.save(currentChapter);
        currentVerse = null;
        currentNote = null;
    }
    private void handleVerse(OahspeEvent.Verse event) {
        if (event.verseKey() != null) {
            currentVerse = Verse.builder().verseKey(event.verseKey()).text(event.text()).chapter(currentChapter).build();
            if (currentChapter != null) currentChapter.getVerses().add(currentVerse);
            currentVerse = verseRepository.save(currentVerse);
            currentNote = null;
        } else if (currentVerse != null) {
            currentVerse.setText(currentVerse.getText() + " " + event.text());
        }
    }
    
    private void handleNote(OahspeEvent.Note event) {
        if (event.noteKey() != null) {
            currentNote = Note.builder().noteKey(event.noteKey()).text(event.text()).verse(currentVerse).build();
            if (currentVerse != null) currentVerse.getNotes().add(currentNote);
        } else if (currentNote != null) {
            currentNote.setText(currentNote.getText() + " " + event.text());
        }
    }
    
    private void handleImageRef(OahspeEvent.ImageRef event) {
        Image image = Image.builder()
                .imageKey(event.imageKey())
                .title(event.caption())
                .description(event.caption())
                .sourcePage(currentPageNumber)
                .build();
        Image savedImage = imageRepository.save(image);
        if (currentNote != null) imageNoteLinker.linkImageToNote(currentNote, savedImage);
    }
    
    @Transactional
    public void saveCurrentBook() {
        if (currentBook != null) bookRepository.save(currentBook);
    }
    
    /**
     * Completes the current ingestion session and resets all context state.
     *
     * <p><b>Must be called:</b> Between processing different books to prevent context leakage.
     * Failure to call this method will cause the next book to inherit the previous book's
     * context (currentBook, currentChapter, currentVerse, currentNote).</p>
     *
     * <p><b>Operations:</b>
     * <ol>
     *   <li>Persists current book via {@link #saveCurrentBook()}</li>
     *   <li>Nullifies all context fields (Book, Chapter, Verse, Note)</li>
     *   <li>Resets page number to 0</li>
     * </ol>
     * </p>
     *
     * @example
     * <pre>
     * // Process Book 1
     * service.ingestEvents(book1Events, 1);
     * service.saveCurrentBook();
     * service.finishIngestion();  // IMPORTANT: reset state
     *
     * // Process Book 2 (clean context)
     * service.ingestEvents(book2Events, 5);
     * service.saveCurrentBook();
     * </pre>
     */
    @Transactional
    public void finishIngestion() {
        saveCurrentBook();
        currentBook = null;
        currentChapter = null;
        currentVerse = null;
        currentNote = null;
    }
}
