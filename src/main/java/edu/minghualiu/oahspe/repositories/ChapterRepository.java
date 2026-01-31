package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter,Long> {
    
    /**
     * Find all chapters on a specific page.
     */
    List<Chapter> findByPageNumber(Integer pageNumber);
    
    /**
     * Find all chapters within a page range.
     */
    List<Chapter> findByPageNumberBetween(Integer startPage, Integer endPage);
    
    /**
     * Count chapters that do not have a pageNumber assigned.
     */
    long countByPageNumberIsNull();
}
