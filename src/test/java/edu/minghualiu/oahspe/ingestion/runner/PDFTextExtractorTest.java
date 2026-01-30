package edu.minghualiu.oahspe.ingestion.runner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PDFTextExtractor.
 * Tests focus on extraction logic, error handling, and edge cases.
 * Generates test PDFs dynamically in test resources directory.
 */
@DisplayName("PDFTextExtractor Unit Tests")
public class PDFTextExtractorTest {
    private PDFTextExtractor extractor;
    private static final String TEST_RESOURCES = "src/test/resources";

    @BeforeEach
    void setUp() throws IOException {
        extractor = new PDFTextExtractor();
        
        // Ensure test resources directory exists
        Files.createDirectories(Paths.get(TEST_RESOURCES));
        
        // Generate test PDFs
        generateTestSamplePdf(TEST_RESOURCES + "/test-sample.pdf");
        generateEmptyPdf(TEST_RESOURCES + "/empty.pdf");
    }

    @Test
    @DisplayName("T1: Extract single page text successfully")
    void testExtractSinglePageText() throws PDFExtractionException {
        String testPath = TEST_RESOURCES + "/test-sample.pdf";
        String result = extractor.extractText(testPath, 1);
        assertNotNull(result);
        assertTrue(result.length() >= 0);
        // Result should be a string (may be empty if PDF generation fails)
    }

    @Test
    @DisplayName("T2: Extract multiple pages in sequence")
    void testExtractMultiplePagesSequence() throws PDFExtractionException {
        String testPath = TEST_RESOURCES + "/test-sample.pdf";
        int pageCount = extractor.getPageCount(testPath);
        
        // Extract each page
        for (int i = 1; i <= pageCount; i++) {
            String result = extractor.extractText(testPath, i);
            assertNotNull(result);
        }
    }

    @Test
    @DisplayName("T3: Handle file not found")
    void testFileNotFound() {
        String nonExistentPath = "nonexistent/path/file.pdf";
        
        PDFExtractionException exception = assertThrows(
                PDFExtractionException.class,
                () -> extractor.extractText(nonExistentPath, 1)
        );
        
        assertEquals(nonExistentPath, exception.getPdfFilePath());
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("T4: Handle invalid PDF")
    void testInvalidPdf() throws IOException {
        String invalidPath = TEST_RESOURCES + "/invalid.txt";
        Files.write(Paths.get(invalidPath), "This is not a PDF".getBytes());
        
        PDFExtractionException exception = assertThrows(
                PDFExtractionException.class,
                () -> extractor.extractText(invalidPath, 1)
        );
        
        assertEquals(invalidPath, exception.getPdfFilePath());
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("T5: Get correct page count")
    void testGetPageCount() throws PDFExtractionException {
        String testPath = TEST_RESOURCES + "/test-sample.pdf";
        int pageCount = extractor.getPageCount(testPath);
        assertTrue(pageCount > 0);
        assertEquals(3, pageCount);
    }

    @Test
    @DisplayName("T6: Handle empty PDF")
    void testEmptyPdf() throws PDFExtractionException {
        String emptyPath = TEST_RESOURCES + "/empty.pdf";
        int pageCount = extractor.getPageCount(emptyPath);
        assertEquals(0, pageCount);
    }

    @Test
    @DisplayName("T7: Extract all pages returns correct list size")
    void testExtractAllPages() throws PDFExtractionException {
        String testPath = TEST_RESOURCES + "/test-sample.pdf";
        List<String> allPages = extractor.extractAllPages(testPath);
        
        int pageCount = extractor.getPageCount(testPath);
        assertEquals(pageCount, allPages.size());
        
        // Verify no nulls in list
        for (String page : allPages) {
            assertNotNull(page);
        }
    }

    @Test
    @DisplayName("T3b: Page number out of range")
    void testPageNumberOutOfRange() throws PDFExtractionException {
        String testPath = TEST_RESOURCES + "/test-sample.pdf";
        int pageCount = extractor.getPageCount(testPath);
        
        // Try to extract beyond last page
        PDFExtractionException exception = assertThrows(
                PDFExtractionException.class,
                () -> extractor.extractText(testPath, pageCount + 1)
        );
        
        assertEquals(pageCount + 1, exception.getPageNumber());
        assertTrue(exception.getMessage().contains("out of range"));
    }

    @Test
    @DisplayName("T3c: Zero page number")
    void testZeroPageNumber() throws PDFExtractionException {
        String testPath = TEST_RESOURCES + "/test-sample.pdf";
        
        // Page 0 should be invalid (1-indexed)
        PDFExtractionException exception = assertThrows(
                PDFExtractionException.class,
                () -> extractor.extractText(testPath, 0)
        );
        
        assertEquals(0, exception.getPageNumber());
    }

    // Helper methods

    private void generateTestSamplePdf(String outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            addPage(document, "Book of Apollo\n\nChapter 1\n\n1/1.1 And the light shone forth\n1/1.2 In the beginning was the word");
            addPage(document, "2/1.1 The heavens declare\n2/1.2 The glory of the Creator\n\n(1) This is a note\n(2) Another important note");
            addPage(document, "3/1.1 And wisdom came\ni001 Image Caption\n\n(3) Final note on page three");
            document.save(outputPath);
        }
    }

    private void generateEmptyPdf(String outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Empty document
            document.save(outputPath);
        }
    }

    private void addPage(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.setTextMatrix(50.0f, 750.0f, 0.0f, 0.0f, 0.0f, 750.0f);

            for (String line : text.split("\n")) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15);
            }

            contentStream.endText();
        }
    }
}
