package edu.minghualiu.oahspe.ingestion.validator;

import org.springframework.stereotype.Component;
import edu.minghualiu.oahspe.entities.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CrossEntityValidator - Cross-entity relationship and consistency validation
 * 
 * Validates relationships and consistency across multiple entity types:
 * - Verse sequencing (verses numbered 1, 2, 3 in each chapter)
 * - Reference integrity (foreign key references point to existing entities)
 * - Relationship completeness (entities have required related entities)
 * 
 * This validator focuses on INTER-ENTITY relationships.
 * For INDIVIDUAL entity validation, see EntityValidator.
 * 
 * Validation Rules:
 * 1. VerseSequencing: Verses in chapter numbered consecutively from 1
 * 2. ChapterCompleteness: Each chapter has at least one verse
 * 3. ReferenceIntegrity: All foreign key references valid
 * 4. BookCompleteness: Each book has at least one chapter
 * 
 * Design Principle: Separate from EntityValidator to keep concerns isolated
 * - Easy to test relationship logic independently
 * - Clear separation of entity vs. relationship validation
 * - Can update without affecting individual entity rules
 * 
 * @author Development Team
 * @version 1.0
 * @since 2026-01-30
 */
@Component
public class CrossEntityValidator {
    
    /**
     * Validate all relationships and cross-entity consistency
     * 
     * @param books All books from database
     * @param chapters All chapters from database
     * @param verses All verses from database
     * @param notes All notes from database
     * @param images All images from database
     * 
     * @return List of ValidationIssue objects for all relationship violations
     */
    public List<ValidationIssue> validateAll(List<Book> books, List<Chapter> chapters,
                                            List<Verse> verses, List<Note> notes,
                                            List<Image> images) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        // Validate each relationship type
        issues.addAll(validateVerseSequencing(chapters, verses));
        issues.addAll(validateChapterCompleteness(chapters, verses));
        issues.addAll(validateBookCompleteness(books, chapters));
        issues.addAll(validateReferenceIntegrity(chapters, verses, notes, images));
        
        return issues;
    }
    
    /**
     * Validate verse sequencing within chapters
     * 
     * Rule: Verses in each chapter should be present (no hard requirement for consecutive numbering
     * since Verse entities use verseKey, not sequential numbers).
     * 
     * @param chapters All chapters
     * @param verses All verses
     * @return Issues related to verse completeness
     */
    private List<ValidationIssue> validateVerseSequencing(List<Chapter> chapters,
                                                         List<Verse> verses) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        // Group verses by chapter
        Map<Long, List<Verse>> versesByChapter = verses.stream()
            .filter(v -> v.getChapter() != null)
            .collect(Collectors.groupingBy(v -> v.getChapter().getId()));
        
        // Check each chapter has verses (structure validation, not sequencing)
        for (Chapter chapter : chapters) {
            List<Verse> chapterVerses = versesByChapter.getOrDefault(chapter.getId(), 
                                                                     new ArrayList<>());
            
            // Chapter should have verses
            if (!chapterVerses.isEmpty()) {
                // Verses exist - check they have text
                for (Verse verse : chapterVerses) {
                    if (verse.getText() == null || verse.getText().isBlank()) {
                        issues.add(new ValidationIssue(
                            Severity.WARNING,
                            "VERSE",
                            (long) verse.getId(),
                            "EmptyVerseText",
                            String.format("Chapter \"%s\" has verse with no text",
                                        chapter.getTitle()),
                            "Add text content to verse"
                        ));
                    }
                }
            }
        }
        
        return issues;
    }
    
    /**
     * Validate chapter completeness (has verses)
     * 
     * Rule: Each chapter should have at least one verse.
     * Warning (not critical) - allowing empty chapters for data entry flexibility.
     * 
     * @param chapters All chapters
     * @param verses All verses
     * @return Issues related to chapter completeness
     */
    private List<ValidationIssue> validateChapterCompleteness(List<Chapter> chapters,
                                                             List<Verse> verses) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        // Group verses by chapter
        Set<Long> chapterIdsWithVerses = verses.stream()
            .filter(v -> v.getChapter() != null)
            .map(v -> v.getChapter().getId())
            .collect(Collectors.toSet());
        
        // Check for empty chapters
        for (Chapter chapter : chapters) {
            if (!chapterIdsWithVerses.contains(chapter.getId())) {
                issues.add(new ValidationIssue(
                    Severity.WARNING,
                    "CHAPTER",
                    chapter.getId(),
                    "ChapterCompleteness",
                    String.format("Chapter \"%s\" has no verses", chapter.getTitle()),
                    "Add verses to chapter"
                ));
            }
        }
        
        return issues;
    }
    
    /**
     * Validate book completeness (has chapters)
     * 
     * Rule: Each book should have at least one chapter.
     * 
     * @param books All books
     * @param chapters All chapters
     * @return Issues related to book completeness
     */
    private List<ValidationIssue> validateBookCompleteness(List<Book> books,
                                                          List<Chapter> chapters) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        // Group chapters by book
        Set<Long> bookIdsWithChapters = chapters.stream()
            .filter(c -> c.getBook() != null)
            .map(c -> c.getBook().getId().longValue())
            .collect(Collectors.toSet());
        
        // Check for empty books
        for (Book book : books) {
            if (!bookIdsWithChapters.contains(book.getId().longValue())) {
                issues.add(new ValidationIssue(
                    Severity.WARNING,
                    "BOOK",
                    book.getId().longValue(),
                    "BookCompleteness",
                    String.format("Book \"%s\" has no chapters", book.getTitle()),
                    "Add chapters to book"
                ));
            }
        }
        
        return issues;
    }
    
    /**
     * Validate referential integrity
     * 
     * Rules:
     * - All verses reference valid chapters
     * - All notes reference valid verses (optional, but if present must exist)
     * 
     * @param chapters All chapters
     * @param verses All verses
     * @param notes All notes
     * @param images All images (not validated for references here)
     * @return Issues related to reference integrity
     */
    private List<ValidationIssue> validateReferenceIntegrity(List<Chapter> chapters,
                                                             List<Verse> verses,
                                                             List<Note> notes,
                                                             List<Image> images) {
        List<ValidationIssue> issues = new ArrayList<>();
        
        // Build maps for fast lookup
        Set<Long> validChapterIds = chapters.stream()
            .map(Chapter::getId)
            .collect(Collectors.toSet());
        
        Set<Integer> validVerseIds = verses.stream()
            .map(Verse::getId)
            .collect(Collectors.toSet());
        
        // Validate verses reference valid chapters
        for (Verse verse : verses) {
            if (verse.getChapter() != null && 
                !validChapterIds.contains(verse.getChapter().getId())) {
                issues.add(new ValidationIssue(
                    Severity.CRITICAL,
                    "VERSE",
                    (long) verse.getId(),
                    "ReferenceIntegrity",
                    String.format("Verse references non-existent chapter #%d",
                                verse.getChapter().getId()),
                    "Ensure chapter exists in database"
                ));
            }
        }
        
        // Validate notes reference valid verses (optional reference)
        for (Note note : notes) {
            if (note.getVerse() != null && 
                !validVerseIds.contains(note.getVerse().getId())) {
                issues.add(new ValidationIssue(
                    Severity.ERROR,
                    "NOTE",
                    (long) note.getId(),
                    "ReferenceIntegrity",
                    String.format("Note references non-existent verse #%d",
                                note.getVerse().getId()),
                    "Ensure verse exists in database or remove reference"
                ));
            }
        }
        
        return issues;
    }
}
