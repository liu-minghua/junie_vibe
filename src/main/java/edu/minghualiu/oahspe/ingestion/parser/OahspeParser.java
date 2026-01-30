package edu.minghualiu.oahspe.ingestion.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core parser for Oahspe text content.
 * 
 * OahspeParser implements a finite state machine that converts raw text lines
 * into a stream of structured OahspeEvent objects. The parser detects:
 * - Book titles (e.g., "Book of Apollo")
 * - Chapter headers (e.g., "Chapter 1")
 * - Verses with markers (e.g., "14/7.1 ...")
 * - Notes/footnotes (e.g., "(1) ...")
 * - Image references (e.g., "i002 Caption")
 * - Page breaks
 * 
 * The parser is deterministic: given the same input, it always produces
 * the same output. It also handles continuation lines intelligently based on
 * the current parsing state.
 * 
 * Thread safety: Each call to parse() resets internal state, making the
 * parser safe for concurrent use across different parse jobs.
 */
@Component
public class OahspeParser {

    private static final Logger log = LoggerFactory.getLogger(OahspeParser.class);

    // Regex Patterns (precompiled for performance)
    
    /**
     * Pattern for detecting book titles.
     * Matches English format "Book of [Title]" and Chinese format "[Prefix]之[Suffix]书"
     * Examples: "Book of Apollo", "Book of Jehovih", "启示之书"
     */
    private static final Pattern BOOK_PATTERN = 
        Pattern.compile("^(Book of .+|.*?之.*?书)$");

    /**
     * Pattern for detecting chapter headers.
     * Matches English format "Chapter N" and Chinese format "第[ZH]章"
     * Examples: "Chapter 1", "Chapter 42", "第七章"
     */
    private static final Pattern CHAPTER_PATTERN = 
        Pattern.compile("^(Chapter\\s+\\d+|第[一二三四五六七八九十百]+章)$");

    /**
     * Pattern for detecting verse markers and text.
     * Format: "XX/YY.ZZ [Text...]"
     * Capture groups: (1) verse key, (2) text
     * Examples: "14/7.1 In the beginning...", "1/0.1 Jehovih spoke"
     */
    private static final Pattern VERSE_PATTERN = 
        Pattern.compile("^(\\d+/\\d+\\.\\d+)\\s+(.*)$");

    /**
     * Pattern for detecting note/footnote markers and text.
     * Format: "(N) [Text...]" or "N) [Text...]"
     * Capture groups: (1) note number, (2) text
     * Examples: "(1) This refers to...", "42) Another note"
     */
    private static final Pattern NOTE_PATTERN = 
        Pattern.compile("^\\(?([0-9]+)\\)?\\s+(.*)$");

    /**
     * Pattern for detecting image references.
     * Format: "iNNN [Caption...]" where NNN is exactly 3 digits
     * Capture groups: (1) image number, (2) caption
     * Examples: "i002 Etherea Roadway", "i045 The divine plate"
     */
    private static final Pattern IMAGE_PATTERN = 
        Pattern.compile("^i(\\d{3})\\s+(.*)$");

    // Parser state (reset on each parse call)
    private ParserState state = ParserState.OUTSIDE_BOOK;

