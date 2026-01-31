package edu.minghualiu.oahspe.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WorkflowState entity.
 */
class WorkflowStateTest {
    
    @Test
    void testBuilder() {
        WorkflowState workflow = WorkflowState.builder()
                .workflowName("test-workflow")
                .currentPhase(WorkflowPhase.PAGE_LOADING)
                .status(WorkflowStatus.NOT_STARTED)
                .build();
        
        assertThat(workflow.getWorkflowName()).isEqualTo("test-workflow");
        assertThat(workflow.getCurrentPhase()).isEqualTo(WorkflowPhase.PAGE_LOADING);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.NOT_STARTED);
    }
    
    @Test
    void testUpdatePhase() {
        WorkflowState workflow = WorkflowState.builder()
                .workflowName("test-workflow")
                .currentPhase(WorkflowPhase.PAGE_LOADING)
                .status(WorkflowStatus.NOT_STARTED)
                .build();
        
        workflow.updatePhase(WorkflowPhase.CLEANUP);
        
        assertThat(workflow.getCurrentPhase()).isEqualTo(WorkflowPhase.CLEANUP);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        // Note: updatedAt uses @UpdateTimestamp which is only set by JPA
    }
    
    @Test
    void testMarkCompleted() {
        WorkflowState workflow = WorkflowState.builder()
                .workflowName("test-workflow")
                .currentPhase(WorkflowPhase.CONTENT_INGESTION)
                .status(WorkflowStatus.IN_PROGRESS)
                .build();
        
        workflow.markCompleted();
        
        assertThat(workflow.getCurrentPhase()).isEqualTo(WorkflowPhase.COMPLETED);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(workflow.getCompletedAt()).isNotNull();
        assertThat(workflow.getCompletedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    void testMarkFailed() {
        WorkflowState workflow = WorkflowState.builder()
                .workflowName("test-workflow")
                .currentPhase(WorkflowPhase.CLEANUP)
                .status(WorkflowStatus.IN_PROGRESS)
                .build();
        
        workflow.markFailed("Test error message");
        
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(workflow.getLastError()).isEqualTo("Test error message");
        assertThat(workflow.getCompletedAt()).isNotNull();
    }
    
    @Test
    void testIsTerminal_notStarted() {
        WorkflowState workflow = WorkflowState.builder()
                .status(WorkflowStatus.NOT_STARTED)
                .build();
        
        assertThat(workflow.isTerminal()).isFalse();
    }
    
    @Test
    void testIsTerminal_inProgress() {
        WorkflowState workflow = WorkflowState.builder()
                .status(WorkflowStatus.IN_PROGRESS)
                .build();
        
        assertThat(workflow.isTerminal()).isFalse();
    }
    
    @Test
    void testIsTerminal_completed() {
        WorkflowState workflow = WorkflowState.builder()
                .status(WorkflowStatus.COMPLETED)
                .build();
        
        assertThat(workflow.isTerminal()).isTrue();
    }
    
    @Test
    void testIsTerminal_failed() {
        WorkflowState workflow = WorkflowState.builder()
                .status(WorkflowStatus.FAILED)
                .build();
        
        assertThat(workflow.isTerminal()).isTrue();
    }
}
