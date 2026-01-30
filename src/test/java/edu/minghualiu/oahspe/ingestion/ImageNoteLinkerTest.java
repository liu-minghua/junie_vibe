package edu.minghualiu.oahspe.ingestion;

import edu.minghualiu.oahspe.entities.Image;
import edu.minghualiu.oahspe.entities.Note;
import edu.minghualiu.oahspe.repositories.ImageRepository;
import edu.minghualiu.oahspe.repositories.NoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest
@Import(ImageNoteLinker.class)
class ImageNoteLinkerTest {

    @Autowired
    ImageNoteLinker imageNoteLinker;
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Test
    void testLinkImageToNote() {
        Note note = Note.builder()
                .noteKey("NOTE100")
                .text("Note with image")
                .build();

        Image image = Image.builder()
                .imageKey("IMG100")
                .title("Test Image")
                .description("Desc")
                .data("abc".getBytes())
                .build();

        imageNoteLinker.linkImageToNote(note, image);

        Image loaded = imageRepository.findByImageKey("IMG100").orElseThrow();
        assertThat(loaded.getNotes()).hasSize(1);
    }

}
