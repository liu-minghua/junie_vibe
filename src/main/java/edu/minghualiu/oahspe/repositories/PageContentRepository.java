package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.PageCategory;
import edu.minghualiu.oahspe.entities.PageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PageContent entities.
 * Provides queries for page-based ingestion workflow.
 */
@Repository
public interface PageContentRepository extends JpaRepository<PageContent, Long> {
    
    /**
     * Find a page by its page number.
     */
    Optional<PageContent> findByPageNumber(Integer pageNumber);
    
    /**
     * Find all pages in a specific category.
     */
    List<PageContent> findByCategory(PageCategory category);
    
    /**
     * Find all pages in a category that have not been ingested yet, ordered by page number.
     */
    List<PageContent> findByCategoryAndIngestedFalseOrderByPageNumberAsc(PageCategory category);
    
    /**
     * Find all pages in a category that have not been ingested yet.
     * @deprecated Use findByCategoryAndIngestedFalseOrderByPageNumberAsc for proper ordering
     */
    List<PageContent> findByCategoryAndIngestedFalse(PageCategory category);
    
    /**
     * Find all pages that have not been ingested yet (across all categories), ordered by page number.
     */
    List<PageContent> findByIngestedFalseOrderByPageNumberAsc();
    
    /**
     * Find all pages that have not been ingested yet (across all categories).
     * @deprecated Use findByIngestedFalseOrderByPageNumberAsc for proper ordering
     */
    List<PageContent> findByIngestedFalse();
    
    /**
     * Count pages in a specific category.
     */
    long countByCategory(PageCategory category);
    
    /**
     * Count ingested pages in a specific category.
     */
    long countByCategoryAndIngestedTrue(PageCategory category);
    
    /**
     * Find pages within a specific range.
     */
    List<PageContent> findByPageNumberBetween(Integer startPage, Integer endPage);
    
    /**
     * Find all pages with errors.
     */
    List<PageContent> findByErrorMessageIsNotNull();
}
