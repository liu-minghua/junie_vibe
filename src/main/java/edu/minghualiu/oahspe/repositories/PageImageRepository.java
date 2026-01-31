package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.PageImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PageImage entities.
 */
@Repository
public interface PageImageRepository extends JpaRepository<PageImage, Long> {
    
    /**
     * Find all images for a specific PageContent.
     */
    List<PageImage> findByPageContentId(Long pageContentId);
    
    /**
     * Find all PageImages that have not been linked to Image entities yet.
     */
    List<PageImage> findByLinkedImageIdIsNull();
    
    /**
     * Count images for a specific PageContent.
     */
    long countByPageContentId(Long pageContentId);
}
