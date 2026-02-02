package edu.minghualiu.oahspe.ingestion.workflow;

import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WorkflowOrchestrator.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowOrchestratorIntegrationTest {
    
    @Autowired
    private WorkflowOrchestrator workflowOrchestrator;
    
    @Autowired
    private PageContentRepository pageContentRepository;
    
    @Autowired
    private WorkflowStateRepository workflowStateRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private VerseRepository verseRepository;
    
    @BeforeEach
    void setUp() {
        // Clean up before each test
        pageContentRepository.deleteAll();
        workflowStateRepository.deleteAll();
        bookRepository.deleteAll();
        chapterRepository.deleteAll();
        verseRepository.deleteAll();
    }
    
    
    @Test
    void testResumeWorkflow_notFound() {
        try {
            workflowOrchestrator.resumeWorkflow("non-existent-workflow");
            assertThat(false).as("Should have thrown exception").isTrue();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("No workflow found");
        }
    }
    
    @Test
    void testResumeWorkflow_alreadyCompleted() {
        WorkflowState workflow = WorkflowState.builder()
                .workflowName("test-workflow")
                .currentPhase(WorkflowPhase.COMPLETED)
                .status(WorkflowStatus.COMPLETED)
                .build();
        workflowStateRepository.save(workflow);
        
        WorkflowState resumed = workflowOrchestrator.resumeWorkflow("test-workflow");
        
        assertThat(resumed.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
    }
}
