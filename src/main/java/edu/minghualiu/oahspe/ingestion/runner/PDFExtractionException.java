package edu.minghualiu.oahspe.ingestion.runner;

/**
 * Custom exception for PDF extraction errors.
 * Thrown when PDF file operations fail during text extraction.
 *
 * This exception captures context about where and why PDF extraction failed,
 * including the file path, page number, and root cause.
 */
public class PDFExtractionException extends Exception {
    /** The path to the PDF file that caused the error */
    private final String pdfFilePath;

    /** The page number being extracted when error occurred (0 if file-level error) */
    private final int pageNumber;

    /**
     * Constructs a PDFExtractionException with file path and message.
     * Used for file-level errors (e.g., file not found, invalid PDF format).
     *
     * @param pdfFilePath the path to the PDF file
     * @param message description of the error
     */
    public PDFExtractionException(String pdfFilePath, String message) {
        super(String.format("PDF Extraction Error [%s]: %s", pdfFilePath, message));
        this.pdfFilePath = pdfFilePath;
        this.pageNumber = 0;
    }

    /**
     * Constructs a PDFExtractionException with file path, page number, and message.
     * Used for page-level errors during extraction.
     *
     * @param pdfFilePath the path to the PDF file
     * @param pageNumber the page number being processed
     * @param message description of the error
     */
    public PDFExtractionException(String pdfFilePath, int pageNumber, String message) {
        super(String.format("PDF Extraction Error [%s, Page %d]: %s", 
                pdfFilePath, pageNumber, message));
        this.pdfFilePath = pdfFilePath;
        this.pageNumber = pageNumber;
    }

    /**
     * Constructs a PDFExtractionException with file path, message, and cause.
     * Used for file-level errors with underlying cause.
     *
     * @param pdfFilePath the path to the PDF file
     * @param message description of the error
     * @param cause the underlying exception
     */
    public PDFExtractionException(String pdfFilePath, String message, Throwable cause) {
        super(String.format("PDF Extraction Error [%s]: %s", pdfFilePath, message), cause);
        this.pdfFilePath = pdfFilePath;
        this.pageNumber = 0;
    }

    /**
     * Constructs a PDFExtractionException with file path, page number, message, and cause.
     * Used for page-level errors with underlying cause.
     *
     * @param pdfFilePath the path to the PDF file
     * @param pageNumber the page number being processed
     * @param message description of the error
     * @param cause the underlying exception
     */
    public PDFExtractionException(String pdfFilePath, int pageNumber, String message, Throwable cause) {
        super(String.format("PDF Extraction Error [%s, Page %d]: %s", 
                pdfFilePath, pageNumber, message), cause);
        this.pdfFilePath = pdfFilePath;
        this.pageNumber = pageNumber;
    }

    /**
     * Returns the path to the PDF file that caused the error.
     *
     * @return the PDF file path
     */
    public String getPdfFilePath() {
        return pdfFilePath;
    }

    /**
     * Returns the page number where the error occurred.
     *
     * @return the page number (1-indexed), or 0 if file-level error
     */
    public int getPageNumber() {
        return pageNumber;
    }
}
