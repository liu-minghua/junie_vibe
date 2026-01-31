package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByImageKey(String imageKey);
    
    /**
     * Finds the maximum numeric portion of image keys matching pattern 'i###'.
     * Used to resume sequential numbering after restart.
     * 
     * Returns null if no matching images exist in database.
     * 
     * @return the highest image number, or null if none exist
     */
    @Query("SELECT MAX(CAST(SUBSTRING(i.imageKey, 2) AS integer)) " +
           "FROM Image i WHERE i.imageKey LIKE 'i%' " +
           "AND LENGTH(i.imageKey) > 1")
    Integer findMaxImageKeyNumber();
}
