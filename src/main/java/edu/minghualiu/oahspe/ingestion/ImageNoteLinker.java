package edu.minghualiu.oahspe.ingestion;

import edu.minghualiu.oahspe.entities.Image;
import edu.minghualiu.oahspe.entities.Note;
import edu.minghualiu.oahspe.repositories.ImageRepository;
import edu.minghualiu.oahspe.repositories.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ImageNoteLinker {

    private final ImageRepository imageRepository;
    private final NoteRepository noteRepository;

    /**
     * Ensures the image exists in the database, then links it to the note.
     * This method is idempotent and safe to call multiple times.
     */
    @Transactional
    public void linkImageToNote(Note note, Image image) {

        // 1. Ensure the image is persisted
        Image persistedImage = imageRepository.findByImageKey(image.getImageKey())
                .orElseGet(() -> imageRepository.save(image));

        // 2. Ensure the note is persisted
        Note persistedNote = (note.getId() == 0)
                ? noteRepository.save(note)
                : note;

        // 3. Link both sides if not already linked
        if (!persistedNote.getImages().contains(persistedImage)) {
            persistedNote.getImages().add(persistedImage);
        }

        if (!persistedImage.getNotes().contains(persistedNote)) {
            persistedImage.getNotes().add(persistedNote);
        }

        // 4. Save the owning side (Note)
        noteRepository.save(persistedNote);
    }
}
