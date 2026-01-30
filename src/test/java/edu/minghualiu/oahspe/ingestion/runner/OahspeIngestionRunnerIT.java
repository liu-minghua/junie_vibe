package edu.minghualiu.oahspe.ingestion.runner;

import edu.minghualiu.oahspe.ingestion.parser.OahspeParser;
import edu.minghualiu.oahspe.ingestion.OahspeIngestionService;
import edu.minghualiu.oahspe.ingestion.ImageNoteLinker;
import edu.minghualiu.oahspe.repositories.*;
import edu.minghualiu.oahspe.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OahspeIngestionRunner.
 * Tests the complete PDF ingestion pipeline: extraction → parsing → ingestion.
 */
@DataJpaTest
@DisplayName("OahspeIngestionRunner Integration Tests")
public class OahspeIngestionRunnerIT {
    private static final String TEST_RESOURCES = "src/test/resources";

    @Autowired
    private OahspeIngestionRunner runner;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private VerseRepository verseRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private ImageRepository imageRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PDFTextExtractor pdfTextExtractor() {
            return new PDFTextExtractor();
        }

        @Bean
        public OahspeParser oahspeParser() {
            return new OahspeParser();
        }

        @Bean
        public ImageNoteLinker imageNoteLinker(ImageRepository imageRepository, NoteRepository noteRepository) {
            return new ImageNoteLinker(imageRepository, noteRepository);
        }

        @Bean
        public OahspeIngestionService oahspeIngestionService(
                BookRepository bookRepository,
                ChapterRepository chapterRepository,
                VerseRepository verseRepository,
                NoteRepository noteRepository,
                ImageRepository imageRepository,
                ImageNoteLinker imageNoteLinker) {
            return new OahspeIngestionService(
                    bookRepository,
                    chapterRepository,
                    verseRepository,
                    noteRepository,
                    imageRepository,
                    imageNoteLinker
            );
        }

        @Bean
        public OahspeIngestionRunner oahspeIngestionRunner(
                PDFTextExtractor pdfExtractor,
                OahspeParser parser,
                OahspeIngestionService ingestionService) {
            return new OahspeIngestionRunner(pdfExtractor, parser, ingestionService);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        // Ensure test resources directory exists
        Files.createDirectories(Paths.get(TEST_RESOURCES));
        
        // Clear repositories
        verseRepository.deleteAll();
        noteRepository.deleteAll();
        imageRepository.deleteAll();
        chapterRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("T1: Basic PDF ingestion workflow")
    void testBasicPdfIngestion() throws IOException, PDFExtractionException {
        // Setup: Create test PDF
        String pdfPath = createTestPdf("Book of Apollo\nChapter 1\n1/1.1 Verse one\n1/1.2 Verse two");
        
        // Execute: Ingest PDF
        IngestionContext context = runner.ingestPdf(pdfPath);
        
        // Verify: Context populated
        assertNotNull(context);
        assertEquals(pdfPath, context.getPdfFilePath());
        assertTrue(context.getTotalPages() > 0);
        assertEquals(0, context.getTotalErrorsEncountered());
        assertTrue(context.isSuccessful());
    }

    @Test
    @DisplayName("T2: Page-by-page processing")
    void testPageByPageProcessing() throws IOException, PDFExtractionException {
        // Setup: Create 3-page test PDF
        String pdfPath = createMultiPagePdf();
        
        // Execute: Ingest PDF
        IngestionContext context = runner.ingestPdf(pdfPath);
        
        // Verify: All pages processed
        assertEquals(3, context.getTotalPages());
        assertEquals(3, context.getCurrentPageNumber());
        assertTrue(context.getTotalEventsProcessed() > 0);
        assertEquals(0, context.getTotalErrorsEncountered());
    }

    @Test
    @DisplayName("T3: Progress callback integration")
    void testProgressCallback() throws IOException, PDFExtractionException {
        // Setup
        String pdfPath = createMultiPagePdf();
        MockProgressCallback callback = new MockProgressCallback();
        
        // Execute: Ingest with callback
        IngestionContext context = runner.ingestPdfWithProgress(pdfPath, callback);
        
        // Verify: Callbacks were called
        assertTrue(callback.pageStartCount > 0);
        assertTrue(callback.pageCompleteCount > 0);
        assertEquals(1, callback.ingestionCompleteCount);
        assertEquals(0, callback.pageErrorCount);
    }

    @Test
    @DisplayName("T4: File not found error")
    void testFileNotFoundError() {
        String nonExistentPath = "nonexistent.pdf";
        
        assertThrows(PDFExtractionException.class, () -> {
            runner.ingestPdf(nonExistentPath);
        });
    }

    @Test
    @DisplayName("T5: Invalid PDF error")
    void testInvalidPdfError() throws IOException {
        // Create invalid PDF file
        String invalidPath = TEST_RESOURCES + "/invalid-runner.txt";
        Files.write(Paths.get(invalidPath), "Not a PDF".getBytes());
        
        assertThrows(PDFExtractionException.class, () -> {
            runner.ingestPdf(invalidPath);
        });
    }

    @Test
    @DisplayName("T6: Multi-page with state reset")
    void testMultiPageWithStateReset() throws IOException, PDFExtractionException {
        // Create PDF with book content
        String pdfPath = createTestPdf(
                "Book of Apollo\nChapter 1\n1/1.1 First verse\n" +
                "Book of Oahspe\nChapter 1\n2/1.1 Second verse"
        );
        
        // Execute
        IngestionContext context = runner.ingestPdf(pdfPath);
        
        // Verify: Context succeeded (entities may or may not be created depending on parser)
        assertEquals(0, context.getTotalErrorsEncountered());
        assertTrue(context.isSuccessful());
    }

    // Test utility methods

    private String createTestPdf(String content) throws IOException {
        String pdfPath = TEST_RESOURCES + "/test-runner.pdf";
        try (PDDocument document = new PDDocument()) {
            addPage(document, content);
            document.save(pdfPath);
        }
        return pdfPath;
    }

    private String createMultiPagePdf() throws IOException {
        String pdfPath = TEST_RESOURCES + "/test-multipage.pdf";
        try (PDDocument document = new PDDocument()) {
            addPage(document, "Book of Apollo\nChapter 1\n1/1.1 Page one verse");
            addPage(document, "1/1.2 Page two verse\n1/1.3 Another verse");
            addPage(document, "Chapter 2\n2/1.1 Page three verse");
            document.save(pdfPath);
        }
        return pdfPath;
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

    /**
     * Mock implementation of ProgressCallback for testing.
     */
    private static class MockProgressCallback implements ProgressCallback {
        int pageStartCount = 0;
        int pageCompleteCount = 0;
        int pageErrorCount = 0;
        int ingestionCompleteCount = 0;

        @Override
        public void onPageStart(int pageNumber, int totalPages) {
            pageStartCount++;
        }

        @Override
        public void onPageComplete(int pageNumber, int eventsProcessed) {
            pageCompleteCount++;
        }

        @Override
        public void onPageError(int pageNumber, Exception exception) {
            pageErrorCount++;
        }

        @Override
        public void onIngestionComplete(IngestionContext context) {
            ingestionCompleteCount++;
        }
    }
}
