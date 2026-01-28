package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {
}
