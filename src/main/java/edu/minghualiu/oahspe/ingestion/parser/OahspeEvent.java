package edu.minghualiu.oahspe.ingestion.parser;

/**
 * Sealed interface representing events emitted by OahspeParser.
 * 
 * This interface defines all possible parsing events that can occur when
 * processing Oahspe text content. The parser emits a stream of these typed
 * events, which can be consumed by downstream services for data ingestion.
 * 
 * The sealed interface ensures type safety and enables exhaustive pattern
 * matching with Java records for immutable event data.
 * 
 * <h2>Event Types</h2>
 * <ul>
 *   <li><strong>BookStart</strong> - Marks the beginning of a book</li>
 *   <li><strong>ChapterStart</strong> - Marks the beginning of a chapter</li>
 *   <li><strong>Verse</strong> - Contains verse text with optional key marker</li>
 *   <li><strong>Note</strong> - Contains note/footnote text with optional key</li>
 *   <li><strong>ImageRef</strong> - References an image in the document</li>
 *   <li><strong>PageBreak</strong> - Marks a page transition in the source PDF</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * OahspeParser parser = new OahspeParser();
 * List<String> lines = Arrays.asList("Book of Apollo", "Chapter 7", "14/7.1 Text...");
 * List<OahspeEvent> events = parser.parse(lines, 1);
 * 
 * events.forEach(event -> {
 *     switch (event) {
 *         case OahspeEvent.BookStart book -> 
 *             System.out.println("Book: " + book.title());
 *         case OahspeEvent.Verse verse -> 
 *             System.out.println("Verse " + verse.verseKey() + ": " + verse.text());
 *         case OahspeEvent.Note note -> 
 *             System.out.println("Note " + note.noteKey() + ": " + note.text());
 *         case OahspeEvent.ImageRef img -> 
 *             System.out.println("Image: " + img.imageKey());
 *         default -> {}
 *     }
 * });
 * }</pre>
 * 
 * @author OahspeTeam
 * @see OahspeParser
 * @see ParserState
 */
public sealed interface OahspeEvent permits
    OahspeEvent.BookStart,
    OahspeEvent.ChapterStart,
    OahspeEvent.Verse,
    OahspeEvent.Note,
    OahspeEvent.ImageRef,
    OahspeEvent.PageBreak {

    /**
     * Event emitted when the parser detects the start of a book.
     * 
     * @param title The book title (e.g., "Book of Apollo", "Book of Jehovih")
     */
    record BookStart(String title) implements OahspeEvent {}

    /**
     * Event emitted when the parser detects the start of a chapter.
     * 
     * @param title The chapter title or header (e.g., "Chapter 1", "第七章")
     */
    record ChapterStart(String title) implements OahspeEvent {}

    /**
     * Event emitted when the parser detects a verse line.
     * 
     * @param verseKey The verse key in format "XX/YY.ZZ" (e.g., "14/7.1"),
     *                 or null if this line continues the previous verse
     * @param text The verse text content
     */
    record Verse(String verseKey, String text) implements OahspeEvent {}

    /**
     * Event emitted when the parser detects a note/footnote line.
     * 
     * @param noteKey The note number/key (e.g., "1", "42"),
     *                or null if this line continues the previous note
     * @param text The note text content
     */
    record Note(String noteKey, String text) implements OahspeEvent {}

    /**
     * Event emitted when the parser detects a reference to an image.
     * 
     * @param imageKey The image identifier/key (e.g., "fig_14.1")
     * @param caption The image caption or description
     */
    record ImageRef(String imageKey, String caption) implements OahspeEvent {}

    /**
     * Event emitted when the parser detects a page break in the source document.
     * 
     * @param pageNumber The page number of the source PDF
     */
    record PageBreak(int pageNumber) implements OahspeEvent {}
}
