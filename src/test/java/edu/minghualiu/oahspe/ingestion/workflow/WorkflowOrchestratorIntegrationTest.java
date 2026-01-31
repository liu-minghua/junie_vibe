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
    void testVerifyPageLoading_allPagesLoaded() {
        // Create 1831 PageContent entities
        for (int i = 1; i <= 1831; i++) {
            PageCategory category = PageCategory.fromPageNumber(i);
            PageContent page = PageContent.builder()
                    .pageNumber(i)
                    .category(category)
                    .rawText("Test text page " + i)
                    .build();
            pageContentRepository.save(page);
        }
        
        boolean verified = workflowOrchestrator.verifyPageLoading();
        
        assertThat(verified).isTrue();
    }
    
    @Test
    void testVerifyPageLoading_missingPages() {
        // Create only 100 pages
        for (int i = 1; i <= 100; i++) {
            PageCategory category = PageCategory.fromPageNumber(i);
            PageContent page = PageContent.builder()
                    .pageNumber(i)
                    .category(category)
                    .rawText("Test text page " + i)
                    .build();
            pageContentRepository.save(page);
        }
        
        boolean verified = workflowOrchestrator.verifyPageLoading();
        
        assertThat(verified).isFalse();
    }
    
    @Test
    void testVerifyCleanup_pageContentPreserved() {
        // Create 1831 PageContent entities
        for (int i = 1; i <= 1831; i++) {
            PageCategory category = PageCategory.fromPageNumber(i);
            PageContent page = PageContent.builder()
                    .pageNumber(i)
                    .category(category)
                    .rawText("Test text page " + i)
                    .build();
            pageContentRepository.save(page);
        }
        
        boolean verified = workflowOrchestrator.verifyCleanup();
        
        assertThat(verified).isTrue();
    }
    
    @Test
    void testVerifyIngestion_allPagesIngested() {
        // Create PageContent for all pages and mark ingested
        for (int i = 1; i <= 1831; i++) {
            PageCategory category = PageCategory.fromPageNumber(i);
            PageContent page = PageContent.builder()
                    .pageNumber(i)
                    .category(category)
                    .rawText("Test text page " + i)
                    .build();
            page.markIngested();
            pageContentRepository.save(page);
        }
        
        boolean verified = workflowOrchestrator.verifyIngestion();
        
        assertThat(verified).isTrue();
    }
    
    @Test
    void testVerifyIngestion_someUnprocessed() {
        // Create pages but don't mark them as ingested
        for (int i = 7; i <= 100; i++) {
            PageContent page = PageContent.builder()
                    .pageNumber(i)
                    .category(PageCategory.OAHSPE_BOOKS)
                    .rawText("Test text page " + i)
                    .build();
            pageContentRepository.save(page);
        }
        
        boolean verified = workflowOrchestrator.verifyIngestion();
        
        assertThat(verified).isFalse();
    }
    
    @Test
    void testVerifyIngestion_ignoresNonIngestPages() {
        // Create cover pages (should not be ingested)
        for (int i = 1; i <= 3; i++) {
            PageContent page = PageContent.builder()
                    .pageNumber(i)
                    .category(PageCategory.COVER)
                    .rawText("Cover page " + i)
                    .build();
            // Don't mark as ingested
            pageContentRepository.save(page);
        }
        
        // Verification should pass because cover pages shouldn't be ingested
        boolean verified = workflowOrchestrator.verifyIngestion();
        
        assertThat(verified).isTrue();
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
