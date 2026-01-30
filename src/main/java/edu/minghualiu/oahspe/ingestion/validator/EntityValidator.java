package edu.minghualiu.oahspe.ingestion.validator;

import org.springframework.stereotype.Component;
import edu.minghualiu.oahspe.entities.*;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityValidator - Individual entity validation logic
 * 
 * Validates individual entity instances against business rules.
 * Each validation rule is specific to entity type:
 * - Book: title required, non-empty
 * - Chapter: number required, positive, book reference valid
 * - Verse: number required, text required, chapter reference valid
 * - Note: content required, verse reference valid
 * - Image: path required, note reference valid (if linked)
 * 
 * Validation Rules:
 * - CRITICAL: Data invalid, must fix before use
 * - ERROR: Data inconsistent, should fix
 * - WARNING: Data suboptimal, nice to fix
 * 
 * This validator focuses on INDIVIDUAL entity rules only.
 * For CROSS-ENTITY validation (relationships), see CrossEntityValidator.
 * 
 * Design Principle: Single Responsibility - validate one entity at a time
 * This makes testing easier and logic clearer.
 * 
 * @author Development Team
 * @version 1.0
 * @since 2026-01-30
 */
@Component
public class EntityValidator {
    
    /**
     * Validate a Book entity
     * 
     * Book rules:
     * - Required: title (non-blank)
     * - Optional: description
     * - Format: title < 500 characters
     * 
     * @param book Book entity to validate
     * @return List of ValidationIssue objects (empty if valid)
     * 
     * Example:
     * {@code
     * List<ValidationIssue> issues = validator.validateBook(book);
     * if (!issues.isEmpty()) {
     *     issues.forEach(i -> System.out.println(i.getMessage()));
     * }
     * }
     */
    public List<ValidationIssue> validateBook(Book book) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        if (book == null) {
            issues.add(new ValidationIssue(
                Severity.CRITICAL,
                "BOOK",
                null,
                "NullBook",
                "Book object is null"
            ));
            return issues;
        }
        
