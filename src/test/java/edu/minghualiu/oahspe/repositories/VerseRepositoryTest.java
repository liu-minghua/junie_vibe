package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Book;
import edu.minghualiu.oahspe.entities.Chapter;
import edu.minghualiu.oahspe.entities.Note;
import edu.minghualiu.oahspe.entities.Verse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class VerseRepositoryTest {

    @Autowired
    private VerseRepository verseRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void saveVerseWithNotes() {
        Book book = Book.builder().title("Oahspe").build();
        bookRepository.save(book);

        Chapter chapter = Chapter.builder()
                .title("Chapter 1")
                .book(book)
                .build();
        chapterRepository.save(chapter);

        Verse verse = Verse.builder()
                .verseKey("1")
                .text("Verse text")
                .chapter(chapter)
                .build();

        Note note1 = Note.builder()
                .noteKey("n1")
                .text("Note 1")
                .verse(verse)
                .build();

        Note note2 = Note.builder()
                .noteKey("n2")
                .text("Note 2")
                .verse(verse)
                .build();

        verse.getNotes().add(note1);
        verse.getNotes().add(note2);

        Verse saved = verseRepository.save(verse);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNotes()).hasSize(2);
    }
}
