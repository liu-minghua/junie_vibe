package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Image;
import edu.minghualiu.oahspe.entities.Note;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ImageRepositoryTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Test
    void testCreateAndLoadImage() {
        // Arrange
        byte[] bytes = "fake-image-data".getBytes();

        Image image = Image.builder()
                .imageKey("IMG001")
                .title("Test Image")
                .description("A sample image for testing")
                .sourcePage(42)
                .originalFilename("plate_42.png")
                .contentType("image/png")
                .data(bytes)
                .build();

        imageRepository.save(image);

        // Act
        Image loaded = imageRepository.findByImageKey("IMG001").orElseThrow();

        // Assert
        assertThat(loaded.getId()).isGreaterThan(0);
        assertThat(loaded.getTitle()).isEqualTo("Test Image");
        assertThat(loaded.getDescription()).isEqualTo("A sample image for testing");
        assertThat(loaded.getSourcePage()).isEqualTo(42);
        assertThat(loaded.getOriginalFilename()).isEqualTo("plate_42.png");
        assertThat(loaded.getContentType()).isEqualTo("image/png");
        assertThat(loaded.getData()).isEqualTo(bytes);
    }

    @Test
    void testImageNoteRelationship() {
        // Arrange
        Image image = Image.builder()
                .imageKey("IMG002")
                .title("Linked Image")
                .description("Image linked to a note")
                .data("123".getBytes())
                .build();
        image = imageRepository.save(image);
        Note note = Note.builder()
                .noteKey("NOTE001")
                .text("This is a note referencing an image")
                .build();

        // Link both sides
        note.setImages(List.of(image));
        image.setNotes(List.of(note));

        noteRepository.save(note); // saves image via cascade

        // Act
        Image loaded = imageRepository.findByImageKey("IMG002").orElseThrow();

        // Assert
        assertThat(loaded.getNotes()).hasSize(1);
        assertThat(loaded.getNotes().getFirst().getNoteKey()).isEqualTo("NOTE001");
    }
}