        // Validate title (required and non-empty)
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            issues.add(new ValidationIssue(
                Severity.ERROR,
                "BOOK",
                book.getId().longValue(),
                "MissingTitle",
                "Book is missing required title field",
                "Add title to the book record"
            ));
        } else if (book.getTitle().length() > 500) {
            issues.add(new ValidationIssue(
                Severity.WARNING,
                "BOOK",
                book.getId().longValue(),
                "TitleTooLong",
                "Book title exceeds 500 characters: " + book.getTitle().length(),
                "Shorten title to under 500 characters"
            ));
        }
        
        return issues;
    }
    
    /**
     * Validate a Chapter entity
     * 
     * Chapter rules:
     * - Required: title (non-blank)
     * - Required: book reference (non-null)
     * - Format: title < 500 characters
     * 
     * Note: Book existence is checked in CrossEntityValidator
     * 
     * @param chapter Chapter entity to validate
     * @return List of ValidationIssue objects
     */
    public List<ValidationIssue> validateChapter(Chapter chapter) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        if (chapter == null) {
            issues.add(new ValidationIssue(
                Severity.CRITICAL,
                "CHAPTER",
                null,
                "NullChapter",
                "Chapter object is null"
            ));
            return issues;
        }
        
        // Validate title (required and non-empty)
        if (chapter.getTitle() == null || chapter.getTitle().isBlank()) {
            issues.add(new ValidationIssue(
                Severity.ERROR,
                "CHAPTER",
                chapter.getId(),
                "MissingTitle",
                "Chapter missing valid title",
                "Set chapter title"
            ));
        } else if (chapter.getTitle().length() > 500) {
            issues.add(new ValidationIssue(
                Severity.WARNING,
                "CHAPTER",
                chapter.getId(),
                "TitleTooLong",
                "Chapter title exceeds 500 characters: " + chapter.getTitle().length(),
                "Shorten title to under 500 characters"
            ));
        }
        
        // Validate book reference (required)
        if (chapter.getBook() == null) {
            issues.add(new ValidationIssue(
                Severity.CRITICAL,
                "CHAPTER",
                chapter.getId(),
                "MissingBookReference",
                "Chapter missing required book reference",
                "Link chapter to valid book"
            ));
        }
        
        return issues;
    }
    
    /**
     * Validate a Verse entity
     * 
     * Verse rules:
     * - Required: text (non-blank)
     * - Required: chapter reference (non-null)
     * - Format: text < 5000 characters
     * 
     * Note: Chapter existence is checked in CrossEntityValidator
     * 
     * @param verse Verse entity to validate
     * @return List of ValidationIssue objects
     */
    public List<ValidationIssue> validateVerse(Verse verse) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        if (verse == null) {
            issues.add(new ValidationIssue(
                Severity.CRITICAL,
                "VERSE",
                null,
                "NullVerse",
                "Verse object is null"
            ));
            return issues;
        }
        
        // Validate text (required and non-empty)
        if (verse.getText() == null || verse.getText().isBlank()) {
            issues.add(new ValidationIssue(
                Severity.ERROR,
                "VERSE",
                (long) verse.getId(),
                "MissingText",
                "Verse missing required text content",
                "Add verse text"
            ));
        } else if (verse.getText().length() > 5000) {
            issues.add(new ValidationIssue(
                Severity.WARNING,
                "VERSE",
                (long) verse.getId(),
                "TextTooLong",
                "Verse text exceeds 5000 characters: " + verse.getText().length(),
                "Consider splitting into multiple verses"
            ));
        }
        
        // Validate chapter reference (required)
        if (verse.getChapter() == null) {
            issues.add(new ValidationIssue(
                Severity.CRITICAL,
                "VERSE",
                (long) verse.getId(),
                "MissingChapterReference",
                "Verse missing required chapter reference",
                "Link verse to valid chapter"
            ));
        }
        
        return issues;
    }
    
    /**
     * Validate a Note entity
     * 
     * Note rules:
     * - Required: text (non-blank)
     * - Required: verse reference (nullable, but if present must exist)
     * - Format: text < 10000 characters
     * 
     * Note: Verse existence is checked in CrossEntityValidator
     * 
     * @param note Note entity to validate
     * @return List of ValidationIssue objects
     */
    public List<ValidationIssue> validateNote(Note note) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        if (note == null) {
            issues.add(new ValidationIssue(
                Severity.CRITICAL,
                "NOTE",
                null,
                "NullNote",
                "Note object is null"
            ));
            return issues;
        }
        
        // Validate text (required and non-empty)
        if (note.getText() == null || note.getText().isBlank()) {
            issues.add(new ValidationIssue(
                Severity.ERROR,
                "NOTE",
                (long) note.getId(),
                "MissingText",
                "Note missing required text content",
                "Add text to the note"
            ));
        } else if (note.getText().length() > 10000) {
            issues.add(new ValidationIssue(
                Severity.WARNING,
                "NOTE",
                (long) note.getId(),
                "TextTooLong",
                "Note text exceeds 10000 characters: " + note.getText().length(),
                "Consider splitting into multiple notes"
            ));
        }
        
        return issues;
    }
    
    /**
     * Validate an Image entity
     * 
     * Image rules:
     * - Required: title (non-blank)
     * - Required: description (non-blank)
     * - Optional: note references (many-to-many)
     * - Format: title < 500 characters, description < 1000 characters
     * 
     * Note: Note existence is checked in CrossEntityValidator
     * 
     * @param image Image entity to validate
     * @return List of ValidationIssue objects
     */
    public List<ValidationIssue> validateImage(Image image) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        if (image == null) {
            issues.add(new ValidationIssue(
                Severity.CRITICAL,
                "IMAGE",
                null,
                "NullImage",
                "Image object is null"
            ));
            return issues;
        }
        
        // Validate title (required and non-empty)
        if (image.getTitle() == null || image.getTitle().isBlank()) {
            issues.add(new ValidationIssue(
                Severity.ERROR,
                "IMAGE",
                (long) image.getId(),
                "MissingTitle",
                "Image missing required title",
                "Add title for the image"
            ));
        } else if (image.getTitle().length() > 500) {
            issues.add(new ValidationIssue(
                Severity.WARNING,
                "IMAGE",
                (long) image.getId(),
                "TitleTooLong",
                "Image title exceeds 500 characters: " + image.getTitle().length(),
                "Shorten title"
            ));
        }
        
        // Validate description (required and non-empty)
        if (image.getDescription() == null || image.getDescription().isBlank()) {
            issues.add(new ValidationIssue(
                Severity.ERROR,
                "IMAGE",
                (long) image.getId(),
                "MissingDescription",
                "Image missing required description",
                "Add description for the image"
            ));
        } else if (image.getDescription().length() > 1000) {
            issues.add(new ValidationIssue(
                Severity.WARNING,
                "IMAGE",
                (long) image.getId(),
                "DescriptionTooLong",
                "Image description exceeds 1000 characters: " + image.getDescription().length(),
                "Shorten description"
            ));
        }
        
        return issues;
    }
}
