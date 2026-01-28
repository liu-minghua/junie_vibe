package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Verse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerseRepository extends JpaRepository<Verse, Long> {
}
