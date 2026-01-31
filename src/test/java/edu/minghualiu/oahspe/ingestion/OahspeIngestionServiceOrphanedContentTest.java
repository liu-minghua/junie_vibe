package edu.minghualiu.oahspe.ingestion;

import edu.minghualiu.oahspe.entities.Book;
import edu.minghualiu.oahspe.entities.Chapter;
import edu.minghualiu.oahspe.entities.Verse;
import edu.minghualiu.oahspe.ingestion.parser.OahspeEvent;
import edu.minghualiu.oahspe.repositories.BookRepository;
import edu.minghualiu.oahspe.repositories.ChapterRepository;
import edu.minghualiu.oahspe.repositories.ImageRepository;
import edu.minghualiu.oahspe.repositories.NoteRepository;
import edu.minghualiu.oahspe.repositories.VerseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OahspeIngestionService's handling of orphaned content
 * (verses that appear before any formal book/chapter declarations).
 * 
 * <p>These tests verify Phase 7.1 implementation - ensuring that orphaned
 * content triggers auto-creation of an "Introduction" book and "Preface" chapter
 * to maintain database referential integrity.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OahspeIngestionService - Orphaned Content Tests")
class OahspeIngestionServiceOrphanedContentTest {

    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private ChapterRepository chapterRepository;
    
    @Mock
    private VerseRepository verseRepository;
    
    @Mock
    private NoteRepository noteRepository;
    
    @Mock
    private ImageRepository imageRepository;
    
    @Mock
    private ImageNoteLinker imageNoteLinker;
    
    @InjectMocks
    private OahspeIngestionService ingestionService;
    
