package edu.minghualiu.oahspe.ingestion.workflow;

import edu.minghualiu.oahspe.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for cleaning up ingested data before re-ingestion.
 * Phase 2 of the workflow - removes old Books/Chapters/Verses/Notes/Images/Glossary/Index
 * while preserving PageContent and PageImage entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionDataCleanup {
    
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final VerseRepository verseRepository;
    private final NoteRepository noteRepository;
    private final ImageRepository imageRepository;
    private final GlossaryTermRepository glossaryTermRepository;
    private final IndexEntryRepository indexEntryRepository;
    private final PageContentRepository pageContentRepository;
    private final PageImageRepository pageImageRepository;
    
    /**
     * Deletes all ingested domain entities.
     * Preserves PageContent and PageImage for re-ingestion.
     * 
     * WARNING: This is destructive! All books, chapters, verses, notes, 
     * glossary terms, and index entries will be permanently deleted.
     */
    @Transactional
    public void cleanupAllIngestedData() {
        log.warn("=== STARTING DATA CLEANUP ===");
        log.warn("This will delete all ingested content!");
        
        // Delete in order to respect foreign key constraints
        
        // 1. Delete Notes (has FK to Verse and Images)
        long noteCount = noteRepository.count();
        if (noteCount > 0) {
            noteRepository.deleteAll();
            log.info("Deleted {} notes", noteCount);
        }
        
        // 2. Delete Verses (has FK to Chapter)
        long verseCount = verseRepository.count();
        if (verseCount > 0) {
            verseRepository.deleteAll();
            log.info("Deleted {} verses", verseCount);
        }
        
        // 3. Delete Chapters (has FK to Book)
        long chapterCount = chapterRepository.count();
        if (chapterCount > 0) {
            chapterRepository.deleteAll();
            log.info("Deleted {} chapters", chapterCount);
        }
        
        // 4. Delete Books
        long bookCount = bookRepository.count();
        if (bookCount > 0) {
            bookRepository.deleteAll();
            log.info("Deleted {} books", bookCount);
        }
        
        // 5. Delete Images (orphaned after notes deleted)
        long imageCount = imageRepository.count();
        if (imageCount > 0) {
            imageRepository.deleteAll();
            log.info("Deleted {} images", imageCount);
        }
        
        // 6. Delete Glossary Terms
        long glossaryCount = glossaryTermRepository.count();
        if (glossaryCount > 0) {
            glossaryTermRepository.deleteAll();
            log.info("Deleted {} glossary terms", glossaryCount);
        }
        
        // 7. Delete Index Entries
        long indexCount = indexEntryRepository.count();
        if (indexCount > 0) {
            indexEntryRepository.deleteAll();
            log.info("Deleted {} index entries", indexCount);
        }
        
        // Verify PageContent is preserved
        preservePageContents();
        
        log.warn("=== CLEANUP COMPLETE ===");
        log.info("Summary: Deleted {} books, {} chapters, {} verses, {} notes, {} images, {} glossary terms, {} index entries",
                bookCount, chapterCount, verseCount, noteCount, imageCount, glossaryCount, indexCount);
    }
    
    /**
     * Verifies that PageContent and PageImage entities are preserved after cleanup.
     * Logs warnings if they were unexpectedly deleted.
     */
    public void preservePageContents() {
        long pageContentCount = pageContentRepository.count();
        long pageImageCount = pageImageRepository.count();
        
        if (pageContentCount == 0) {
            log.error("⚠ WARNING: PageContent table is empty! This should not happen during cleanup.");
        } else {
            log.info("✓ PageContent preserved: {} pages", pageContentCount);
        }
        
        if (pageImageCount == 0) {
            log.warn("PageImage table is empty (may be expected if no images were loaded yet)");
        } else {
            log.info("✓ PageImage preserved: {} images", pageImageCount);
        }
    }
    
    /**
     * Deletes only content entities (Books/Chapters/Verses/Notes/Images).
     * Preserves Glossary and Index for reference.
     */
    @Transactional
    public void cleanupContentOnly() {
        log.info("Cleaning up content entities only (preserving Glossary and Index)");
        
        long noteCount = noteRepository.count();
        noteRepository.deleteAll();
        
        long verseCount = verseRepository.count();
        verseRepository.deleteAll();
        
        long chapterCount = chapterRepository.count();
        chapterRepository.deleteAll();
        
        long bookCount = bookRepository.count();
        bookRepository.deleteAll();
        
        long imageCount = imageRepository.count();
        imageRepository.deleteAll();
        
        log.info("Content cleanup complete: {} books, {} chapters, {} verses, {} notes, {} images deleted",
                bookCount, chapterCount, verseCount, noteCount, imageCount);
    }
}
