package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.IndexEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for IndexEntry entities.
 */
@Repository
public interface IndexEntryRepository extends JpaRepository<IndexEntry, Long> {
    
    /**
     * Find an index entry by its exact topic.
     */
    Optional<IndexEntry> findByTopic(String topic);
    
    /**
     * Find all index entries linked to a specific glossary term.
     */
    List<IndexEntry> findByGlossaryTermId(Long glossaryTermId);
    
    /**
     * Find all index entries with pagination support.
     */
    Page<IndexEntry> findAll(Pageable pageable);
    
    /**
     * Find index entries containing a search string (case-insensitive).
     */
    List<IndexEntry> findByTopicContainingIgnoreCase(String searchTopic);
}
