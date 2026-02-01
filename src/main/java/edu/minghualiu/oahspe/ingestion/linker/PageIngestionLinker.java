package edu.minghualiu.oahspe.ingestion.linker;

import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.ingestion.OahspeIngestionService;
import edu.minghualiu.oahspe.ingestion.parser.GlossaryParser;
import edu.minghualiu.oahspe.ingestion.parser.IndexParser;
import edu.minghualiu.oahspe.ingestion.parser.OahspeEvent;
import edu.minghualiu.oahspe.ingestion.parser.OahspeParser;
import edu.minghualiu.oahspe.ingestion.runner.IngestionContext;
import edu.minghualiu.oahspe.ingestion.runner.ProgressCallback;
import edu.minghualiu.oahspe.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service for ingesting PageContent into domain entities.
 * Phase 3 of the page-based ingestion workflow.
 * 
 * Routes pages to appropriate parsers based on category:
 * - GLOSSARIES → GlossaryParser
 * - INDEX → IndexParser  
 * - OAHSPE_BOOKS → OahspeParser
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageIngestionLinker {
    
    private final PageContentRepository pageContentRepository;
    private final OahspeParser oahspeParser;
    private final GlossaryParser glossaryParser;
    private final IndexParser indexParser;
    private final OahspeIngestionService oahspeIngestionService;
    private final GlossaryTermRepository glossaryTermRepository;
    private final IndexEntryRepository indexEntryRepository;
    private final PageImageRepository pageImageRepository;
    private final ImageRepository imageRepository;
    
    /**
     * Ingests all PageContent entities that should be ingested.
     * 
     * @param callback optional progress callback
     * @return ingestion context with statistics
     */
    public IngestionContext ingestAllPageContents(ProgressCallback callback) {
        log.info("Starting content ingestion from PageContent entities");
        
        // Reset parser state at start of ingestion
        oahspeParser.resetState();
        
        List<PageContent> allPages = pageContentRepository.findByIngestedFalseOrderByPageNumberAsc();
        
        // Filter to only pages that should be ingested
        List<PageContent> pagesToIngest = allPages.stream()
                .filter(pc -> pc.getCategory().shouldIngest())
                .toList();
        
        IngestionContext context = new IngestionContext();
        context.setTotalPages(pagesToIngest.size());
        
        for (PageContent pageContent : pagesToIngest) {
            context.setCurrentPageNumber(pageContent.getPageNumber());
            
            try {
                if (callback != null && pageContent.getPageNumber() % 50 == 0) {
                    callback.onPageStart(pageContent.getPageNumber(), pagesToIngest.size());
                }
                
                ingestSinglePageContent(pageContent, context);
                
                if (callback != null && pageContent.getPageNumber() % 50 == 0) {
                    callback.onPageComplete(pageContent.getPageNumber(), 1);
                }
            } catch (Exception e) {
                context.addPageError(pageContent.getPageNumber(), e.getMessage());
                pageContent.markError(e.getMessage());
                pageContentRepository.save(pageContent);
                log.error("Failed to ingest page {}: {}", 
                        pageContent.getPageNumber(), e.getMessage(), e);
            }
        }
        
        log.info("Content ingestion complete. Pages ingested: {}, Errors: {}", 
                pagesToIngest.size() - context.getTotalErrorsEncountered(),
                context.getTotalErrorsEncountered());
        
        return context;
    }
    
    /**
     * Ingests a single PageContent entity.
     * Routes to appropriate parser based on category.
     * Special handling for page 1668: splits at horizontal line separator 
     * to process book content (above) and glossary content (below) separately.
     * Uses REQUIRES_NEW to isolate each page's transaction.
     * 
     * @param pageContent the page to ingest
     * @param context the ingestion context
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void ingestSinglePageContent(PageContent pageContent, IngestionContext context) {
        log.debug("Ingesting page {} [{}]", 
                pageContent.getPageNumber(), pageContent.getCategory());
        
        int pageNumber = pageContent.getPageNumber();
        PageCategory category = pageContent.getCategory();
        String rawText = pageContent.getRawText();
        
        // Special case: Page 1668 has both book content (above horizontal line) and glossary content (below)
        if (pageNumber == 1668) {
            log.info("Page 1668: Splitting at horizontal line separator");
            
            // Split at horizontal line (typically 5+ underscores or dashes)
            String[] parts = rawText.split("_{5,}|-{5,}");
            
            if (parts.length >= 2) {
                String bookContent = parts[0];      // Above horizontal line
                String glossaryContent = parts[1];  // Below horizontal line
                
                log.info("Page 1668: Split successful - processing book content ({} chars) and glossary content ({} chars) separately", 
                        bookContent.length(), glossaryContent.length());
                
                ingestOahspePage(bookContent, pageNumber, context);
                ingestGlossaryPage(glossaryContent, pageNumber, context);
            } else {
                // Fallback: if split fails, process full text with both parsers
                log.warn("Page 1668: Could not find horizontal line separator, processing full text with both parsers");
                ingestOahspePage(rawText, pageNumber, context);
                ingestGlossaryPage(rawText, pageNumber, context);
            }
            // Fall through to linkage and marking steps below
        } else {
            switch (category) {
                case GLOSSARIES:
                    ingestGlossaryPage(rawText, pageNumber, context);
                    break;
                    
                case INDEX:
                    ingestIndexPage(rawText, pageNumber, context);
                    break;
                    
                case OAHSPE_BOOKS:
                    ingestOahspePage(rawText, pageNumber, context);
                    break;
                    
                default:
                    log.warn("Page {} has category {} which should not be ingested", 
                            pageNumber, category);
                    return;
            }
        }
        
        // Link PageImages to Image entities
        linkPageImagesToImageEntities(pageContent);
        
        // Mark page as ingested
        pageContent.markIngested();
        pageContentRepository.save(pageContent);
        
        context.setTotalEventsProcessed(context.getTotalEventsProcessed() + 1);
    }
    
    /**
     * Ingests all pages in a specific category.
     * 
     * @param category the category to ingest
     * @param callback optional progress callback
     * @return ingestion context
     */
    @Transactional
    public IngestionContext ingestCategoryPages(PageCategory category, ProgressCallback callback) {
        log.info("Ingesting pages for category: {}", category);
        
        List<PageContent> pages = pageContentRepository
                .findByCategoryAndIngestedFalseOrderByPageNumberAsc(category);
        
        IngestionContext context = new IngestionContext();
        context.setTotalPages(pages.size());
        
        for (PageContent pageContent : pages) {
            try {
                if (callback != null && pageContent.getPageNumber() % 50 == 0) {
                    callback.onPageStart(pageContent.getPageNumber(), pages.size());
                }
                
                ingestSinglePageContent(pageContent, context);
                
                if (callback != null && pageContent.getPageNumber() % 50 == 0) {
                    callback.onPageComplete(pageContent.getPageNumber(), 1);
                }
            } catch (Exception e) {
                context.addPageError(pageContent.getPageNumber(), e.getMessage());
                log.error("Failed to ingest page {}: {}", 
                        pageContent.getPageNumber(), e.getMessage(), e);
            }
        }
        
        return context;
    }
    
    /**
     * Ingests a glossary page using GlossaryParser.
     */
    private void ingestGlossaryPage(String rawText, int pageNumber, IngestionContext context) {
        List<GlossaryTerm> terms = glossaryParser.parseGlossaryPage(rawText, pageNumber);
        
        for (GlossaryTerm term : terms) {
            try {
                // Check if term already exists (avoid duplicates)
                glossaryTermRepository.findByTerm(term.getTerm())
                        .ifPresentOrElse(
                                existing -> log.debug("Glossary term already exists: {}", 
                                        term.getTerm()),
                                () -> {
                                    glossaryTermRepository.save(term);
                                    log.debug("Saved glossary term: {}", term.getTerm());
                                }
                        );
            } catch (Exception e) {
                log.warn("Failed to save glossary term {}: {}", 
                        term.getTerm(), e.getMessage());
            }
        }
        
        context.setTotalEventsProcessed(context.getTotalEventsProcessed() + terms.size());
    }
    
    /**
     * Ingests an index page using IndexParser.
     */
    private void ingestIndexPage(String rawText, int pageNumber, IngestionContext context) {
        List<IndexEntry> entries = indexParser.parseIndexPage(rawText, pageNumber);
        
        for (IndexEntry entry : entries) {
            try {
                // Try to link to glossary term if exists
                glossaryTermRepository.findByTerm(entry.getTopic())
                        .ifPresent(entry::setGlossaryTerm);
                
                indexEntryRepository.save(entry);
                log.debug("Saved index entry: {}", entry.getTopic());
                
            } catch (Exception e) {
                log.warn("Failed to save index entry {}: {}", 
                        entry.getTopic(), e.getMessage());
            }
        }
        
        context.setTotalEventsProcessed(context.getTotalEventsProcessed() + entries.size());
    }
    
    /**
     * Ingests an Oahspe book page using OahspeParser.
     */
    private void ingestOahspePage(String rawText, int pageNumber, IngestionContext context) {
        // Split by any combination of \r and \n
        List<String> lines = Arrays.asList(rawText.split("\\r?\\n"));
        List<OahspeEvent> events = oahspeParser.parse(lines, pageNumber);
        
        // Ingest events through existing service (will be enhanced in Task 7.5)
        oahspeIngestionService.ingestEvents(events, pageNumber);
        
        context.setTotalEventsProcessed(context.getTotalEventsProcessed() + events.size());
    }
    
    /**
     * Links PageImage entities to their corresponding Image entities.
     * Updates the linkedImageId field.
     * 
     * @param pageContent the page whose images should be linked
     */
    public void linkPageImagesToImageEntities(PageContent pageContent) {
        List<PageImage> pageImages = pageImageRepository
                .findByPageContentId(pageContent.getId());
        
        for (PageImage pageImage : pageImages) {
            // Find corresponding Image entity by source page and sequence
            // This assumes Image entities have already been created during initial ingestion
            List<Image> matchingImages = imageRepository.findAll().stream()
                    .filter(img -> img.getSourcePage() != null && 
                                   img.getSourcePage().equals(pageContent.getPageNumber()))
                    .toList();
            
            if (!matchingImages.isEmpty()) {
                // Link to first matching image (could be enhanced with better matching logic)
                pageImage.setLinkedImage(matchingImages.get(0));
                pageImageRepository.save(pageImage);
                log.debug("Linked PageImage {} to Image {}", 
                        pageImage.getId(), matchingImages.get(0).getId());
            }
        }
    }
}
