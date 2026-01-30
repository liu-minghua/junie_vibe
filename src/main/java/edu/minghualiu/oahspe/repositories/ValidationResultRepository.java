package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.ValidationResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ValidationResultEntity entities.
 */
@Repository
public interface ValidationResultRepository extends JpaRepository<ValidationResultEntity, String> {
}
