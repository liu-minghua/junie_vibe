package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.GlossaryTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GlossaryTerm entities.
 */
@Repository
public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Long> {
    
    /**
     * Find a glossary term by its exact term.
     */
    Optional<GlossaryTerm> findByTerm(String term);
    
    /**
     * Find all glossary terms of a specific type.
     */
    List<GlossaryTerm> findByTermType(String termType);
    
    /**
     * Find all glossary terms extracted from a specific page.
     */
    List<GlossaryTerm> findByPageNumber(Integer pageNumber);
    
    /**
     * Find glossary terms containing a search string (case-insensitive).
     */
    List<GlossaryTerm> findByTermContainingIgnoreCase(String searchTerm);
}
