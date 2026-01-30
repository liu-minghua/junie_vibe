package edu.minghualiu.oahspe.ingestion.runner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF text extraction component using Apache PDFBox.
 * Handles all PDFBox-specific operations for extracting text from PDF files.
 *
 * This component encapsulates PDFBox logic to extract text from PDF pages
 * and provides a simple, exception-based API for PDF operations.
 *
 * Features:
 * - Per-page text extraction
 * - Page count retrieval
 * - Bulk extraction of all pages
 * - Proper resource management (PDDocument closing)
 * - Clear error messages for file/format issues
 *
 * @see PDFExtractionException
 * @see IngestionContext
 */
@Component
public class PDFTextExtractor {

    /**
     * Extracts text from a specific page of a PDF file.
     * Page numbers are 1-indexed (user-friendly).
     *
     * @param pdfFilePath the path to the PDF file
     * @param pageNumber the page number to extract (1-indexed)
     * @return the extracted text from the page, or empty string if page is blank
     * @throws PDFExtractionException if file not found, invalid PDF, or extraction fails
     */
    public String extractText(String pdfFilePath, int pageNumber) throws PDFExtractionException {
        File file = new File(pdfFilePath);
        if (!file.exists()) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    String.format("File not found: %s", pdfFilePath)
            );
        }

        try (PDDocument document = PDDocument.load(file)) {
            if (pageNumber < 1 || pageNumber > document.getNumberOfPages()) {
                throw new PDFExtractionException(
                        pdfFilePath,
                        pageNumber,
                        String.format("Page number out of range. Document has %d pages.", 
                                document.getNumberOfPages())
                );
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);

            return stripper.getText(document).trim();
        } catch (PDFExtractionException e) {
            throw e;
        } catch (IOException e) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    pageNumber,
                    "Failed to extract text from page",
                    e
            );
        } catch (Exception e) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    pageNumber,
                    "Unexpected error during PDF text extraction",
                    e
            );
        }
    }

    /**
     * Returns the total number of pages in a PDF file.
     *
     * @param pdfFilePath the path to the PDF file
     * @return the number of pages in the PDF
     * @throws PDFExtractionException if file not found, invalid PDF, or operation fails
     */
    public int getPageCount(String pdfFilePath) throws PDFExtractionException {
        File file = new File(pdfFilePath);
        if (!file.exists()) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    String.format("File not found: %s", pdfFilePath)
            );
        }

        try (PDDocument document = PDDocument.load(file)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    "Failed to load PDF document",
                    e
            );
        } catch (Exception e) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    "Unexpected error reading PDF page count",
                    e
            );
        }
    }

    /**
     * Extracts text from all pages of a PDF file.
     * Returns a list of strings, one per page. Empty pages return empty strings.
     *
     * @param pdfFilePath the path to the PDF file
     * @return a list of page texts (index 0 = page 1)
     * @throws PDFExtractionException if file not found, invalid PDF, or extraction fails
     */
    public List<String> extractAllPages(String pdfFilePath) throws PDFExtractionException {
        int pageCount = getPageCount(pdfFilePath);
        List<String> allPages = new ArrayList<>();

        for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
            try {
                String pageText = extractText(pdfFilePath, pageNum);
                allPages.add(pageText);
            } catch (PDFExtractionException e) {
                // Re-throw to caller - don't silently ignore extraction failures
                throw e;
            }
        }

        return allPages;
    }
}
