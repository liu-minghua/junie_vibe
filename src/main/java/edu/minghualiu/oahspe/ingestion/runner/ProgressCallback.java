package edu.minghualiu.oahspe.ingestion.runner;

/**
 * Callback interface for monitoring PDF ingestion progress.
 * Allows external clients to track processing status and handle completion/errors.
 *
 * Implementations receive notifications at key points during the PDF ingestion pipeline:
 * - Page start: Before processing a page
 * - Page complete: After successfully processing a page
 * - Page error: When a page encounters an error
 * - Ingestion complete: When entire PDF is processed
 *
 * This interface is optional - OahspeIngestionRunner can process PDFs without callbacks.
 *
 * @see OahspeIngestionRunner
 * @see IngestionContext
 */
public interface ProgressCallback {

    /**
     * Called before starting to process a page.
     *
     * @param pageNumber the page number about to be processed (1-indexed)
     * @param totalPages the total number of pages in the PDF
     */
    void onPageStart(int pageNumber, int totalPages);

    /**
     * Called after successfully processing a page.
     *
     * @param pageNumber the page number that was processed (1-indexed)
     * @param eventsProcessed the number of OahspeEvents extracted from this page
     */
    void onPageComplete(int pageNumber, int eventsProcessed);

    /**
     * Called when a page encounters an error during processing.
     * Ingestion may continue on subsequent pages.
     *
     * @param pageNumber the page number that encountered the error (1-indexed)
     * @param exception the exception that occurred during processing
     */
    void onPageError(int pageNumber, Exception exception);

    /**
     * Called when entire PDF ingestion is complete.
     * This is called regardless of whether errors occurred during processing.
     *
     * @param context the final IngestionContext with complete statistics
     *                (use context.isSuccessful() to check for errors)
     */
    void onIngestionComplete(IngestionContext context);
}
