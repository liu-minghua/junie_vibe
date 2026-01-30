package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.ValidationRequest;
import edu.minghualiu.oahspe.entities.ValidationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ValidationRequest entities.
 */
@Repository
public interface ValidationRequestRepository extends JpaRepository<ValidationRequest, String> {

    /**
     * Find all validation requests with a specific status.
     *
     * @param status the validation status
     * @return list of validation requests matching the status
     */
    Page<ValidationRequest> findByStatus(ValidationStatus status, Pageable pageable);
}
