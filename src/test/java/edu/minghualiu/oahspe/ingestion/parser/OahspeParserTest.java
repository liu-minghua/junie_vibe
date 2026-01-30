package edu.minghualiu.oahspe.ingestion.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for OahspeParser state machine.
 * 
 * Tests cover:
 * - P1: Book title detection
 * - P2: Chapter header detection
 * - P3: Verse line parsing
 * - P4: Note line parsing
 * - P5: Image reference parsing
 * - P6: Continuation line handling
 * - Edge cases and pattern boundaries
 * - Full page scenarios
 * - Invalid inputs
 * - State machine transitions
 * - Performance with large inputs
 */
@DisplayName("OahspeParser Unit Tests")
class OahspeParserTest {

    private OahspeParser parser;

    @BeforeEach
    void setUp() {
        parser = new OahspeParser();
    }

    // ============ Test Group P1: Book Title Detection ============

    @Test
    @DisplayName("P1: Should detect 'Book of Apollo' title")
    void test_P1_DetectBookTitles_Apollo() {
        List<String> input = List.of("Book of Apollo");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size(), "Should have PageBreak + BookStart");
        assertInstanceOf(OahspeEvent.PageBreak.class, events.get(0));
        assertInstanceOf(OahspeEvent.BookStart.class, events.get(1));

