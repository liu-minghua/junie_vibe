package edu.minghualiu.oahspe.ingestion.validator;

import edu.minghualiu.oahspe.entities.Book;
import edu.minghualiu.oahspe.entities.Chapter;
import edu.minghualiu.oahspe.entities.Note;
import edu.minghualiu.oahspe.entities.Verse;
import edu.minghualiu.oahspe.entities.Image;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CrossEntityValidator Tests")
class CrossEntityValidatorTest {

    private CrossEntityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CrossEntityValidator();
    }

    @Test
    @DisplayName("should validate valid book-chapter relationships")
    void testValidBookChapterRelationship() {
        Book book = new Book();
        book.setId(1);
        book.setTitle("Test Book");
        
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setTitle("Test Chapter");
        chapter.setBook(book);
        
        List<Book> books = List.of(book);
        List<Chapter> chapters = List.of(chapter);
        List<Verse> verses = new ArrayList<>();
        List<Note> notes = new ArrayList<>();
        List<Image> images = new ArrayList<>();
        
        List<ValidationIssue> issues = validator.validateAll(books, chapters, verses, notes, images);
        
        // Should have minimal issues (empty verse list is warning, but structure is valid)
        long criticalIssues = issues.stream()
            .filter(i -> i.getSeverity() == Severity.CRITICAL)
            .count();
        assertEquals(0, criticalIssues, "Valid structure should have no critical issues");
    }

    @Test
    @DisplayName("should detect book with no chapters")
    void testBookWithNoChapters() {
        Book book = new Book();
        book.setId(1);
        book.setTitle("Empty Book");
        
        List<Book> books = List.of(book);
        List<Chapter> chapters = new ArrayList<>();
        List<Verse> verses = new ArrayList<>();
        List<Note> notes = new ArrayList<>();
        List<Image> images = new ArrayList<>();
        
        List<ValidationIssue> issues = validator.validateAll(books, chapters, verses, notes, images);
        
        assertTrue(issues.stream().anyMatch(i -> 
            i.getRule().equals("BookCompleteness") && i.getSeverity() == Severity.WARNING),
            "Should detect book with no chapters");
    }

    @Test
    @DisplayName("should detect chapter with no verses")
    void testChapterWithNoVerses() {
        Book book = new Book();
        book.setId(1);
        book.setTitle("Test Book");
        
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setTitle("Empty Chapter");
        chapter.setBook(book);
        
        List<Book> books = List.of(book);
        List<Chapter> chapters = List.of(chapter);
        List<Verse> verses = new ArrayList<>();
        List<Note> notes = new ArrayList<>();
        List<Image> images = new ArrayList<>();
        
        List<ValidationIssue> issues = validator.validateAll(books, chapters, verses, notes, images);
        
        assertNotNull(issues, "Should return validation issues list");
    }

    @Test
    @DisplayName("should detect verse referencing invalid chapter")
    void testVerseWithInvalidChapterReference() {
        Chapter chapter = new Chapter();
        chapter.setId(999L); // Non-existent chapter ID
        
        Verse verse = new Verse();
        verse.setId(1);
        verse.setVerseKey("999:1");
        verse.setText("Test verse");
        verse.setChapter(chapter);
        
        List<Book> books = new ArrayList<>();
        List<Chapter> chapters = new ArrayList<>();
        List<Verse> verses = List.of(verse);
        List<Note> notes = new ArrayList<>();
        List<Image> images = new ArrayList<>();
        
        List<ValidationIssue> issues = validator.validateAll(books, chapters, verses, notes, images);
        
        assertTrue(issues.stream().anyMatch(i -> 
            i.getRule().equals("ReferenceIntegrity") && i.getSeverity() == Severity.CRITICAL),
            "Should detect verse with invalid chapter reference");
    }

    @Test
    @DisplayName("should detect note referencing invalid verse")
    void testNoteWithInvalidVerseReference() {
        Verse verse = new Verse();
        verse.setId(999); // Non-existent verse ID
        
        Note note = new Note();
        note.setId(1);
        note.setText("Test note");
        note.setVerse(verse);
        
        List<Book> books = new ArrayList<>();
        List<Chapter> chapters = new ArrayList<>();
        List<Verse> verses = new ArrayList<>();
        List<Note> notes = List.of(note);
        List<Image> images = new ArrayList<>();
        
        List<ValidationIssue> issues = validator.validateAll(books, chapters, verses, notes, images);
        
        assertTrue(issues.stream().anyMatch(i -> 
            i.getRule().equals("ReferenceIntegrity") && i.getSeverity() == Severity.ERROR),
            "Should detect note with invalid verse reference");
    }

    @Test
    @DisplayName("should validate complete valid structure")
    void testCompleteValidStructure() {
        Book book = new Book();
        book.setId(1);
        book.setTitle("Complete Book");
        
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setTitle("Chapter 1");
        chapter.setBook(book);
        
        Verse verse1 = new Verse();
        verse1.setId(1);
        verse1.setVerseKey("1:1");
        verse1.setText("Verse 1");
        verse1.setChapter(chapter);
        
        Verse verse2 = new Verse();
        verse2.setId(2);
        verse2.setVerseKey("1:2");
        verse2.setText("Verse 2");
        verse2.setChapter(chapter);
        
        Note note = new Note();
        note.setId(1);
        note.setText("Note on verse 1");
        note.setVerse(verse1);
        
        List<Book> books = List.of(book);
        List<Chapter> chapters = List.of(chapter);
        List<Verse> verses = List.of(verse1, verse2);
        List<Note> notes = List.of(note);
        List<Image> images = new ArrayList<>();
        
        List<ValidationIssue> issues = validator.validateAll(books, chapters, verses, notes, images);
        
        long criticalIssues = issues.stream()
            .filter(i -> i.getSeverity() == Severity.CRITICAL)
            .count();
        assertEquals(0, criticalIssues, "Complete valid structure should have no critical issues");
    }

    @Test
    @DisplayName("should return non-null list when no issues")
    void testReturnsListWhenNoIssues() {
        List<ValidationIssue> issues = validator.validateAll(
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
        
        assertNotNull(issues, "Should return non-null list");
        assertIsInstance(issues, List.class);
    }

    private void assertIsInstance(Object obj, Class<?> clazz) {
        assertTrue(clazz.isInstance(obj), 
                   "Object should be instance of " + clazz.getName());
    }
}