    /**
     * Parse a list of text lines from a PDF page and emit OahspeEvent stream.
     * 
     * The parser resets its state on each call, making it safe for processing
     * multiple pages independently. Events are emitted in the order detected.
     * 
     * The parser implements a deterministic finite state machine:
     * <ul>
     *   <li>Starts in OUTSIDE_BOOK state</li>
     *   <li>Transitions to IN_BOOK on BookStart pattern match</li>
     *   <li>Transitions to IN_CHAPTER on ChapterStart pattern match</li>
     *   <li>Transitions to IN_VERSE on Verse pattern match</li>
     *   <li>Transitions to IN_NOTE on Note pattern match</li>
     *   <li>Continuation lines (non-pattern-matching) are appended based on state</li>
     * </ul>
     * 
     * <h2>Usage Example</h2>
     * <pre>{@code
     * OahspeParser parser = new OahspeParser();
     * List<String> lines = Arrays.asList(
     *     "Book of Apollo",
     *     "Chapter 7",
     *     "14/7.1 In the beginning...",
     *     "(1) This refers to creation",
     *     "i003 Divine throne"
     * );
     * List<OahspeEvent> events = parser.parse(lines, 1);
     * 
     * // Process events using pattern matching (Java 17+)
     * events.forEach(event -> {
     *     switch (event) {
     *         case OahspeEvent.BookStart book -> 
     *             System.out.println("Book: " + book.title());
     *         case OahspeEvent.ChapterStart chapter -> 
     *             System.out.println("Chapter: " + chapter.title());
     *         case OahspeEvent.Verse verse -> {
     *             if (verse.verseKey() != null) {
     *                 System.out.println("Verse " + verse.verseKey() + ": " + verse.text());
     *             } else {
     *                 System.out.println("  (continuation) " + verse.text());
     *             }
     *         }
     *         case OahspeEvent.Note note -> 
     *             System.out.println("Note " + note.noteKey() + ": " + note.text());
     *         case OahspeEvent.ImageRef img -> 
     *             System.out.println("Image: " + img.imageKey() + " - " + img.caption());
     *         case OahspeEvent.PageBreak pb -> 
     *             System.out.println("--- Page " + pb.pageNumber() + " ---");
     *     }
     * });
     * }</pre>
     * 
     * @param lines the text lines to parse (typically one page from PDF)
     * @param pageNumber the source page number (included in PageBreak event)
     * @return list of OahspeEvent objects emitted during parsing, in order
     * @throws IllegalArgumentException if lines is null
     * 
     * @see OahspeEvent
     * @see ParserState
     */
    public List<OahspeEvent> parse(List<String> lines, int pageNumber) {
        if (lines == null) {
            throw new IllegalArgumentException("lines cannot be null");
        }

        List<OahspeEvent> events = new ArrayList<>();
        state = ParserState.OUTSIDE_BOOK;

        // Always emit page break first
        events.add(new OahspeEvent.PageBreak(pageNumber));
        log.debug("Processing page {} with {} lines", pageNumber, lines.size());

        for (String line : lines) {
            line = line.trim();
            
            // Skip empty lines
            if (line.isEmpty()) {
                continue;
            }

            log.trace("Processing line: {}", line);

            // Try patterns in order: Book → Chapter → Verse → Note → Image → Continuation

            // Pattern 1: Book Title
            Matcher bookMatcher = BOOK_PATTERN.matcher(line);
            if (bookMatcher.matches()) {
                state = ParserState.IN_BOOK;
                OahspeEvent.BookStart event = new OahspeEvent.BookStart(line);
                events.add(event);
                log.debug("Detected book: {}", line);
                continue;
            }

            // Pattern 2: Chapter Header
            Matcher chapterMatcher = CHAPTER_PATTERN.matcher(line);
            if (chapterMatcher.matches()) {
                ParserState oldState = state;
                state = ParserState.IN_CHAPTER;
                OahspeEvent.ChapterStart event = new OahspeEvent.ChapterStart(line);
                events.add(event);
                log.debug("State transition: {} -> {}", oldState, state);
                log.debug("Detected chapter: {}", line);
                continue;
            }

            // Pattern 3: Verse (with marker)
            Matcher verseMatcher = VERSE_PATTERN.matcher(line);
            if (verseMatcher.matches()) {
                String verseKey = verseMatcher.group(1);
                String verseText = verseMatcher.group(2);
                ParserState oldState = state;
                state = ParserState.IN_VERSE;
                OahspeEvent.Verse event = new OahspeEvent.Verse(verseKey, verseText);
                events.add(event);
                log.debug("State transition: {} -> {}", oldState, state);
                log.debug("Detected verse: {} with {} chars", verseKey, verseText.length());
                continue;
            }

            // Pattern 4: Note (with number)
            Matcher noteMatcher = NOTE_PATTERN.matcher(line);
            if (noteMatcher.matches()) {
                String noteKey = noteMatcher.group(1);
                String noteText = noteMatcher.group(2);
                // Avoid matching false positives: notes should start with (N) or N)
                // Check if line actually starts with note marker pattern
                if (line.startsWith("(") || (line.charAt(0) >= '0' && line.charAt(0) <= '9' && line.length() > 1 && line.charAt(1) == ')')) {
                    ParserState oldState = state;
                    state = ParserState.IN_NOTE;
                    OahspeEvent.Note event = new OahspeEvent.Note(noteKey, noteText);
                    events.add(event);
                    log.debug("State transition: {} -> {}", oldState, state);
                    continue;
                }
            }

            // Pattern 5: Image Reference
            Matcher imageMatcher = IMAGE_PATTERN.matcher(line);
            if (imageMatcher.matches()) {
                String imageNumber = imageMatcher.group(1);
                String caption = imageMatcher.group(2);
                String imageKey = "IMG" + imageNumber; // Add IMG prefix
                OahspeEvent.ImageRef event = new OahspeEvent.ImageRef(imageKey, caption);
                events.add(event);
                log.debug("Detected image: i{} - {}", imageNumber, caption);
                continue;
            }

            // Pattern 6: Continuation lines (context-dependent)
            handleContinuationLine(line, events);
        }

        log.debug("Completed parsing page {} - emitted {} events", pageNumber, events.size());
        return events;
    }

    /**
     * Handle continuation lines based on parser state.
     * 
     * Continuation lines are text that doesn't match any pattern but belongs to
     * the current context (e.g., multi-line verses or multi-line notes).
     * 
     * <h2>Behavior by State</h2>
     * <ul>
     *   <li>IN_VERSE: Creates Verse event with null verseKey (continuation marker)</li>
     *   <li>IN_NOTE: Creates Note event with null noteKey (continuation marker)</li>
     *   <li>OUTSIDE_BOOK, IN_BOOK, IN_CHAPTER: Lines are logged as unexpected</li>
     * </ul>
     * 
     * @param line the continuation line text
     * @param events the event list to append to
     */
    private void handleContinuationLine(String line, List<OahspeEvent> events) {
        switch (state) {
            case IN_VERSE -> {
                OahspeEvent.Verse event = new OahspeEvent.Verse(null, line);
                events.add(event);
                log.trace("Continuation verse: {}", line);
            }
            case IN_NOTE -> {
                OahspeEvent.Note event = new OahspeEvent.Note(null, line);
                events.add(event);
                log.trace("Continuation note: {}", line);
            }
            case OUTSIDE_BOOK, IN_BOOK, IN_CHAPTER -> {
                log.warn("Unexpected line content in state {}: {}", state, line);
            }
        }
    }

    /**
     * Get the current parser state. Primarily for testing.
     * 
     * @return the current parsing state
     */
    public ParserState getState() {
        return state;
    }
}
