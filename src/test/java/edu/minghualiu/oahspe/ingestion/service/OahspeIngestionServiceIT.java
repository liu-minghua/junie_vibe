package edu.minghualiu.oahspe.ingestion.service;

import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.ingestion.OahspeIngestionService;
import edu.minghualiu.oahspe.ingestion.parser.OahspeEvent;
import edu.minghualiu.oahspe.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class OahspeIngestionServiceIT {
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public edu.minghualiu.oahspe.ingestion.ImageNoteLinker imageNoteLinker(
                ImageRepository imageRepository, NoteRepository noteRepository) {
            return new edu.minghualiu.oahspe.ingestion.ImageNoteLinker(imageRepository, noteRepository);
        }
        
        @Bean
        public OahspeIngestionService oahspeIngestionService(
                BookRepository bookRepository,
                ChapterRepository chapterRepository,
                VerseRepository verseRepository,
                NoteRepository noteRepository,
                ImageRepository imageRepository,
                edu.minghualiu.oahspe.ingestion.ImageNoteLinker imageNoteLinker) {
            return new OahspeIngestionService(bookRepository, chapterRepository, verseRepository, noteRepository, imageRepository, imageNoteLinker);
        }
    }
    
    @Autowired
    private OahspeIngestionService service;
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
    
    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        chapterRepository.deleteAll();
        verseRepository.deleteAll();
        noteRepository.deleteAll();
        imageRepository.deleteAll();
    }
    
    @Test
    void testParserToServiceIntegration_SingleVerse() {
        List<OahspeEvent> events = Arrays.asList(
            new OahspeEvent.BookStart("Oahspe"),
            new OahspeEvent.ChapterStart("Chapter 1"),
            new OahspeEvent.Verse("1:1", "This is a verse.")
        );
        
        service.ingestEvents(events, 1);
        service.saveCurrentBook();
        
        List<Book> books = bookRepository.findAll();
        assertEquals(1, books.size());
        assertEquals("Oahspe", books.get(0).getTitle());
    }
    
    @Test
    void testContinuationLineHandling() {
        List<OahspeEvent> events = Arrays.asList(
            new OahspeEvent.BookStart("Oahspe"),
            new OahspeEvent.ChapterStart("Chapter 1"),
            new OahspeEvent.Verse("1:1", "First part of verse "),
            new OahspeEvent.Verse(null, "continues here.")
        );
        
        service.ingestEvents(events, 1);
        service.saveCurrentBook();
        
        List<Verse> verses = verseRepository.findAll();
        assertEquals(1, verses.size());
        assertTrue(verses.get(0).getText().contains("First part"));
        assertTrue(verses.get(0).getText().contains("continues here"));
    }
    
    @Test
    void testImageLinkingIntegration() {
        List<OahspeEvent> events = Arrays.asList(
            new OahspeEvent.BookStart("Oahspe"),
            new OahspeEvent.ChapterStart("Chapter 1"),
            new OahspeEvent.Verse("1:1", "Verse with image"),
            new OahspeEvent.Note("1:1a", "Note attached to verse"),
            new OahspeEvent.ImageRef("IMG001", "Portrait of Oahspe")
        );
        
        service.ingestEvents(events, 1);
        service.saveCurrentBook();
        
        List<Image> images = imageRepository.findAll();
        assertEquals(1, images.size());
        assertEquals("IMG001", images.get(0).getImageKey());
        assertEquals("Portrait of Oahspe", images.get(0).getTitle());
        assertEquals(1, images.get(0).getNotes().size());
    }
    
    @Test
    void testFullPageIngestion() {
        List<OahspeEvent> events = Arrays.asList(
            new OahspeEvent.BookStart("Oahspe"),
            new OahspeEvent.ChapterStart("Chapter 1"),
            new OahspeEvent.Verse("1:1", "First verse"),
            new OahspeEvent.Verse("1:2", "Second verse"),
            new OahspeEvent.Note("1:1a", "Note on first verse"),
            new OahspeEvent.Note("1:2b", "Note on second verse"),
            new OahspeEvent.ImageRef("IMG001", "Image A"),
            new OahspeEvent.ChapterStart("Chapter 2"),
            new OahspeEvent.Verse("2:1", "Chapter two verse"),
            new OahspeEvent.Note("2:1a", "Note on chapter 2"),
            new OahspeEvent.ImageRef("IMG002", "Image B")
        );
        
        service.ingestEvents(events, 1);
        service.saveCurrentBook();
        
        List<Book> books = bookRepository.findAll();
        assertEquals(1, books.size());
        assertEquals(2, books.get(0).getChapters().size());
        
        List<Verse> verses = verseRepository.findAll();
        assertEquals(3, verses.size());
        
        List<Note> notes = noteRepository.findAll();
        assertEquals(3, notes.size());
        
        List<Image> images = imageRepository.findAll();
        assertEquals(2, images.size());
    }
    
    @Test
    void testIdempotentImageLinking() {
        Image testImage = Image.builder()
                .imageKey("IMG001")
                .title("Test Image")
                .description("A test image")
                .sourcePage(1)
                .build();
        imageRepository.save(testImage);
        
        Note testNote1 = Note.builder()
                .noteKey("NOTE1")
                .text("First note")
                .build();
        Note savedNote1 = noteRepository.save(testNote1);
        
        // Link same image to same note twice
        edu.minghualiu.oahspe.ingestion.ImageNoteLinker linker = 
            new edu.minghualiu.oahspe.ingestion.ImageNoteLinker(imageRepository, noteRepository);
        
        linker.linkImageToNote(savedNote1, testImage);
        linker.linkImageToNote(savedNote1, testImage);
        
        List<Image> images = imageRepository.findAll();
        assertEquals(1, images.size());
        
        Note linkedNote = noteRepository.findAll().get(0);
        assertEquals(1, linkedNote.getImages().size());
    }
    
    @Test
    void testMultipleBooksIngestion() {
        // Book 1
        List<OahspeEvent> bookOneEvents = Arrays.asList(
            new OahspeEvent.BookStart("Book One"),
            new OahspeEvent.ChapterStart("Chapter 1"),
            new OahspeEvent.Verse("A1:1", "Book one verse"),
            new OahspeEvent.Verse("A1:2", "Another verse")
        );
        
        service.ingestEvents(bookOneEvents, 1);
        service.saveCurrentBook();
        service.finishIngestion();
        
        // Book 2
        List<OahspeEvent> bookTwoEvents = Arrays.asList(
            new OahspeEvent.BookStart("Book Two"),
            new OahspeEvent.ChapterStart("Chapter 1"),
            new OahspeEvent.Verse("B1:1", "Book two verse")
        );
        
        service.ingestEvents(bookTwoEvents, 5);
        service.saveCurrentBook();
        
        List<Book> books = bookRepository.findAll();
        assertEquals(2, books.size());
        
        Book book1 = books.stream().filter(b -> b.getTitle().equals("Book One")).findFirst().get();
        assertEquals(1, book1.getChapters().size());
        assertEquals(2, book1.getChapters().get(0).getVerses().size());
        
        Book book2 = books.stream().filter(b -> b.getTitle().equals("Book Two")).findFirst().get();
        assertEquals(1, book2.getChapters().size());
        assertEquals(1, book2.getChapters().get(0).getVerses().size());
    }
}
