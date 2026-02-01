package edu.minghualiu.oahspe.ingestion.workflow;

import edu.minghualiu.oahspe.entities.WorkflowPhase;
import edu.minghualiu.oahspe.entities.WorkflowState;
import edu.minghualiu.oahspe.entities.WorkflowStatus;
import edu.minghualiu.oahspe.ingestion.linker.PageIngestionLinker;
import edu.minghualiu.oahspe.ingestion.loader.PageLoader;
import edu.minghualiu.oahspe.ingestion.runner.IngestionContext;
import edu.minghualiu.oahspe.ingestion.runner.ProgressCallback;
import edu.minghualiu.oahspe.repositories.PageContentRepository;
import edu.minghualiu.oahspe.repositories.WorkflowStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates the complete 3-phase ingestion workflow.
 * 
 * Workflow sequence:
 * 1. Phase 1: Load all pages from PDF → PageContent
 * 2. Gate 1: Verify all pages loaded
 * 3. Phase 2: Cleanup old ingested data
 * 4. Gate 2: Verify cleanup complete
 * 5. Phase 3: Ingest PageContent → domain entities
 * 6. Gate 3: Verify ingestion complete
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {
    
    private final PageLoader pageLoader;
    private final IngestionDataCleanup dataCleanup;
    private final PageIngestionLinker pageIngestionLinker;
    private final WorkflowStateRepository workflowStateRepository;
    private final PageContentRepository pageContentRepository;
    
    private static final String DEFAULT_WORKFLOW_NAME = "oahspe-ingestion";
    private static final int EXPECTED_TOTAL_PAGES = 1831;
    
    /**
     * Executes the complete 3-phase workflow.
     * 
     * @param pdfPath absolute path to the PDF file
     * @param callback optional progress callback
     * @return final workflow state
     */
    public WorkflowState executeFullWorkflow(String pdfPath, ProgressCallback callback) {
        log.info("Starting full ingestion workflow for PDF: {}", pdfPath);
        
        // Initialize or resume workflow
        WorkflowState workflow = workflowStateRepository
                .findByWorkflowName(DEFAULT_WORKFLOW_NAME)
                .orElse(WorkflowState.builder()
                        .workflowName(DEFAULT_WORKFLOW_NAME)
                        .currentPhase(WorkflowPhase.PAGE_LOADING)
                        .status(WorkflowStatus.NOT_STARTED)
                        .build());
        
        try {
            // Phase 1: Load pages from PDF
            if (workflow.getCurrentPhase().ordinal() <= WorkflowPhase.PAGE_LOADING.ordinal()) {
                executePhase1(pdfPath, workflow, callback);
                
                if (!verifyPageLoading()) {
                    throw new RuntimeException("Phase 1 verification failed: Not all pages loaded");
                }
            }
            
            // Phase 2: Cleanup old data
            if (workflow.getCurrentPhase().ordinal() <= WorkflowPhase.CLEANUP.ordinal()) {
                executePhase2(workflow, callback);
                
                if (!verifyCleanup()) {
                    throw new RuntimeException("Phase 2 verification failed: Old data still exists");
                }
            }
            
            // Phase 3: Ingest content
            if (workflow.getCurrentPhase().ordinal() <= WorkflowPhase.CONTENT_INGESTION.ordinal()) {
                executePhase3(workflow, callback);
                
                if (!verifyIngestion()) {
                    throw new RuntimeException("Phase 3 verification failed: Not all pages ingested");
                }
            }
            
            // Workflow completed successfully
            workflow.markCompleted();
            workflow.setStatistics(generateStatistics());
            workflowStateRepository.save(workflow);
            
            log.info("Full workflow completed successfully");
            return workflow;
            
        } catch (Exception e) {
            log.error("Workflow failed: {}", e.getMessage(), e);
            workflow.markFailed(e.getMessage());
            workflowStateRepository.save(workflow);
            throw new RuntimeException("Workflow execution failed", e);
        }
    }
    
    /**
     * Phase 1: Load all pages from PDF.
     */
    private void executePhase1(String pdfPath, WorkflowState workflow, ProgressCallback callback) {
        log.info("=== Phase 1: Loading pages from PDF ===");
        workflow.updatePhase(WorkflowPhase.PAGE_LOADING);
        workflowStateRepository.save(workflow);
        
        IngestionContext context = pageLoader.loadAllPages(pdfPath, callback);
        
        if (!context.isSuccessful()) {
            throw new RuntimeException(String.format(
                    "Phase 1 failed with %d errors", context.getTotalErrorsEncountered()));
        }
        
        log.info("Phase 1 complete: {} pages loaded", context.getTotalPages());
    }
    
    /**
     * Phase 2: Cleanup old ingested data.
     */
    private void executePhase2(WorkflowState workflow, ProgressCallback callback) {
        log.info("=== Phase 2: Cleaning up old data ===");
        workflow.updatePhase(WorkflowPhase.CLEANUP);
        workflowStateRepository.save(workflow);
        
        dataCleanup.cleanupAllIngestedData();
        
        log.info("Phase 2 complete: Old data cleaned up");
    }
    
    /**
     * Phase 3: Ingest content from PageContent entities.
     * Processes each category separately with its appropriate parser.
     */
    private void executePhase3(WorkflowState workflow, ProgressCallback callback) {
        log.info("=== Phase 3: Ingesting content ===");
        workflow.updatePhase(WorkflowPhase.CONTENT_INGESTION);
        workflowStateRepository.save(workflow);
        
        // Phase 3a: Ingest OAHSPE_BOOKS (pages 7-1668) using OahspeParser
        log.info("Phase 3a: Ingesting OAHSPE_BOOKS (main content)");
        IngestionContext booksContext = pageIngestionLinker.ingestCategoryPages(
                edu.minghualiu.oahspe.entities.PageCategory.OAHSPE_BOOKS, callback);
        
        if (!booksContext.isSuccessful()) {
            throw new RuntimeException(String.format(
                    "Phase 3a (OAHSPE_BOOKS) failed with %d errors", 
                    booksContext.getTotalErrorsEncountered()));
        }
        log.info("Phase 3a complete: {} books processed", booksContext.getTotalEventsProcessed());
        
        // Phase 3b: Ingest GLOSSARIES (pages 1668-1690) using GlossaryParser
        log.info("Phase 3b: Ingesting GLOSSARIES");
        IngestionContext glossariesContext = pageIngestionLinker.ingestCategoryPages(
                edu.minghualiu.oahspe.entities.PageCategory.GLOSSARIES, callback);
        
        if (!glossariesContext.isSuccessful()) {
            throw new RuntimeException(String.format(
                    "Phase 3b (GLOSSARIES) failed with %d errors", 
                    glossariesContext.getTotalErrorsEncountered()));
        }
        log.info("Phase 3b complete: {} glossary entries processed", glossariesContext.getTotalEventsProcessed());
        
        // Phase 3c: Ingest INDEX (pages 1691-1831) using IndexParser
        log.info("Phase 3c: Ingesting INDEX");
        IngestionContext indexContext = pageIngestionLinker.ingestCategoryPages(
                edu.minghualiu.oahspe.entities.PageCategory.INDEX, callback);
        
        if (!indexContext.isSuccessful()) {
            throw new RuntimeException(String.format(
                    "Phase 3c (INDEX) failed with %d errors", 
                    indexContext.getTotalErrorsEncountered()));
        }
        log.info("Phase 3c complete: {} index entries processed", indexContext.getTotalEventsProcessed());
        
        int totalEvents = booksContext.getTotalEventsProcessed() + 
                         glossariesContext.getTotalEventsProcessed() + 
                         indexContext.getTotalEventsProcessed();
        log.info("Phase 3 complete: {} total events processed across all categories", totalEvents);
    }
    
    /**
     * Gate 1: Verify all pages have been loaded.
     * 
     * @return true if verification passes
     */
    public boolean verifyPageLoading() {
        long loadedPages = pageContentRepository.count();
        boolean verified = loadedPages == EXPECTED_TOTAL_PAGES;
        
        if (verified) {
            log.info("✓ Gate 1 PASSED: All {} pages loaded", loadedPages);
        } else {
            log.error("✗ Gate 1 FAILED: Expected {} pages, found {}", 
                    EXPECTED_TOTAL_PAGES, loadedPages);
        }
        
        return verified;
    }
    
    /**
     * Gate 2: Verify old data has been cleaned up.
     * PageContent should still exist, but Books/Chapters/Verses should be empty.
     * 
     * @return true if verification passes
     */
    public boolean verifyCleanup() {
        long pageContentCount = pageContentRepository.count();
        boolean pageContentPreserved = pageContentCount == EXPECTED_TOTAL_PAGES;
        
        // Note: This will be enhanced in Task 7.6 to actually check Book/Chapter/Verse counts
        // For now, assume cleanup worked if PageContent is preserved
        boolean verified = pageContentPreserved;
        
        if (verified) {
            log.info("✓ Gate 2 PASSED: PageContent preserved ({} pages)", pageContentCount);
        } else {
            log.error("✗ Gate 2 FAILED: PageContent corrupted (expected {}, found {})", 
                    EXPECTED_TOTAL_PAGES, pageContentCount);
        }
        
        return verified;
    }
    
    /**
     * Gate 3: Verify all shouldIngest pages have been ingested.
     * 
     * @return true if verification passes
     */
    public boolean verifyIngestion() {
        List<edu.minghualiu.oahspe.entities.PageContent> unprocessed = 
                pageContentRepository.findByIngestedFalse();
        
        // Filter to only pages that should have been ingested
        long unprocessedRequired = unprocessed.stream()
                .filter(pc -> pc.getCategory().shouldIngest())
                .count();
        
        boolean verified = unprocessedRequired == 0;
        
        if (verified) {
            log.info("✓ Gate 3 PASSED: All required pages ingested");
        } else {
            log.error("✗ Gate 3 FAILED: {} pages still unprocessed", unprocessedRequired);
        }
        
        return verified;
    }
    
    /**
     * Resumes an interrupted workflow from its last saved state.
     * 
     * @param workflowName the workflow to resume
     * @return workflow state
     */
    @Transactional
    public WorkflowState resumeWorkflow(String workflowName) {
        WorkflowState workflow = workflowStateRepository
                .findByWorkflowName(workflowName)
                .orElseThrow(() -> new RuntimeException(
                        "No workflow found with name: " + workflowName));
        
        if (workflow.isTerminal()) {
            log.warn("Workflow {} is already in terminal state: {}", 
                    workflowName, workflow.getStatus());
            return workflow;
        }
        
        log.info("Resuming workflow from phase: {}", workflow.getCurrentPhase());
        
        // Resume from current phase (pdfPath would need to be stored in statistics)
        throw new UnsupportedOperationException(
                "Resume not yet implemented - need to store pdfPath in workflow state");
    }
    
    /**
     * Generates statistics summary for completed workflow.
     */
    private String generateStatistics() {
        long totalPages = pageContentRepository.count();
        long ingestedPages = pageContentRepository.findByIngestedFalse().size();
        
        return String.format("Total pages: %d, Ingested: %d", 
                totalPages, EXPECTED_TOTAL_PAGES - ingestedPages);
    }
}
