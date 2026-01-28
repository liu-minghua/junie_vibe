package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Image;
import edu.minghualiu.oahspe.entities.Note;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NoteRepositoryTest {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Test
    void saveNoteWithImages() {
        Image img1 = Image.builder()
                .imageKey("i001")
                .title("Image 1")
                .description("Desc 1")
                .contentType("image/png")
                .data(new byte[]{1})
                .build();

        Image img2 = Image.builder()
                .imageKey("i002")
                .title("Image 2")
                .description("Desc 2")
                .contentType("image/png")
                .data(new byte[]{2})
                .build();

        imageRepository.save(img1);
        imageRepository.save(img2);

        Note note = Note.builder()
                .noteKey("n001")
                .text("Note with images")
                .build();

        note.getImages().add(img1);
        note.getImages().add(img2);

        Note saved = noteRepository.save(note);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getImages()).hasSize(2);
    }
}

