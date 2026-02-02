package edu.minghualiu.oahspe.ingestion.runner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PDF text extraction component using Apache PDFBox.
 */
@Component
public class PDFTextExtractor {

    /**
     * Regex for Oahspe verse markers like:
     * 01/2.15
     * 02/1.1
     * 03/4.12
     */
    public static final Pattern VERSE_PATTERN =
            Pattern.compile("\\d{2}/\\d+\\.\\d+");

    /**
     * Extracts text from a specific page of a PDF file.
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
     */
    public List<String> extractAllPages(String pdfFilePath) throws PDFExtractionException {
        int pageCount = getPageCount(pdfFilePath);
        List<String> allPages = new ArrayList<>();

        for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
            String pageText = extractText(pdfFilePath, pageNum);
            allPages.add(pageText);
        }

        return allPages;
    }
}
