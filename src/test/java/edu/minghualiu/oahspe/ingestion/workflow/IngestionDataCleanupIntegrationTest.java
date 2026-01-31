package edu.minghualiu.oahspe.ingestion.workflow;

import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for IngestionDataCleanup.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IngestionDataCleanupIntegrationTest {
    
    @Autowired
    private IngestionDataCleanup dataCleanup;
    
    @Autowired
    private PageContentRepository pageContentRepository;
    
    @Autowired
    private PageImageRepository pageImageRepository;
    
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
    
    @Autowired
    private GlossaryTermRepository glossaryTermRepository;
    
    @Autowired
    private IndexEntryRepository indexEntryRepository;
    
    @BeforeEach
    void setUp() {
        // Clean all tables
        noteRepository.deleteAll();
        verseRepository.deleteAll();
        chapterRepository.deleteAll();
        bookRepository.deleteAll();
        imageRepository.deleteAll();
        glossaryTermRepository.deleteAll();
        indexEntryRepository.deleteAll();
        pageImageRepository.deleteAll();
        pageContentRepository.deleteAll();
    }
    
    @Test
    void testCleanupAllIngestedData_deletesBooks() {
        // Create test data
        Book book = Book.builder()
                .title("Test Book")
                .pageNumber(100)
                .build();
        bookRepository.save(book);
        
        assertThat(bookRepository.count()).isEqualTo(1);
        
        dataCleanup.cleanupAllIngestedData();
        
        assertThat(bookRepository.count()).isZero();
    }
    
    @Test
    void testCleanupAllIngestedData_deletesChapters() {
        // Create test data
        Book book = Book.builder()
                .title("Test Book")
                .pageNumber(100)
                .build();
        bookRepository.save(book);
        
        Chapter chapter = Chapter.builder()
                .book(book)
                .title("Test Chapter")
                .pageNumber(101)
                .build();
        chapterRepository.save(chapter);
        
        assertThat(chapterRepository.count()).isEqualTo(1);
        
        dataCleanup.cleanupAllIngestedData();
        
        assertThat(chapterRepository.count()).isZero();
    }
    
    @Test
    void testCleanupAllIngestedData_deletesVerses() {
        // Create test data
        Book book = Book.builder()
                .title("Test Book")
                .pageNumber(100)
                .build();
        bookRepository.save(book);
        
        Chapter chapter = Chapter.builder()
                .book(book)
                .title("Test Chapter")
                .pageNumber(101)
                .build();
        chapterRepository.save(chapter);
        
        Verse verse = Verse.builder()
                .chapter(chapter)
                .verseKey("1")
                .text("Test verse")
                .pageNumber(101)
                .build();
        verseRepository.save(verse);
        
        assertThat(verseRepository.count()).isEqualTo(1);
        
        dataCleanup.cleanupAllIngestedData();
        
        assertThat(verseRepository.count()).isZero();
    }
    
    @Test
    void testCleanupAllIngestedData_deletesGlossaryTerms() {
        GlossaryTerm term = GlossaryTerm.builder()
                .term("TEST")
                .definition("Test definition")
                .pageNumber(1670)
                .build();
        glossaryTermRepository.save(term);
        
        assertThat(glossaryTermRepository.count()).isEqualTo(1);
        
        dataCleanup.cleanupAllIngestedData();
        
        assertThat(glossaryTermRepository.count()).isZero();
    }
    
    @Test
    void testCleanupAllIngestedData_deletesIndexEntries() {
        IndexEntry entry = IndexEntry.builder()
                .topic("Test Topic")
                .pageReferences("100, 200")
                .extractedFromPage(1700)
                .build();
        indexEntryRepository.save(entry);
        
        assertThat(indexEntryRepository.count()).isEqualTo(1);
        
        dataCleanup.cleanupAllIngestedData();
        
        assertThat(indexEntryRepository.count()).isZero();
    }
    
    @Test
    void testCleanupAllIngestedData_preservesPageContent() {
        // Create PageContent
        PageContent page = PageContent.builder()
                .pageNumber(100)
                .category(PageCategory.OAHSPE_BOOKS)
                .rawText("Test text")
                .build();
        pageContentRepository.save(page);
        
        assertThat(pageContentRepository.count()).isEqualTo(1);
        
        dataCleanup.cleanupAllIngestedData();
        
        // PageContent should still exist
        assertThat(pageContentRepository.count()).isEqualTo(1);
    }
    
    @Test
    void testCleanupAllIngestedData_preservesPageImages() {
        // Create PageContent and PageImage
        PageContent page = PageContent.builder()
                .pageNumber(100)
                .category(PageCategory.OAHSPE_BOOKS)
                .rawText("Test text")
                .build();
        pageContentRepository.save(page);
        
        PageImage pageImage = PageImage.builder()
                .pageContent(page)
                .imageSequence(1)
                .imageData(new byte[]{1, 2, 3})
                .build();
        pageImageRepository.save(pageImage);
        
        assertThat(pageImageRepository.count()).isEqualTo(1);
        
        dataCleanup.cleanupAllIngestedData();
        
        // PageImage should still exist
        assertThat(pageImageRepository.count()).isEqualTo(1);
    }
    
    @Test
    void testCleanupContentOnly_preservesGlossaryAndIndex() {
        // Create Books
        Book book = Book.builder()
                .title("Test Book")
                .pageNumber(100)
                .build();
        bookRepository.save(book);
        
        // Create Glossary Terms
        GlossaryTerm term = GlossaryTerm.builder()
                .term("TEST")
                .definition("Test definition")
                .pageNumber(1670)
                .build();
        glossaryTermRepository.save(term);
        
        // Create Index Entries
        IndexEntry entry = IndexEntry.builder()
                .topic("Test Topic")
                .pageReferences("100")
                .extractedFromPage(1700)
                .build();
        indexEntryRepository.save(entry);
        
        dataCleanup.cleanupContentOnly();
        
        // Books should be deleted
        assertThat(bookRepository.count()).isZero();
        
        // Glossary and Index should be preserved
        assertThat(glossaryTermRepository.count()).isEqualTo(1);
        assertThat(indexEntryRepository.count()).isEqualTo(1);
    }
}
