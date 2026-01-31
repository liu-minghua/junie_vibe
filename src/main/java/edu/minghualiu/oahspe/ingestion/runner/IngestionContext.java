package edu.minghualiu.oahspe.ingestion.runner;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Context holder for an ongoing PDF ingestion session.
 * Tracks progress, statistics, and errors during page-by-page processing.
 *
 * This class maintains state across multiple pages and serves as the output
 * of the OahspeIngestionRunner, providing completion metrics and diagnostics.
 *
 * @see OahspeIngestionRunner
 */
@Getter
@Setter
public class IngestionContext {
    /** Path to the PDF file being ingested */
    private String pdfFilePath;

    /** Total number of pages in the PDF */
    private int totalPages;

    /** Current page being processed (1-indexed) */
    private int currentPageNumber;

    /** Timestamp when ingestion started (ms since epoch) */
    private long startTime;

    /** Total number of OahspeEvents processed across all pages */
    private int totalEventsProcessed;

    /** Total number of errors encountered during ingestion */
    private int totalErrorsEncountered;

    /** Total number of images extracted across all pages */
    private int totalImagesExtracted;

    /** List of error messages per page (format: "Page N: error description") */
    private List<String> pageErrors;

    /**
     * Constructs an empty IngestionContext.
     * Fields should be populated before/during ingestion process.
     */
    public IngestionContext() {
        this.pageErrors = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
        this.totalEventsProcessed = 0;
        this.totalErrorsEncountered = 0;
        this.totalImagesExtracted = 0;
    }

    /**
     * Constructs an IngestionContext with PDF file path and total pages.
     *
     * @param pdfFilePath the path to the PDF being ingested
     * @param totalPages the total number of pages in the PDF
     */
    public IngestionContext(String pdfFilePath, int totalPages) {
        this();
        this.pdfFilePath = pdfFilePath;
        this.totalPages = totalPages;
    }

    /**
     * Adds an error message for a specific page.
     *
     * @param pageNumber the page number where the error occurred
     * @param errorMessage description of the error
     */
    public void addPageError(int pageNumber, String errorMessage) {
        pageErrors.add(String.format("Page %d: %s", pageNumber, errorMessage));
        totalErrorsEncountered++;
    }

    /**
     * Adds to the count of extracted images.
     *
     * @param count the number of images extracted from current page
     */
    public void addExtractedImages(int count) {
        this.totalImagesExtracted += count;
    }

    /**
     * Returns elapsed time in milliseconds since ingestion started.
     *
     * @return elapsed milliseconds
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Returns whether ingestion completed without errors.
     *
     * @return true if totalErrorsEncountered == 0, false otherwise
     */
    public boolean isSuccessful() {
        return totalErrorsEncountered == 0;
    }

    /**
     * Returns a formatted string representation of the ingestion context.
     * Useful for logging progress and debugging.
     *
     * @return multi-line string with ingestion statistics
     */
    @Override
    public String toString() {
        long elapsedMs = getElapsedTime();
        long seconds = elapsedMs / 1000;
        return String.format(
                "IngestionContext {%n" +
                        "  pdfFilePath: %s%n" +
                        "  totalPages: %d%n" +
                        "  currentPage: %d%n" +
                        "  totalEvents: %d%n" +
                        "  totalImages: %d%n" +
                        "  totalErrors: %d%n" +
                        "  success: %s%n" +
                        "  elapsedTime: %d ms (%d sec)%n" +
                        "  pageErrors: %d entries%n" +
                        "}",
                pdfFilePath,
                totalPages,
                currentPageNumber,
                totalEventsProcessed,
                totalImagesExtracted,
                totalErrorsEncountered,
                isSuccessful(),
                elapsedMs,
                seconds,
                pageErrors.size()
        );
    }
}
