package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    
    /**
     * Find all notes on a specific page.
     */
    List<Note> findByPageNumber(Integer pageNumber);
    
    /**
     * Count notes that do not have a pageNumber assigned.
     */
    long countByPageNumberIsNull();
}
