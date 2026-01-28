package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter,Long> {
}
