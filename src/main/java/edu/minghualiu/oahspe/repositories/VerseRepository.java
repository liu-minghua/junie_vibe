package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Verse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerseRepository extends JpaRepository<Verse, Long> {
    
    /**
     * Find all verses on a specific page.
     */
    List<Verse> findByPageNumber(Integer pageNumber);
    
    /**
     * Find all verses within a page range.
     */
    List<Verse> findByPageNumberBetween(Integer startPage, Integer endPage);
    
    /**
     * Count verses that do not have a pageNumber assigned.
     */
    long countByPageNumberIsNull();
}
