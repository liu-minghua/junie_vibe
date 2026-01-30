package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {
    @Autowired
    private BookRepository bookRepository;

    @Test
    void testSaveBook() {
        //given
        Book book = Book.builder()
                .title("Book of Jehovih")
                .build();
        // when
        Book savedBook = bookRepository.save(book);
        // Then
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.getTitle()).isEqualTo("Book of Jehovih");
        assertThat(savedBook.getId()).isNotNull();
        //assertThat(bookRepository.findAll()).isNotEmpty();
        //cleanup
        bookRepository.delete(savedBook);
        //assertThat(bookRepository.findAll()).hasSize(0);
    }
}
