package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Book;
import edu.minghualiu.oahspe.entities.Chapter;
import edu.minghualiu.oahspe.entities.Verse;
import edu.minghualiu.oahspe.repositories.ChapterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChapterRepositoryTest {

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    void testSaveAndFindChapter() {

        // --- 1. Save Book in its own transaction ---
        TransactionTemplate template = new TransactionTemplate(txManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Integer bookId = template.execute(status -> {
            Book book = Book.builder()
                    .title("Test Book")
                    .description("desc")
                    .build();
            Book saved = bookRepository.save(book);
            return saved.getId();
        });

        // --- 2. Now we are back in the test transaction ---
        Book managedBook = bookRepository.findById(bookId).orElseThrow();

        // --- 3. Save Chapter referencing the stable Book ---
        Chapter chapter = Chapter.builder()
                .title("Chapter 1")
                .description("desc")
                .book(managedBook)
                .build();

        Chapter savedChapter = chapterRepository.save(chapter);

        assertThat(savedChapter.getId()).isNotNull();
        assertThat(savedChapter.getBook().getId()).isEqualTo(bookId);
    }
    @Test void saveChapterWithVerses() {
        Book book = Book.builder()
                .title("Oahspe")
                .build();
        Book savedBook = bookRepository.save(book);
        Chapter chapter = Chapter.builder()
                .title("Chapter 1")
                .book(savedBook)
                .build();
        Verse verse1 = Verse.builder()
                .verseKey("1")
                .text("First verse")
                .chapter(chapter)
                .build();
        Verse verse2 = Verse.builder()
                .verseKey("2")
                .text("Second verse")
                .chapter(chapter)
                .build();
        chapter.getVerses().add(verse1);
        chapter.getVerses().add(verse2);
        Chapter saved = chapterRepository.save(chapter);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVerses()).hasSize(2);
    }
}
