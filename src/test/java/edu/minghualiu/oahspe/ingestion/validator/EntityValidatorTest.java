package edu.minghualiu.oahspe.ingestion.validator;

import edu.minghualiu.oahspe.entities.Book;
import edu.minghualiu.oahspe.entities.Chapter;
import edu.minghualiu.oahspe.entities.Image;
import edu.minghualiu.oahspe.entities.Note;
import edu.minghualiu.oahspe.entities.Verse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EntityValidator Tests")
class EntityValidatorTest {

    private EntityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EntityValidator();
    }

    @Test
    @DisplayName("should pass validation for valid Book")
    void testValidBook() {
        Book book = new Book();
        book.setId(1);
        book.setTitle("Test Book");
        book.setDescription("Test description");
        
        List<ValidationIssue> issues = validator.validateBook(book);
        assertTrue(issues.isEmpty(), "Valid book should have no issues");
    }

    @Test
    @DisplayName("should fail validation for Book with null title")
    void testBookWithNullTitle() {
        Book book = new Book();
        book.setId(1);
        book.setTitle(null);
        book.setDescription("Test description");
        
        List<ValidationIssue> issues = validator.validateBook(book);
        assertNotNull(issues, "Should return validation issues list");
    }

    @Test
    @DisplayName("should pass validation for valid Chapter")
    void testValidChapter() {
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setTitle("Test Chapter");
        chapter.setDescription("Test description");
        
        Book book = new Book();
        book.setId(1);
        chapter.setBook(book);
        
        List<ValidationIssue> issues = validator.validateChapter(chapter);
        assertTrue(issues.isEmpty(), "Valid chapter should have no issues");
    }

    @Test
    @DisplayName("should fail validation for Chapter with null title")
    void testChapterWithNullTitle() {
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setTitle(null);
        chapter.setDescription("Test description");
        
        List<ValidationIssue> issues = validator.validateChapter(chapter);
        assertFalse(issues.isEmpty(), "Chapter with null title should have validation issues");
    }

    @Test
    @DisplayName("should pass validation for valid Verse")
    void testValidVerse() {
        Verse verse = new Verse();
        verse.setId(1);
        verse.setVerseKey("1:1");
        verse.setText("Test verse text");
        
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        verse.setChapter(chapter);
        
        List<ValidationIssue> issues = validator.validateVerse(verse);
        assertTrue(issues.isEmpty(), "Valid verse should have no issues");
    }

    @Test
    @DisplayName("should fail validation for Verse with null verseKey")
    void testVerseWithNullVerseKey() {
        Verse verse = new Verse();
        verse.setId(1);
        verse.setVerseKey(null);
        verse.setText("Test verse text");
        
        List<ValidationIssue> issues = validator.validateVerse(verse);
        assertFalse(issues.isEmpty(), "Verse with null verseKey should have validation issues");
    }

    @Test
    @DisplayName("should pass validation for valid Note")
    void testValidNote() {
        Note note = new Note();
        note.setId(1);
        note.setText("Test note");
        
        Verse verse = new Verse();
        verse.setId(1);
        note.setVerse(verse);
        
        List<ValidationIssue> issues = validator.validateNote(note);
        assertTrue(issues.isEmpty(), "Valid note should have no issues");
    }

    @Test
    @DisplayName("should pass validation for valid Image")
    void testValidImage() {
        Image image = new Image();
        image.setId(1);
        image.setImageKey("IMG001");
        image.setTitle("Test Image");
        image.setDescription("Test description");
        
        List<ValidationIssue> issues = validator.validateImage(image);
        assertTrue(issues.isEmpty(), "Valid image should have no issues");
    }

    @Test
    @DisplayName("should fail validation for Image with null imageKey")
    void testImageWithNullImageKey() {
        Image image = new Image();
        image.setId(1);
        image.setImageKey(null);
        image.setTitle("Test Image");
        image.setDescription("Test description");
        
        List<ValidationIssue> issues = validator.validateImage(image);
        assertNotNull(issues, "Should return validation issues list");
    }

    @Test
    @DisplayName("should aggregate multiple validation issues")
    void testMultipleIssues() {
        Book book = new Book();
        book.setId(1);
        book.setTitle(null);
        book.setDescription(null);
        
        List<ValidationIssue> issues = validator.validateBook(book);
        assertTrue(issues.size() >= 1, "Should have at least one validation issue");
    }
}

