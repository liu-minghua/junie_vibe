package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    
    /**
     * Find all books on a specific page.
     */
    List<Book> findByPageNumber(Integer pageNumber);
    
    /**
     * Find all books within a page range.
     */
    List<Book> findByPageNumberBetween(Integer startPage, Integer endPage);
    
    /**
     * Count books that do not have a pageNumber assigned.
     */
    long countByPageNumberIsNull();
}