    @BeforeEach
    void setup() {
        // Setup mock responses for repository saves
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verseRepository.save(any(Verse.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    @Test
    @DisplayName("Should create introduction chapter when verse appears before first book/chapter")
    void shouldCreateIntroductionForOrphanedVerse() {
        // Given: A verse event with no current book/chapter context
        OahspeEvent verseEvent = new OahspeEvent.Verse("1/1.1", "This is an orphaned verse in the preface");
        
        // When: Process the orphaned verse
        ingestionService.ingestEvents(List.of(verseEvent), 1);
        
        // Then: Should create introduction book
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository, times(1)).save(bookCaptor.capture());
        Book savedBook = bookCaptor.getValue();
        assertThat(savedBook.getTitle()).isEqualTo("Introduction");
        assertThat(savedBook.getDescription()).isEqualTo("Preface and introductory content");
        
        // Then: Should create introduction chapter
        ArgumentCaptor<Chapter> chapterCaptor = ArgumentCaptor.forClass(Chapter.class);
        verify(chapterRepository, times(1)).save(chapterCaptor.capture());
        Chapter savedChapter = chapterCaptor.getValue();
        assertThat(savedChapter.getTitle()).isEqualTo("Preface");
        assertThat(savedChapter.getDescription()).isEqualTo("Content before first formal chapter");
        
        // Then: Should save the verse with chapter reference
        ArgumentCaptor<Verse> verseCaptor = ArgumentCaptor.forClass(Verse.class);
        verify(verseRepository, times(1)).save(verseCaptor.capture());
        Verse savedVerse = verseCaptor.getValue();
        assertThat(savedVerse.getText()).isEqualTo("This is an orphaned verse in the preface");
        assertThat(savedVerse.getChapter()).isNotNull();
        assertThat(savedVerse.getChapter().getTitle()).isEqualTo("Preface");
        
        // Then: Introduction chapter created flag should be true
        assertThat(ingestionService.isIntroductionChapterCreated()).isTrue();
    }
    
    @Test
    @DisplayName("Should reuse introduction chapter for multiple orphaned verses")
    void shouldReuseIntroductionForMultipleOrphanedVerses() {
        // Given: Multiple orphaned verses
        OahspeEvent verse1 = new OahspeEvent.Verse("1/1.1", "First orphaned verse");
        OahspeEvent verse2 = new OahspeEvent.Verse("1/1.2", "Second orphaned verse");
        OahspeEvent verse3 = new OahspeEvent.Verse("1/1.3", "Third orphaned verse");
        
        // When: Process all orphaned verses
        ingestionService.ingestEvents(List.of(verse1, verse2, verse3), 1);
        
        // Then: Should create introduction book only once
        verify(bookRepository, times(1)).save(any(Book.class));
        
        // Then: Should create introduction chapter only once
        verify(chapterRepository, times(1)).save(any(Chapter.class));
        
        // Then: Should save all three verses
        verify(verseRepository, times(3)).save(any(Verse.class));
    }
    
    @Test
    @DisplayName("Should transition from introduction chapter to real chapter correctly")
    void shouldTransitionFromIntroductionToRealChapter() {
        // Given: Orphaned verse followed by real book/chapter
        OahspeEvent orphanedVerse = new OahspeEvent.Verse("1/1.1", "Orphaned verse in preface");
        OahspeEvent bookStart = new OahspeEvent.BookStart("Book of Genesis");
        OahspeEvent chapterStart = new OahspeEvent.ChapterStart("Chapter 1: Creation");
        OahspeEvent regularVerse = new OahspeEvent.Verse("14/1.1", "Regular verse in real chapter");
        
        // When: Process events in sequence
        ingestionService.ingestEvents(List.of(orphanedVerse, bookStart, chapterStart, regularVerse), 1);
        
        // Then: Should create two books (Introduction + Genesis)
        verify(bookRepository, times(2)).save(any(Book.class));
        
        // Then: Should create two chapters (Preface + Chapter 1)
        verify(chapterRepository, times(2)).save(any(Chapter.class));
        
        // Then: Should save two verses (orphaned + regular)
        ArgumentCaptor<Verse> verseCaptor = ArgumentCaptor.forClass(Verse.class);
        verify(verseRepository, times(2)).save(verseCaptor.capture());
        
        var allVerses = verseCaptor.getAllValues();
        assertThat(allVerses.get(0).getText()).isEqualTo("Orphaned verse in preface");
        assertThat(allVerses.get(0).getChapter().getTitle()).isEqualTo("Preface");
        
        assertThat(allVerses.get(1).getText()).isEqualTo("Regular verse in real chapter");
        assertThat(allVerses.get(1).getChapter().getTitle()).isEqualTo("Chapter 1: Creation");
    }
    
    @Test
    @DisplayName("Should reset introduction flag after ingestion finishes")
    void shouldResetIntroductionFlagAfterIngestion() {
        // Given: Orphaned verse processed
        OahspeEvent orphanedVerse = new OahspeEvent.Verse("1/1.1", "Orphaned verse");
        
        ingestionService.ingestEvents(List.of(orphanedVerse), 1);
        assertThat(ingestionService.isIntroductionChapterCreated()).isTrue();
        
        // When: Finish ingestion
        ingestionService.finishIngestion();
        
        // Then: Flag should be reset
        assertThat(ingestionService.isIntroductionChapterCreated()).isFalse();
    }
    
    @Test
    @DisplayName("Should not create introduction if verse has chapter context")
    void shouldNotCreateIntroductionIfChapterExists() {
        // Given: Real book and chapter established first
        OahspeEvent bookStart = new OahspeEvent.BookStart("Book of Genesis");
        OahspeEvent chapterStart = new OahspeEvent.ChapterStart("Chapter 1");
        OahspeEvent verse = new OahspeEvent.Verse("14/1.1", "Regular verse");
        
        // When: Process events
        ingestionService.ingestEvents(List.of(bookStart, chapterStart, verse), 1);
        
        // Then: Should create only one book (Genesis, not Introduction)
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository, times(1)).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getTitle()).isEqualTo("Book of Genesis");
        
        // Then: Introduction flag should remain false
        assertThat(ingestionService.isIntroductionChapterCreated()).isFalse();
    }
}
