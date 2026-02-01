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
    private boolean introductionChapterCreated = false;
    
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
        
        // Recover state if needed - handles case where previous transaction rolled back
        recoverStateIfNeeded();
        
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
    
    /**
     * Recover state from database. Always re-fetches entities to handle detachment
     * due to transaction boundaries or rollbacks.
     * 
     * This method is called at the start of each page ingestion to ensure we have
     * managed entities in the current persistence context.
     */
    private void recoverStateIfNeeded() {
        // Always try to re-fetch entities to ensure they're managed in current persistence context
        if (currentBook != null && currentBook.getId() != null) {
            Integer bookId = currentBook.getId();
            Long chapterId = (currentChapter != null && currentChapter.getId() != null) 
                    ? currentChapter.getId() : null;
            
            // Re-fetch book
            currentBook = bookRepository.findById(bookId).orElse(null);
            
            // Re-fetch chapter if we had one
            if (chapterId != null && currentBook != null) {
                currentChapter = chapterRepository.findById(chapterId).orElse(null);
                // Verify chapter still belongs to current book
                if (currentChapter != null && !currentChapter.getBook().getId().equals(currentBook.getId())) {
                    currentChapter = null;
                }
            } else if (currentBook != null && !currentBook.getChapters().isEmpty()) {
                // Get the last chapter from the book
                currentChapter = currentBook.getChapters().get(currentBook.getChapters().size() - 1);
            } else {
                currentChapter = null;
            }
            
            log.trace("Recovered state: book={}, chapter={}", 
                    currentBook != null ? currentBook.getTitle() : "null",
                    currentChapter != null ? currentChapter.getTitle() : "null");
        }
    }
    
    private void handleBookStart(OahspeEvent.BookStart event) {
        currentBook = Book.builder()
                .title(event.title())
                .pageNumber(currentPageNumber)
                .build();
        currentBook = bookRepository.save(currentBook);
        currentChapter = null;
        currentVerse = null;
        currentNote = null;
        log.debug("Starting book: {} on page {}", event.title(), currentPageNumber);
    }
    
    private void handleChapterStart(OahspeEvent.ChapterStart event) {
        currentChapter = Chapter.builder()
                .title(event.title())
                .book(currentBook)
                .pageNumber(currentPageNumber)
                .build();
        // Don't manipulate collections - just save with the FK reference
        currentChapter = chapterRepository.save(currentChapter);
        currentVerse = null;
        currentNote = null;
        log.debug("Created chapter: {} for book: {}", event.title(), 
                currentBook != null ? currentBook.getTitle() : "null");
    }
    private void handleVerse(OahspeEvent.Verse event) {
        // Handle orphaned content: create introduction chapter if verse appears before any chapter
        if (currentChapter == null && event.verseKey() != null) {
            createIntroductionChapter();
        }
        
        if (event.verseKey() != null) {
            currentVerse = Verse.builder()
                    .verseKey(event.verseKey())
                    .text(event.text())
                    .chapter(currentChapter)
                    .pageNumber(currentPageNumber)
                    .build();
            // Don't manipulate collections - just save with the FK reference
            currentVerse = verseRepository.save(currentVerse);
            currentNote = null;
            log.trace("Saved verse: {} on page {}", event.verseKey(), currentPageNumber);
        } else if (currentVerse != null) {
            // Continuation line - append text
            currentVerse.setText(currentVerse.getText() + " " + event.text());
            currentVerse = verseRepository.save(currentVerse);
        }
    }
    
    private void handleNote(OahspeEvent.Note event) {
        if (event.noteKey() != null) {
            currentNote = Note.builder()
                    .noteKey(event.noteKey())
                    .text(event.text())
                    .verse(currentVerse)
                    .pageNumber(currentPageNumber)
                    .build();
            // Don't manipulate collections - just save with the FK reference
            currentNote = noteRepository.save(currentNote);
            log.trace("Saved note: {} on page {}", event.noteKey(), currentPageNumber);
        } else if (currentNote != null) {
            // Continuation line - append text
            currentNote.setText(currentNote.getText() + " " + event.text());
            currentNote = noteRepository.save(currentNote);
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
        introductionChapterCreated = false;
    }
    
    /**
     * Creates an "Introduction" chapter for orphaned content that appears before any formal book/chapter.
     * 
     * <p>This handles edge cases where verse content exists in preface/introduction sections
     * before the first BookStart/ChapterStart events. Creates a pseudo-book and chapter to
     * maintain database referential integrity while preserving all content.</p>
     * 
     * <p>The introduction book and chapter use descriptive titles to clearly distinguish
     * them from regular content.</p>
     */
    private void createIntroductionChapter() {
        if (!introductionChapterCreated) {
            log.info("Creating introduction chapter for orphaned content before first book/chapter on page {}", currentPageNumber);
            
            // Create or get introduction book
            if (currentBook == null) {
                currentBook = Book.builder()
                        .title("Introduction")
                        .description("Preface and introductory content")
                        .pageNumber(currentPageNumber)
                        .build();
                currentBook = bookRepository.save(currentBook);
                log.debug("Created introduction book on page {}", currentPageNumber);
            }
            
            // Create introduction chapter - don't manipulate collections
            currentChapter = Chapter.builder()
                    .title("Preface")
                    .description("Content before first formal chapter")
                    .book(currentBook)
                    .pageNumber(currentPageNumber)
                    .build();
            currentChapter = chapterRepository.save(currentChapter);
            
            introductionChapterCreated = true;
            log.debug("Created introduction chapter for book: {} on page {}", currentBook.getTitle(), currentPageNumber);
        }
    }
    
    /**
     * Returns whether an introduction chapter was auto-created during this ingestion session.
     * 
     * @return true if introduction chapter was created for orphaned content
     */
    public boolean isIntroductionChapterCreated() {
        return introductionChapterCreated;
    }
}
