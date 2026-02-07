package edu.minghualiu.oahspe.ingestion.linker;

import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.enums.PageCategory;
import edu.minghualiu.oahspe.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for linking ingested content entities to their source pages.
 * Populates pageNumber field on Book, Chapter, Verse, and Note entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentPageLinkingService {
    
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final VerseRepository verseRepository;
    private final NoteRepository noteRepository;
    private final PageContentRepository pageContentRepository;
    
    /**
     * Links all content entities to their source pages.
     * Analyzes content to determine which page it was extracted from.
     * 
     * Note: This is a best-effort linking. Since entities were created
     * during ingestion with pageNumber already set (in Phase 7.5), this
     * service primarily validates and reports on linking status.
     * 
     * @return report with linking statistics
     */
    @Transactional(readOnly = true)
    public ContentLinkingReport linkAllContentToPages() {
        log.info("Starting content-page linking analysis");
        
        ContentLinkingReport report = new ContentLinkingReport();
        
        // Count entities with and without pageNumber
        long booksWithPage = bookRepository.count() - bookRepository.countByPageNumberIsNull();
        long booksWithoutPage = bookRepository.countByPageNumberIsNull();
        
        long chaptersWithPage = chapterRepository.count() - chapterRepository.countByPageNumberIsNull();
        long chaptersWithoutPage = chapterRepository.countByPageNumberIsNull();
        
        long versesWithPage = verseRepository.count() - verseRepository.countByPageNumberIsNull();
        long versesWithoutPage = verseRepository.countByPageNumberIsNull();
        
        long notesWithPage = noteRepository.count() - noteRepository.countByPageNumberIsNull();
        long notesWithoutPage = noteRepository.countByPageNumberIsNull();
        
        report.setTotalPages((int) pageContentRepository.count());
        report.setPagesLinked((int) pageContentRepository.countByCategoryAndIngestedTrue(PageCategory.OAHSPE_BOOKS));
        
        report.setBooksWithPageNumber((int) booksWithPage);
        report.setBooksWithoutPageNumber((int) booksWithoutPage);
        
        report.setChaptersWithPageNumber((int) chaptersWithPage);
        report.setChaptersWithoutPageNumber((int) chaptersWithoutPage);
        
        report.setVersesWithPageNumber((int) versesWithPage);
        report.setVersesWithoutPageNumber((int) versesWithoutPage);
        
        report.setNotesWithPageNumber((int) notesWithPage);
        report.setNotesWithoutPageNumber((int) notesWithoutPage);
        
        log.info("Content linking analysis complete");
        log.info(report.getSummary());
        
        return report;
    }
    
    /**
     * Finds all pages that have been loaded but not linked to any content.
     * 
     * @return list of page summaries
     */
    public List<PageContentSummary> findUnlinkedPages() {
        List<PageContentSummary> summaries = new ArrayList<>();
        
        List<PageContent> unlinkedPages = pageContentRepository.findByIngestedFalseOrderByPageNumberAsc();
        
        for (PageContent page : unlinkedPages) {
            // Only report pages that should have been ingested
            if (!page.getCategory().shouldIngest()) {
                continue;
            }
            
            PageContentSummary summary = PageContentSummary.builder()
                    .pageNumber(page.getPageNumber())
                    .category(page.getCategory())
                    .hasText(page.getRawText() != null && !page.getRawText().isEmpty())
                    .imageCount(0) // Would need PageImageRepository to count
                    .ingested(page.getIngested())
                    .hasError(page.hasError())
                    .errorMessage(page.getErrorMessage())
                    .build();
            
            summaries.add(summary);
        }
        
        return summaries;
    }
    
    /**
     * Finds all verses that were created but don't have a pageNumber assigned.
     * These are likely orphaned verses that appeared before any book/chapter structure.
     * 
     * @return list of orphaned verses
     */
    public List<Verse> findOrphanedVerses() {
        // Query verses where pageNumber is null
        List<Verse> allVerses = verseRepository.findAll();
        
        return allVerses.stream()
                .filter(v -> v.getPageNumber() == null)
                .toList();
    }
    
    /**
     * Finds all books without pageNumber.
     * 
     * @return list of books without page links
     */
    public List<Book> findBooksWithoutPageNumber() {
        List<Book> allBooks = bookRepository.findAll();
        
        return allBooks.stream()
                .filter(b -> b.getPageNumber() == null)
                .toList();
    }
    
    /**
     * Finds all chapters without pageNumber.
     * 
     * @return list of chapters without page links
     */
    public List<Chapter> findChaptersWithoutPageNumber() {
        List<Chapter> allChapters = chapterRepository.findAll();
        
        return allChapters.stream()
                .filter(c -> c.getPageNumber() == null)
                .toList();
    }
}
