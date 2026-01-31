package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.WorkflowState;
import edu.minghualiu.oahspe.entities.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WorkflowState entities.
 */
@Repository
public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {
    
    /**
     * Find a workflow by its name.
     */
    Optional<WorkflowState> findByWorkflowName(String workflowName);
    
    /**
     * Find all workflows with a specific status.
     */
    List<WorkflowState> findByStatus(WorkflowStatus status);
}
