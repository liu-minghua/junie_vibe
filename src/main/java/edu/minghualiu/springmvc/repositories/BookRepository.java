package edu.minghualiu.springmvc.repositories;

import edu.minghualiu.springmvc.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
