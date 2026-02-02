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

    /* ============================================================
     *  MANUAL TESTING HELPERS (NEW)
     * ============================================================ */

    /**
     * Loads pages ONLY — no cleanup, no ingestion, no gates.
     */
    public IngestionContext runPhase1Only(String pdfPath) {
        log.info("=== MANUAL TEST: Phase 1 ONLY ===");
        IngestionContext ctx = pageLoader.loadAllPages(pdfPath, null);
        log.info("Phase 1 finished: {} pages loaded, {} errors",
                ctx.getTotalPages(), ctx.getTotalErrorsEncountered());
        return ctx;
    }

    /**
     * Loads pages and prints a summary, but does NOT enforce the 1831-page rule.
     */
    public IngestionContext runPhase1AndVerify(String pdfPath) {
        log.info("=== MANUAL TEST: Phase 1 + Summary ===");
        IngestionContext ctx = pageLoader.loadAllPages(pdfPath, null);

        long count = pageContentRepository.count();
        log.info("Loaded {} pages into PageContent", count);

        return ctx;
    }

    /**
     * Runs ingestion for a single category ONLY.
     * Useful for testing parsers without reloading pages.
     */
    public IngestionContext runPhase3Only(edu.minghualiu.oahspe.entities.PageCategory category) {
        log.info("=== MANUAL TEST: Phase 3 ONLY for {} ===", category);
        IngestionContext ctx = pageIngestionLinker.ingestCategoryPages(category, null);

        log.info("Phase 3 test complete: {} events processed, {} errors",
                ctx.getTotalEventsProcessed(), ctx.getTotalErrorsEncountered());

        return ctx;
    }

    /**
     * Runs all phases but SKIPS strict gate checks.
     * Useful for incremental testing.
     */
    public WorkflowState runFullWorkflowRelaxed(String pdfPath) {
        log.info("=== MANUAL TEST: Full Workflow (Relaxed Mode) ===");

        WorkflowState workflow = initWorkflow();

        // Phase 1
        executePhase1(pdfPath, workflow, null);

        // Phase 2 (no gate)
        executePhase2(workflow, null);

        // Phase 3 (no gate)
        executePhase3(workflow, null);

        workflow.markCompleted();
        workflowStateRepository.save(workflow);

        log.info("Relaxed workflow completed.");
        return workflow;
    }

    /* ============================================================
     *  ORIGINAL PRODUCTION WORKFLOW (UNCHANGED)
     * ============================================================ */

    public WorkflowState executeFullWorkflow(String pdfPath, ProgressCallback callback) {
        log.info("Starting full ingestion workflow for PDF: {}", pdfPath);

        WorkflowState workflow = initWorkflow();

        try {
            if (workflow.getCurrentPhase().ordinal() <= WorkflowPhase.PAGE_LOADING.ordinal()) {
                executePhase1(pdfPath, workflow, callback);

                if (!verifyPageLoading()) {
                    throw new RuntimeException("Phase 1 verification failed: Not all pages loaded");
                }
            }

            if (workflow.getCurrentPhase().ordinal() <= WorkflowPhase.CLEANUP.ordinal()) {
                executePhase2(workflow, callback);

                if (!verifyCleanup()) {
                    throw new RuntimeException("Phase 2 verification failed: Old data still exists");
                }
            }

            if (workflow.getCurrentPhase().ordinal() <= WorkflowPhase.CONTENT_INGESTION.ordinal()) {
                executePhase3(workflow, callback);

                if (!verifyIngestion()) {
                    throw new RuntimeException("Phase 3 verification failed: Not all pages ingested");
                }
            }

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

    /* ============================================================
     *  INTERNAL HELPERS
     * ============================================================ */

    private WorkflowState initWorkflow() {
        return workflowStateRepository
                .findByWorkflowName(DEFAULT_WORKFLOW_NAME)
                .orElse(WorkflowState.builder()
                        .workflowName(DEFAULT_WORKFLOW_NAME)
                        .currentPhase(WorkflowPhase.PAGE_LOADING)
                        .status(WorkflowStatus.NOT_STARTED)
                        .build());
    }

    private void executePhase1(String pdfPath, WorkflowState workflow, ProgressCallback callback) {
        log.info("=== Phase 1: Loading pages from PDF ===");
        workflow.updatePhase(WorkflowPhase.PAGE_LOADING);
        workflowStateRepository.save(workflow);

        IngestionContext context = pageLoader.loadAllPages(pdfPath, callback);

        if (!context.isSuccessful()) {
            throw new RuntimeException("Phase 1 failed with " + context.getTotalErrorsEncountered() + " errors");
        }
    }

    private void executePhase2(WorkflowState workflow, ProgressCallback callback) {
        log.info("=== Phase 2: Cleaning up old data ===");
        workflow.updatePhase(WorkflowPhase.CLEANUP);
        workflowStateRepository.save(workflow);

        dataCleanup.cleanupAllIngestedData();
    }

    private void executePhase3(WorkflowState workflow, ProgressCallback callback) {
        log.info("=== Phase 3: Ingesting content ===");
        workflow.updatePhase(WorkflowPhase.CONTENT_INGESTION);
        workflowStateRepository.save(workflow);

        pageIngestionLinker.ingestCategoryPages(
                edu.minghualiu.oahspe.entities.PageCategory.OAHSPE_BOOKS, callback);

        pageIngestionLinker.ingestCategoryPages(
                edu.minghualiu.oahspe.entities.PageCategory.GLOSSARIES, callback);

        pageIngestionLinker.ingestCategoryPages(
                edu.minghualiu.oahspe.entities.PageCategory.INDEX, callback);
    }

    private boolean verifyPageLoading() {
        return pageContentRepository.count() == EXPECTED_TOTAL_PAGES;
    }

    private boolean verifyCleanup() {
        return pageContentRepository.count() == EXPECTED_TOTAL_PAGES;
    }

    private boolean verifyIngestion() {
        List<edu.minghualiu.oahspe.entities.PageContent> unprocessed =
                pageContentRepository.findByIngestedFalseOrderByPageNumberAsc();

        return unprocessed.stream()
                .filter(pc -> pc.getCategory().shouldIngest())
                .count() == 0;
    }

    private String generateStatistics() {
        long totalPages = pageContentRepository.count();
        long ingestedPages = pageContentRepository.findByIngestedFalseOrderByPageNumberAsc().size();

        return String.format("Total pages: %d, Ingested: %d",
                totalPages, EXPECTED_TOTAL_PAGES - ingestedPages);
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

}
