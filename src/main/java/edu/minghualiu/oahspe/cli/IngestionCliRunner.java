package edu.minghualiu.oahspe.cli;

import edu.minghualiu.oahspe.entities.ContentLinkingReport;
import edu.minghualiu.oahspe.entities.WorkflowState;
import edu.minghualiu.oahspe.ingestion.linker.ContentPageLinkingService;
import edu.minghualiu.oahspe.ingestion.linker.PageIngestionLinker;
import edu.minghualiu.oahspe.ingestion.loader.PageLoader;
import edu.minghualiu.oahspe.ingestion.runner.IngestionContext;
import edu.minghualiu.oahspe.ingestion.runner.OahspeIngestionRunner;
import edu.minghualiu.oahspe.ingestion.runner.ProgressCallback;
import edu.minghualiu.oahspe.ingestion.workflow.IngestionDataCleanup;
import edu.minghualiu.oahspe.ingestion.workflow.WorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionCliRunner implements CommandLineRunner {

    private final OahspeIngestionRunner ingestionRunner;
    private final WorkflowOrchestrator workflowOrchestrator;
    private final PageLoader pageLoader;
    private final PageIngestionLinker pageIngestionLinker;
    private final IngestionDataCleanup dataCleanup;
    private final ContentPageLinkingService linkingService;

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String command = args[0];

        switch (command) {

            /* ============================================================
             * NEW MANUAL TEST OPTION
             * ============================================================ */
            case "--test-phase1":
                if (args.length < 2) {
                    log.error("Missing PDF path. Usage: --test-phase1 <pdf-path>");
                    return;
                }
                runTestPhase1(args[1]);
                break;

            case "--workflow":
                if (args.length < 2) {
                    log.error("Missing PDF path. Usage: --workflow <pdf-path>");
                    return;
                }
                runFullWorkflow(args[1]);
                break;

            case "--load-pages":
                if (args.length < 2) {
                    log.error("Missing PDF path. Usage: --load-pages <pdf-path>");
                    return;
                }
                runPageLoading(args[1]);
                break;

            case "--ingest-pages":
                runIngestion();
                break;

            case "--verify-links":
                runVerification();
                break;

            case "--cleanup":
                boolean skipPrompt = args.length > 1 && "--confirm".equals(args[1]);
                runCleanup(skipPrompt);
                break;

            case "--resume":
                if (args.length < 2) {
                    log.error("Missing workflow name. Usage: --resume <workflow-name>");
                    return;
                }
                resumeWorkflow(args[1]);
                break;

            case "--help":
            case "-h":
                printHelp();
                break;

            default:
                if (command.startsWith("--")) {
                    log.error("Unknown command: {}", command);
                    printHelp();
                } else {
                    runLegacyIngestion(command);
                }
                break;
        }
    }

    /* ============================================================
     * NEW MANUAL TEST METHOD
     * ============================================================ */

    /**
     * Manual Test: Phase 1 only (load pages, no cleanup, no ingestion).
     */
    private void runTestPhase1(String pdfPath) {
        log.info("=".repeat(80));
        log.info("MANUAL TEST: Phase 1 ONLY (Load Pages)");
        log.info("=".repeat(80));
        log.info("PDF File: {}", pdfPath);
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            IngestionContext context = workflowOrchestrator.runPhase1Only(pdfPath);

            long duration = System.currentTimeMillis() - startTime;

            log.info("");
            log.info("=".repeat(80));
            log.info("✓ MANUAL TEST COMPLETE: Phase 1 Only");
            log.info("=".repeat(80));
            log.info("Duration: {} ms ({} seconds)", duration, duration / 1000.0);
            log.info("Pages Loaded: {}", context.getTotalPages());
            log.info("Errors: {}", context.getTotalErrorsEncountered());

            if (!context.getPageErrors().isEmpty()) {
                log.warn("");
                log.warn("Errors encountered:");
                context.getPageErrors().forEach(error -> log.warn("  {}", error));
            }

            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ MANUAL TEST FAILED (Phase 1 Only)");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Manual test failed", e);
        }
    }

    /* ============================================================
     * EXISTING METHODS BELOW (UNCHANGED)
     * ============================================================ */

    private ProgressCallback createProgressCallback() {
        return new ProgressCallback() {
            @Override
            public void onPageStart(int pageNumber, int totalPages) {
                if (pageNumber % 50 == 0) {
                    log.info("[{}/{}] Starting page {}", pageNumber, totalPages, pageNumber);
                }
            }

            @Override
            public void onPageComplete(int pageNumber, int eventsProcessed) {
                if (pageNumber % 50 == 0) {
                    log.info("[{}/{}] Completed page {}", pageNumber, eventsProcessed, pageNumber);
                }
            }

            @Override
            public void onPageError(int pageNumber, Exception exception) {
                log.error("[Error] Page {}: {}", pageNumber, exception.getMessage());
            }

            @Override
            public void onIngestionComplete(IngestionContext context) {
                log.info("Ingestion complete: {} events processed, {} errors",
                        context.getTotalEventsProcessed(), context.getTotalErrorsEncountered());
            }
        };
    }

    private void printHelp() {
        log.info("");
        log.info("=".repeat(80));
        log.info("Oahspe PDF Ingestion CLI - Phase 7");
        log.info("=".repeat(80));
        log.info("");
        log.info("USAGE:");
        log.info("  mvn spring-boot:run -Dspring-boot.run.arguments=\"<command> [args]\"");
        log.info("");
        log.info("COMMANDS:");
        log.info("");
        log.info("  --test-phase1 <pdf>        Manual Test: Phase 1 only (load pages)");
        log.info("");
        log.info("  --workflow <pdf>           Run complete 3-phase workflow");
        log.info("  --load-pages <pdf>         Phase 1: Load pages");
        log.info("  --ingest-pages             Phase 3: Ingest content");
        log.info("  --verify-links             Verify content-page linking");
        log.info("  --cleanup [--confirm]      Phase 2: Cleanup old data");
        log.info("  --resume <workflow-name>   Resume workflow");
        log.info("  <pdf>                      Legacy ingestion");
        log.info("  --help, -h                 Show help");
        log.info("");
        log.info("=".repeat(80));
        log.info("");
    }

    private void runFullWorkflow(String pdfPath) {
        log.info("=".repeat(80));
        log.info("PHASE 7 WORKFLOW: Complete 3-Phase Ingestion");
        log.info("=".repeat(80));
        log.info("PDF File: {}", pdfPath);
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            WorkflowState workflow = workflowOrchestrator.executeFullWorkflow(
                    pdfPath,
                    createProgressCallback()
            );

            long duration = System.currentTimeMillis() - startTime;

            log.info("");
            log.info("=".repeat(80));
            log.info("✓ WORKFLOW COMPLETE!");
            log.info("=".repeat(80));
            log.info("Duration: {} ms ({} seconds)", duration, duration / 1000.0);
            log.info("Status: {}", workflow.getStatus());
            log.info("Statistics: {}", workflow.getStatistics());
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ WORKFLOW FAILED!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Workflow execution failed", e);
        }
    }

    private void runPageLoading(String pdfPath) {
        log.info("=".repeat(80));
        log.info("PHASE 1: Loading Pages from PDF");
        log.info("=".repeat(80));
        log.info("PDF File: {}", pdfPath);
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            IngestionContext context = pageLoader.loadAllPages(pdfPath, createProgressCallback());

            long duration = System.currentTimeMillis() - startTime;

            log.info("");
            log.info("=".repeat(80));
            log.info("✓ PAGE LOADING COMPLETE!");
            log.info("=".repeat(80));
            log.info("Duration: {} ms ({} seconds)", duration, duration / 1000.0);
            log.info("Pages Loaded: {}", context.getTotalPages());
            log.info("Errors: {}", context.getTotalErrorsEncountered());

            if (!context.getPageErrors().isEmpty()) {
                log.warn("");
                log.warn("Errors encountered:");
                context.getPageErrors().forEach(error -> log.warn("  {}", error));
            }

            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ PAGE LOADING FAILED!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Page loading failed", e);
        }
    }

    private void runIngestion() {
        log.info("=".repeat(80));
        log.info("PHASE 3: Ingesting Content from Loaded Pages");
        log.info("=".repeat(80));
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            IngestionContext context = pageIngestionLinker.ingestAllPageContents(createProgressCallback());

            long duration = System.currentTimeMillis() - startTime;

            log.info("");
            log.info("=".repeat(80));
            log.info("✓ CONTENT INGESTION COMPLETE!");
            log.info("=".repeat(80));
            log.info("Duration: {} ms ({} seconds)", duration, duration / 1000.0);
            log.info("Events Processed: {}", context.getTotalEventsProcessed());
            log.info("Errors: {}", context.getTotalErrorsEncountered());

            if (!context.getPageErrors().isEmpty()) {
                log.warn("");
                log.warn("Errors encountered:");
                context.getPageErrors().forEach(error -> log.warn("  {}", error));
            }

            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ CONTENT INGESTION FAILED!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Content ingestion failed", e);
        }
    }

    private void runVerification() {
        log.info("=".repeat(80));
        log.info("VERIFICATION: Content-Page Linking Report");
        log.info("=".repeat(80));
        log.info("");

        try {
            ContentLinkingReport report = linkingService.linkAllContentToPages();

            log.info(report.getSummary());

            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ VERIFICATION FAILED!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Verification failed", e);
        }
    }

    private void runCleanup(boolean skipPrompt) {
        log.info("=".repeat(80));
        log.info("PHASE 2: Data Cleanup");
        log.info("=".repeat(80));
        log.warn("");
        log.warn("⚠ WARNING: This will DELETE all ingested data!");
        log.warn("  - Books, Chapters, Verses, Notes");
        log.warn("  - Glossary Terms, Index Entries");
        log.warn("  - PageContent is preserved");
        log.warn("");

        if (!skipPrompt) {
            System.out.print("Are you sure you want to continue? (yes/no): ");
            Scanner scanner = new Scanner(System.in);
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if (!confirmation.equals("yes")) {
                log.info("Cleanup cancelled.");
                return;
            }
        } else {
            log.info("--confirm flag provided, skipping prompt");
        }

        log.info("");
        log.info("Proceeding with cleanup...");

        try {
            dataCleanup.cleanupAllIngestedData();

            log.info("");
            log.info("=".repeat(80));
            log.info("✓ CLEANUP COMPLETE!");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ CLEANUP FAILED!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Cleanup failed", e);
        }
    }

    private void resumeWorkflow(String workflowName) {
        log.info("=".repeat(80));
        log.info("RESUME WORKFLOW: {}", workflowName);
        log.info("=".repeat(80));
        log.info("");

        try {
            WorkflowState workflow = workflowOrchestrator.resumeWorkflow(workflowName);

            log.info("Workflow resumed from phase: {}", workflow.getCurrentPhase());
            log.info("Status: {}", workflow.getStatus());
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ RESUME FAILED!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Resume failed", e);
        }
    }

    private void runLegacyIngestion(String pdfPath) {
        log.info("=".repeat(80));
        log.info("LEGACY MODE: Old Ingestion Runner");
        log.info("=".repeat(80));
        log.info("PDF File: {}", pdfPath);
