package edu.minghualiu.oahspe.ingestion.parser;

/**
 * Enum representing the parser's current state in the state machine.
 * 
 * The parser uses a finite state machine to track its position in the document
 * structure and determine how to interpret continuation lines. State transitions
 * are driven by pattern matching against input lines.
 * 
 * <h2>State Machine Diagram</h2>
 * <pre>
 *     OUTSIDE_BOOK
 *          ↓ (BookStart pattern)
 *       IN_BOOK
 *          ↓ (ChapterStart pattern)
 *       IN_CHAPTER
 *       ├→ IN_VERSE (Verse pattern)
 *       │  ├→ (continuation) → stays IN_VERSE
 *       │  └→ IN_NOTE (Note pattern)
 *       │     ├→ (continuation) → stays IN_NOTE
 *       │     └→ IN_VERSE (Verse pattern)
 *       └→ IN_NOTE (Note pattern from IN_CHAPTER)
 * </pre>
 * 
 * <h2>Continuation Line Handling</h2>
 * When the parser encounters a line that doesn't match any pattern:
 * <ul>
 *   <li>If IN_VERSE: line is appended to current verse (with null verseKey)</li>
 *   <li>If IN_NOTE: line is appended to current note (with null noteKey)</li>
 *   <li>Otherwise: line is skipped or logged as unexpected</li>
 * </ul>
 * 
 * @author OahspeTeam
 * @see OahspeParser
 */
public enum ParserState {
    /**
     * Initial state before any book is detected.
     * Parser ignores all lines until a BookStart pattern is found.
     */
    OUTSIDE_BOOK,

    /**
     * Parser is inside a book but outside any chapter.
     * Parser looks for ChapterStart patterns to transition to IN_CHAPTER.
     */
    IN_BOOK,

    /**
     * Parser is inside a chapter, ready to parse verses.
     * Parser looks for Verse patterns to transition to IN_VERSE.
     */
    IN_CHAPTER,

    /**
     * Parser is inside a verse text.
     * Continuation lines (non-pattern-matching) are appended to the current verse.
     * Parser transitions to IN_NOTE when Note pattern is found.
     */
    IN_VERSE,

    /**
     * Parser is inside a note/footnote text.
     * Continuation lines (non-pattern-matching) are appended to the current note.
     * Parser transitions back to IN_VERSE when Verse pattern is found.
     */
    IN_NOTE
}
