package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks the state of multi-phase workflow executions.
 * Enables resume capability and progress monitoring.
 */
@Entity
@Table(name = "workflow_states",
    indexes = {
        @Index(name = "idx_workflow_name", columnList = "workflow_name", unique = true),
        @Index(name = "idx_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_name", nullable = false, unique = true, length = 100)
    private String workflowName;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", nullable = false, length = 50)
    private WorkflowPhase currentPhase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private WorkflowStatus status = WorkflowStatus.NOT_STARTED;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String statistics;  // JSON string with workflow stats

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Returns true if this workflow has completed (successfully or with failure).
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * Marks the workflow as completed successfully.
     */
    public void markCompleted() {
        this.status = WorkflowStatus.COMPLETED;
        this.currentPhase = WorkflowPhase.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.lastError = null;
    }

    /**
     * Marks the workflow as failed with an error message.
     */
    public void markFailed(String error) {
        this.status = WorkflowStatus.FAILED;
        this.lastError = error;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Updates the current phase and marks workflow as in progress.
     */
    public void updatePhase(WorkflowPhase phase) {
        this.currentPhase = phase;
        this.status = WorkflowStatus.IN_PROGRESS;
    }

    /**
     * Returns a brief summary for logging.
     */
    public String getSummary() {
        return String.format("Workflow[%s] - %s - %s", 
                workflowName,
                currentPhase.getDescription(),
                status.getDescription());
    }
}
