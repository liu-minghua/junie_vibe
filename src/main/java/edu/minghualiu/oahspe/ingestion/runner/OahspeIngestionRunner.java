package edu.minghualiu.oahspe.ingestion.runner;

import edu.minghualiu.oahspe.entities.Image;
import edu.minghualiu.oahspe.ingestion.parser.OahspeParser;
import edu.minghualiu.oahspe.ingestion.parser.OahspeEvent;
import edu.minghualiu.oahspe.ingestion.OahspeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

/**
 * Main orchestrator service for end-to-end PDF ingestion workflow.
 * Coordinates PDF extraction, parsing, and database ingestion into a unified pipeline.
 *
 * Architecture:
 * The OahspeIngestionRunner implements a three-stage pipeline pattern that brings together
 * the work from previous phases:
 *
 * Stage 1: PDF Text Extraction (Phase 3)
 *   - PDFTextExtractor loads PDF using Apache PDFBox
 *   - Extracts raw text from each page
 *   - Handles file/format errors with PDFExtractionException
 *
 * Stage 2: Text Parsing (Phase 1)
 *   - OahspeParser converts extracted text into OahspeEvent objects
 *   - Recognizes 6 event types: BookStart, ChapterStart, Verse, Note, Image, ImageRef
 *   - Returns structured events ready for database persistence
 *
 * Stage 3: Database Ingestion (Phase 2)
 *   - OahspeIngestionService consumes OahspeEvents
 *   - Creates/persists Book, Chapter, Verse, Note, Image entities
 *   - Manages entity relationships and state transitions
 *
 * Page-by-page processing allows:
 * - Memory-efficient handling of large PDFs
 * - Per-page error tracking and recovery
 * - Progress monitoring via optional ProgressCallback
 * - Clear separation of concerns (extraction → parsing → ingestion)
 *
 * @see PDFTextExtractor
 * @see OahspeParser
 * @see OahspeIngestionService
 * @see ProgressCallback
 * @see IngestionContext
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OahspeIngestionRunner {
    private final PDFTextExtractor pdfExtractor;
    private final PDFImageExtractor imageExtractor;
    private final OahspeParser parser;
    private final OahspeIngestionService ingestionService;

    /**
     * Ingests a PDF file into the database end-to-end.
     * Processes all pages sequentially without progress callbacks.
     *
     * Workflow:
     * 1. Load PDF and get page count
     * 2. For each page:
     *    a. Extract text using PDFBox
     *    b. Parse text into OahspeEvents
     *    c. Ingest events into database
     *    d. Track errors in IngestionContext
     * 3. Return IngestionContext with completion metrics
     *
     * Error handling:
     * - File/PDF errors: Throw PDFExtractionException immediately
     * - Per-page errors: Log warning, add to context.pageErrors, continue
     * - Database errors: Log error, add to context, continue to next page
     *
     * Transaction semantics:
     * Each page's events are ingested in its own transaction (REQUIRES_NEW).
     * If a page fails, only that page's entities are rolled back.
     * The runner continues processing remaining pages.
     *
     * @param pdfFilePath path to the PDF file to ingest
     * @return IngestionContext with completion status and metrics
     * @throws PDFExtractionException if PDF file invalid or inaccessible
     * @see IngestionContext#isSuccessful()
     * @see IngestionContext#getPageErrors()
     */
    public IngestionContext ingestPdf(String pdfFilePath) throws PDFExtractionException {
        return ingestPdfWithProgress(pdfFilePath, null);
    }

    /**
     * Ingests a PDF file with optional progress monitoring.
     * Processes all pages sequentially, calling callback methods at key points.
     *
     * Callback behavior:
     * - onPageStart: Called before each page is processed
     * - onPageComplete: Called after successful page processing
     * - onPageError: Called if a page encounters an error (ingestion continues)
     * - onIngestionComplete: Called after all pages processed
     *
     * Callback exceptions are logged but do not interrupt ingestion.
     *
     * @param pdfFilePath path to the PDF file to ingest
     * @param progressCallback optional callback for progress monitoring (can be null)
     * @return IngestionContext with completion status and metrics
     * @throws PDFExtractionException if PDF file invalid or inaccessible
     */
    @Transactional
    public IngestionContext ingestPdfWithProgress(
            String pdfFilePath,
            ProgressCallback progressCallback) throws PDFExtractionException {

        IngestionContext context = new IngestionContext(
                pdfFilePath,
                pdfExtractor.getPageCount(pdfFilePath)
        );

        log.info("Starting PDF ingestion: {} ({} pages)", pdfFilePath, context.getTotalPages());
        
        // Initialize image counter from database for idempotent restart
        imageExtractor.initializeImageCounter();

        for (int pageNum = 1; pageNum <= context.getTotalPages(); pageNum++) {
            context.setCurrentPageNumber(pageNum);

            // Notify callback of page start
            if (progressCallback != null) {
                try {
                    progressCallback.onPageStart(pageNum, context.getTotalPages());
                } catch (Exception e) {
                    log.warn("Progress callback onPageStart failed", e);
                }
            }

            try {
                processSinglePage(pageNum, context);

                // Notify callback of success
                if (progressCallback != null) {
                    try {
                        progressCallback.onPageComplete(pageNum, 
                                context.getTotalEventsProcessed());
                    } catch (Exception e) {
                        log.warn("Progress callback onPageComplete failed", e);
                    }
                }
            } catch (Exception e) {
                String errorMsg = String.format("Page %d processing failed: %s", 
                        pageNum, e.getMessage());
                context.addPageError(pageNum, e.getMessage());
                log.warn(errorMsg, e);

                // Notify callback of error
                if (progressCallback != null) {
                    try {
                        progressCallback.onPageError(pageNum, e);
                    } catch (Exception callbackErr) {
                        log.warn("Progress callback onPageError failed", callbackErr);
                    }
                }
            }

            // Reset for next book in case multiple books in PDF
            ingestionService.finishIngestion();
        }

        // Notify callback of completion
        if (progressCallback != null) {
            try {
                progressCallback.onIngestionComplete(context);
            } catch (Exception e) {
                log.warn("Progress callback onIngestionComplete failed", e);
            }
        }

        log.info("PDF ingestion complete: {} - Events: {}, Images: {}, Errors: {}, Time: {}ms",
                pdfFilePath,
                context.getTotalEventsProcessed(),
                context.getTotalImagesExtracted(),
                context.getTotalErrorsEncountered(),
                context.getElapsedTime());

        return context;
    }

    /**
     * Processes a single page of the PDF.
     * Internal method that orchestrates the extraction → parsing → ingestion flow.
     *
     * Process:
     * 1. Extract page text using PDFTextExtractor
     * 2. Extract images using PDFImageExtractor
     * 3. Parse text using OahspeParser.parse()
     * 4. Ingest events using OahspeIngestionService.ingestEvents()
     * 5. Update context with event and image counts
     *
     * Note: The ingestEvents() method is transactional, so each page's database
     * operations run in their own transaction automatically.
     *
     * @param pageNumber the page number to process (1-indexed)
     * @param context the IngestionContext to update with progress
     * @throws Exception if extraction, parsing, or ingestion fails
     */
    private void processSinglePage(int pageNumber, IngestionContext context) 
            throws Exception {
        // Stage 1: Extract text
        String pageText = pdfExtractor.extractText(context.getPdfFilePath(), pageNumber);

        // Stage 2: Extract images
        try {
            List<Image> images = imageExtractor.extractImagesFromPage(
                    context.getPdfFilePath(), pageNumber, context);
            context.addExtractedImages(images.size());
            if (!images.isEmpty()) {
                log.debug("Page {} extracted {} images", pageNumber, images.size());
            }
        } catch (Exception e) {
            log.warn("Image extraction failed for page {}: {}", pageNumber, e.getMessage());
            // Continue processing text even if image extraction fails
        }

        if (pageText.isEmpty()) {
            log.debug("Page {} is empty or contains no text", pageNumber);
            return;
        }

        // Stage 3: Parse text into events
        List<String> lines = Arrays.asList(pageText.split("\n"));
        List<OahspeEvent> events = parser.parse(lines, pageNumber);

        if (events.isEmpty()) {
            log.debug("Page {} produced no events", pageNumber);
            return;
        }

        // Stage 4: Ingest events into database
        ingestionService.ingestEvents(events, pageNumber);

        // Update context
        context.setTotalEventsProcessed(context.getTotalEventsProcessed() + events.size());
        log.debug("Page {} processed: {} events", pageNumber, events.size());
    }
}