        OahspeEvent.BookStart book = (OahspeEvent.BookStart) events.get(1);
        assertEquals("Book of Apollo", book.title());
    }

    @Test
    @DisplayName("P1: Should detect 'Book of Jehovih' title")
    void test_P1_DetectBookTitles_Jehovih() {
        List<String> input = List.of("Book of Jehovih");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.BookStart book = (OahspeEvent.BookStart) events.get(1);
        assertEquals("Book of Jehovih", book.title());
    }

    @Test
    @DisplayName("P1: Should detect 'Book of Oahspe' title")
    void test_P1_DetectBookTitles_Oahspe() {
        List<String> input = List.of("Book of Oahspe");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.BookStart book = (OahspeEvent.BookStart) events.get(1);
        assertEquals("Book of Oahspe", book.title());
    }

    // ============ Test Group P2: Chapter Header Detection ============

    @Test
    @DisplayName("P2: Should detect 'Chapter 7' header")
    void test_P2_DetectChapterTitles_Chapter7() {
        List<String> input = List.of("Chapter 7");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size(), "Should have PageBreak + ChapterStart");
        assertInstanceOf(OahspeEvent.PageBreak.class, events.get(0));
        assertInstanceOf(OahspeEvent.ChapterStart.class, events.get(1));

        OahspeEvent.ChapterStart chapter = (OahspeEvent.ChapterStart) events.get(1);
        assertEquals("Chapter 7", chapter.title());
    }

    @Test
    @DisplayName("P2: Should detect 'Chapter 1' header")
    void test_P2_DetectChapterTitles_Chapter1() {
        List<String> input = List.of("Chapter 1");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.ChapterStart chapter = (OahspeEvent.ChapterStart) events.get(1);
        assertEquals("Chapter 1", chapter.title());
    }

    @Test
    @DisplayName("P2: Should detect 'Chapter 42' header")
    void test_P2_DetectChapterTitles_Chapter42() {
        List<String> input = List.of("Chapter 42");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.ChapterStart chapter = (OahspeEvent.ChapterStart) events.get(1);
        assertEquals("Chapter 42", chapter.title());
    }

    // ============ Test Group P3: Verse Line Parsing ============

    @Test
    @DisplayName("P3: Should detect verse with key 14/7.1")
    void test_P3_DetectVerseLines_Standard() {
        List<String> input = List.of("14/7.1 In the beginning...");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size(), "Should have PageBreak + Verse");
        assertInstanceOf(OahspeEvent.Verse.class, events.get(1));

        OahspeEvent.Verse verse = (OahspeEvent.Verse) events.get(1);
        assertEquals("14/7.1", verse.verseKey());
        assertEquals("In the beginning...", verse.text());
    }

    @Test
    @DisplayName("P3: Should detect verse with key 1/0.1")
    void test_P3_DetectVerseLines_Low() {
        List<String> input = List.of("1/0.1 Jehovih said...");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.Verse verse = (OahspeEvent.Verse) events.get(1);
        assertEquals("1/0.1", verse.verseKey());
        assertEquals("Jehovih said...", verse.text());
    }

    @Test
    @DisplayName("P3: Should detect verse with key 48/7.99")
    void test_P3_DetectVerseLines_High() {
        List<String> input = List.of("48/7.99 Far future...");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.Verse verse = (OahspeEvent.Verse) events.get(1);
        assertEquals("48/7.99", verse.verseKey());
        assertEquals("Far future...", verse.text());
    }

    // ============ Test Group P4: Note Line Parsing ============

    @Test
    @DisplayName("P4: Should detect note with parentheses (1)")
    void test_P4_DetectNoteLines_WithParentheses() {
        List<String> input = List.of("(1) This refers to...");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size(), "Should have PageBreak + Note");
        assertInstanceOf(OahspeEvent.Note.class, events.get(1));

        OahspeEvent.Note note = (OahspeEvent.Note) events.get(1);
        assertEquals("1", note.noteKey());
        assertEquals("This refers to...", note.text());
    }

    @Test
    @DisplayName("P4: Should detect note without parentheses 1)")
    void test_P4_DetectNoteLines_WithoutOpenParenthesis() {
        List<String> input = List.of("1) Also valid format");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.Note note = (OahspeEvent.Note) events.get(1);
        assertEquals("1", note.noteKey());
        assertEquals("Also valid format", note.text());
    }

    @Test
    @DisplayName("P4: Should detect multi-digit note (42)")
    void test_P4_DetectNoteLines_MultiDigit() {
        List<String> input = List.of("(42) Multi-digit note");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.Note note = (OahspeEvent.Note) events.get(1);
        assertEquals("42", note.noteKey());
        assertEquals("Multi-digit note", note.text());
    }

    // ============ Test Group P5: Image Reference Parsing ============

    @Test
    @DisplayName("P5: Should detect image reference i002")
    void test_P5_DetectImageReferences_i002() {
        List<String> input = List.of("i002 Etherea Roadway");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size(), "Should have PageBreak + ImageRef");
        assertInstanceOf(OahspeEvent.ImageRef.class, events.get(1));

        OahspeEvent.ImageRef image = (OahspeEvent.ImageRef) events.get(1);
        assertEquals("IMG002", image.imageKey());
        assertEquals("Etherea Roadway", image.caption());
    }

    @Test
    @DisplayName("P5: Should detect image reference i045")
    void test_P5_DetectImageReferences_i045() {
        List<String> input = List.of("i045 The divine plate");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.ImageRef image = (OahspeEvent.ImageRef) events.get(1);
        assertEquals("IMG045", image.imageKey());
        assertEquals("The divine plate", image.caption());
    }

    @Test
    @DisplayName("P5: Should detect image reference i001")
    void test_P5_DetectImageReferences_i001() {
        List<String> input = List.of("i001 Opening image");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.ImageRef image = (OahspeEvent.ImageRef) events.get(1);
        assertEquals("IMG001", image.imageKey());
        assertEquals("Opening image", image.caption());
    }

    // ============ Test Group P6: Continuation Line Handling ============

    @Test
    @DisplayName("P6: Should handle verse continuation lines")
    void test_P6_ContinuationLines_Verse() {
        List<String> input = List.of(
            "14/7.1 Jehovih said...",
            "and the Lords answered..."
        );
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(3, events.size(), "Should have PageBreak + 2 Verses");
        assertInstanceOf(OahspeEvent.Verse.class, events.get(1));
        assertInstanceOf(OahspeEvent.Verse.class, events.get(2));

        OahspeEvent.Verse v1 = (OahspeEvent.Verse) events.get(1);
        assertEquals("14/7.1", v1.verseKey());

        OahspeEvent.Verse v2 = (OahspeEvent.Verse) events.get(2);
        assertNull(v2.verseKey(), "Continuation verse should have null key");
        assertEquals("and the Lords answered...", v2.text());
    }

    @Test
    @DisplayName("P6: Should handle note continuation lines")
    void test_P6_ContinuationLines_Note() {
        List<String> input = List.of(
            "(1) This is a note",
            "that spans multiple",
            "lines of text"
        );
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(4, events.size(), "Should have PageBreak + 3 Notes");
        assertInstanceOf(OahspeEvent.Note.class, events.get(1));
        assertInstanceOf(OahspeEvent.Note.class, events.get(2));
        assertInstanceOf(OahspeEvent.Note.class, events.get(3));

        OahspeEvent.Note n1 = (OahspeEvent.Note) events.get(1);
        assertEquals("1", n1.noteKey());

        OahspeEvent.Note n2 = (OahspeEvent.Note) events.get(2);
        assertNull(n2.noteKey(), "Continuation note should have null key");

        OahspeEvent.Note n3 = (OahspeEvent.Note) events.get(3);
        assertNull(n3.noteKey(), "Continuation note should have null key");
    }

    @Test
    @DisplayName("P6: Should handle mixed verse/note/continuation")
    void test_P6_ContinuationLines_Mixed() {
        List<String> input = List.of(
            "Book of Apollo",
            "Chapter 7",
            "14/7.1 In the beginning...",
            "and it was good"
        );
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(5, events.size(), "Should have PageBreak + BookStart + ChapterStart + Verse + Verse continuation");
        assertTrue(events.get(0) instanceof OahspeEvent.PageBreak);
        assertTrue(events.get(1) instanceof OahspeEvent.BookStart);
        assertTrue(events.get(2) instanceof OahspeEvent.ChapterStart);

        OahspeEvent.Verse v1 = (OahspeEvent.Verse) events.get(3);
        assertEquals("14/7.1", v1.verseKey());
    }

    // ============ Test Group: Edge Cases ============

    @Test
    @DisplayName("Edge Case: Empty input list")
    void test_EdgeCase_EmptyInput() {
        List<String> input = List.of();
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(1, events.size(), "Should have only PageBreak");
        assertInstanceOf(OahspeEvent.PageBreak.class, events.get(0));
    }

    @Test
    @DisplayName("Edge Case: Whitespace only lines")
    void test_EdgeCase_WhitespaceOnly() {
        List<String> input = List.of("   ", "\t\t", "  \t  ");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(1, events.size(), "Should skip all whitespace lines");
        assertInstanceOf(OahspeEvent.PageBreak.class, events.get(0));
    }

    @Test
    @DisplayName("Edge Case: Leading/trailing whitespace trimmed")
    void test_EdgeCase_TrimmedWhitespace() {
        List<String> input = List.of("  Book of Apollo  ");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(2, events.size());
        OahspeEvent.BookStart book = (OahspeEvent.BookStart) events.get(1);
        assertEquals("Book of Apollo", book.title(), "Whitespace should be trimmed");
    }

    @Test
    @DisplayName("Edge Case: Multiple spaces in verse text")
    void test_EdgeCase_MultipleSpaces() {
        List<String> input = List.of("14/7.1   Multiple   spaces   inside");
        List<OahspeEvent> events = parser.parse(input, 1);

        OahspeEvent.Verse verse = (OahspeEvent.Verse) events.get(1);
        assertEquals("Multiple   spaces   inside", verse.text(), "Internal spaces preserved");
    }

    // ============ Test Group: Full Page Scenario ============

    @Test
    @DisplayName("Full page: Mixed book, chapter, verses, notes, images")
    void test_FullPageScenario() {
        List<String> input = List.of(
            "Book of Apollo",
            "Chapter 7",
            "14/7.1 In the beginning Jehovih spoke to the hosts...",
            "(1) This refers to the creation moment",
            "14/7.2 And the hosts were assembled in the great council...",
            "(2) The hosts are the divine angels of the creation",
            "i003 The Divine Throne",
            "14/7.3 And it was good..."
        );

        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(9, events.size(), "Full page should have 9 events");
        assertTrue(events.get(0) instanceof OahspeEvent.PageBreak);
        assertTrue(events.get(1) instanceof OahspeEvent.BookStart);
        assertTrue(events.get(2) instanceof OahspeEvent.ChapterStart);
        assertTrue(events.get(3) instanceof OahspeEvent.Verse);
        assertTrue(events.get(4) instanceof OahspeEvent.Note);
        assertTrue(events.get(5) instanceof OahspeEvent.Verse);
        assertTrue(events.get(6) instanceof OahspeEvent.Note);
        assertTrue(events.get(7) instanceof OahspeEvent.ImageRef);
        assertTrue(events.get(8) instanceof OahspeEvent.Verse);
    }

    // ============ Test Group: Invalid Patterns ============

    @Test
    @DisplayName("Invalid: Random text that doesn't match")
    void test_InvalidPatterns_RandomText() {
        List<String> input = List.of("This is random text that matches nothing");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(1, events.size(), "Random text should be ignored");
    }

    @Test
    @DisplayName("Invalid: Verse with wrong format (missing decimal)")
    void test_InvalidPatterns_WrongVerseFormat() {
        List<String> input = List.of("14/7 wrong format");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(1, events.size(), "Malformed verse should not match");
    }

    @Test
    @DisplayName("Invalid: Image with 2 digits instead of 3")
    void test_InvalidPatterns_ImageWrongDigits() {
        List<String> input = List.of("i02 only two digits");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(1, events.size(), "Image number must be exactly 3 digits");
    }

    @Test
    @DisplayName("Invalid: Image with 4 digits instead of 3")
    void test_InvalidPatterns_ImageTooManyDigits() {
        List<String> input = List.of("i9999 too many digits");
        List<OahspeEvent> events = parser.parse(input, 1);

        assertEquals(1, events.size(), "Image number must be exactly 3 digits");
    }

    // ============ Test Group: State Machine Transitions ============

    @Test
    @DisplayName("State: State transitions to IN_VERSE when parsing a verse")
    void test_StateMachine_InitialState() {
        // When parsing a verse, state should transition to IN_VERSE
        List<String> input = List.of("14/7.1 Verse");
        parser.parse(input, 1);

        assertEquals(ParserState.IN_VERSE, parser.getState());
    }

    @Test
    @DisplayName("State: OUTSIDE_BOOK → IN_BOOK when book detected")
    void test_StateMachine_BookTransition() {
        List<String> input = List.of("Book of Apollo");
        parser.parse(input, 1);

        assertEquals(ParserState.IN_BOOK, parser.getState());
    }

    @Test
    @DisplayName("State: IN_BOOK → IN_CHAPTER when chapter detected")
    void test_StateMachine_ChapterTransition() {
        List<String> input = List.of(
            "Book of Apollo",
            "Chapter 7"
        );
        parser.parse(input, 1);

        assertEquals(ParserState.IN_CHAPTER, parser.getState());
    }

    @Test
    @DisplayName("State: IN_CHAPTER → IN_VERSE when verse detected")
    void test_StateMachine_VerseTransition() {
        List<String> input = List.of(
            "Book of Apollo",
            "Chapter 7",
            "14/7.1 Text"
        );
        parser.parse(input, 1);

        assertEquals(ParserState.IN_VERSE, parser.getState());
    }

    @Test
    @DisplayName("State: IN_VERSE → IN_NOTE when note detected")
    void test_StateMachine_NoteTransition() {
        List<String> input = List.of(
            "Book of Apollo",
            "Chapter 7",
            "14/7.1 Verse text",
            "(1) Note text"
        );
        parser.parse(input, 1);

        assertEquals(ParserState.IN_NOTE, parser.getState());
    }

    // ============ Test Group: Performance ============

    @Test
    @DisplayName("Performance: Parse 1000+ lines efficiently")
    void test_ParserPerformance() {
        List<String> input = new ArrayList<>();
        input.add("Book of Apollo");
        input.add("Chapter 7");

        // Generate 500 verses with notes
        for (int i = 0; i < 500; i++) {
            input.add(String.format("%d/7.%d Verse text...", (i % 50) + 1, i));
            input.add(String.format("(%d) Note text", i % 100));
        }

        long startTime = System.nanoTime();
        List<OahspeEvent> events = parser.parse(input, 1);
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;

        assertNotNull(events, "Parser should return event list");
        assertTrue(events.size() > 500, "Should have parsed all lines");
        assertTrue(durationMs < 1000, 
            "Parser took " + durationMs + "ms (should be < 1000ms)");
    }
}
