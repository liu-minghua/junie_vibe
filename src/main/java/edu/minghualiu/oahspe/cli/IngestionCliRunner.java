package edu.minghualiu.oahspe.cli;

import edu.minghualiu.oahspe.entities.ContentLinkingReport;
import edu.minghualiu.oahspe.entities.WorkflowState;
import edu.minghualiu.oahspe.ingestion.linker.ContentPageLinkingService;
import edu.minghualiu.oahspe.ingestion.linker.PageIngestionLinker;
import edu.minghualiu.oahspe.ingestion.loader.PageLoader;
import edu.minghualiu.oahspe.ingestion.runner.IngestionContext;
import edu.minghualiu.oahspe.ingestion.runner.OahspeIngestionRunner;
import edu.minghualiu.oahspe.ingestion.runner.ProgressCallback;
import edu.minghualiu.oahspe.ingestion.util.PdfPageUtil;
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

            // Manual test: Phase 1 only
            case "--test-phase1":
                runTestPhase1();
                break;

            // Manual test: cleanup PageContent + PageImage only
            case "--cleanup-pages":
                {
                    boolean skipPrompt = args.length > 1 && "--confirm".equals(args[1]);
                    runCleanupPages(skipPrompt);
                    break;
                }

            case "--workflow":
                runFullWorkflow();
                break;

            case "--load-pages":
                runPageLoading();
                break;

            case "--ingest-pages":
                runIngestion();
                break;

            case "--classify-books":
                classifyOahspeBooks();
                break;

            case "--verify-links":
                runVerification();
                break;

            case "--cleanup":
                {
                    boolean skipPrompt = args.length > 1 && "--confirm".equals(args[1]);
                    runCleanup(skipPrompt);
                    break;
                }

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
                log.error("Unknown command: {}", command);
                printHelp();
                break;
        }
        
        // Exit application after command completes
        System.exit(0);
    }

    // ---------- Manual test helpers ----------

    /**
     * Manual Test: Phase 1 only (load pages, no cleanup, no ingestion).
     */
    private void runTestPhase1() {
        log.info("=".repeat(80));
        log.info("MANUAL TEST: Phase 1 ONLY (Load Pages)");
        log.info("=".repeat(80));
        log.info("PDF File: {}", PdfPageUtil.getPdfPath());
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            IngestionContext context = workflowOrchestrator.runPhase1Only();

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

    /**
     * Manual Test Cleanup:
     * Deletes ALL PageContent + PageImage rows.
     * Does NOT affect Books/Chapters/Verses/etc.
     */
    private void runCleanupPages(boolean skipPrompt) {
        log.info("=".repeat(80));
        log.info("MANUAL TEST CLEANUP: PageContent + PageImage");
        log.info("=".repeat(80));
        log.warn("");
        log.warn("⚠ WARNING: This will DELETE ALL PageContent and PageImage rows!");
        log.warn("  - This is ONLY for manual testing of Phase 1");
        log.warn("  - Books/Chapters/Verses/etc. will NOT be touched");
        log.warn("");

        if (!skipPrompt) {
            System.out.print("Are you sure you want to delete ALL PageContent + PageImage? (yes/no): ");
            Scanner scanner = new Scanner(System.in);
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if (!confirmation.equals("yes")) {
                log.info("Cleanup cancelled.");
                return;
            }
        } else {
            log.info("--confirm flag provided, skipping interactive prompt");
        }

        log.info("");
        log.info("Proceeding with PageContent + PageImage cleanup...");

        try {
            dataCleanup.cleanupPageContentsAndImagesForTesting();

            log.info("");
            log.info("=".repeat(80));
            log.info("✓ PAGE CONTENT CLEANUP COMPLETE!");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ PAGE CONTENT CLEANUP FAILED!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Page content cleanup failed", e);
        }
    }

    // ---------- Shared helpers ----------

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
        log.info("  --test-phase1              Manual Test: Phase 1 only (load pages)");
        log.info("  --cleanup-pages [--confirm] Delete ALL PageContent + PageImage (manual testing only)");
        log.info("");
        log.info("  --workflow                 Run complete 3-phase workflow");
        log.info("  --load-pages               Phase 1: Load all pages from PDF");
        log.info("  --classify-books           Classify all Oahspe Book pages (7-1668)");
        log.info("  --ingest-pages             Phase 3: Ingest loaded pages");
        log.info("  --verify-links             Verify content-page linking");
        log.info("  --cleanup [--confirm]      Phase 2: Delete ingested domain data");
        log.info("  --resume <workflow-name>   Resume an interrupted workflow");
        log.info("");
        log.info("  --help, -h                 Show this help message");
        log.info("");
        log.info("=".repeat(80));
        log.info("");
    }

    // ---------- Existing workflow commands ----------

    private void runFullWorkflow() {
        log.info("=".repeat(80));
        log.info("PHASE 7 WORKFLOW: Complete 3-Phase Ingestion");
        log.info("=".repeat(80));
        log.info("PDF File: {}", PdfPageUtil.getPdfPath());
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
                WorkflowState workflow = workflowOrchestrator.executeFullWorkflow(
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

    private void runPageLoading() {
        log.info("=".repeat(80));
        log.info("PHASE 1: Loading Pages from PDF");
        log.info("=".repeat(80));
        log.info("PDF File: {}", PdfPageUtil.getPdfPath());
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            IngestionContext context = pageLoader.loadAllPages(createProgressCallback());

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

    private void classifyOahspeBooks() {
        log.info("=".repeat(80));
        log.info("CLASSIFICATION: Oahspe Book Pages (7-1668)");
        log.info("=".repeat(80));
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            edu.minghualiu.oahspe.enums.PageCategory bookCategory = edu.minghualiu.oahspe.enums.PageCategory.OAHSPE_BOOKS;
            int startPage = bookCategory.getStartPage();
            int endPage = bookCategory.getEndPage();

            log.info("Classifying pages {} to {}", startPage, endPage);
            log.info("");

            int classifiedCount = pageLoader.classifyPageRange(startPage, endPage);

            long duration = System.currentTimeMillis() - startTime;

            log.info("");
            log.info("=".repeat(80));
            log.info("✓ CLASSIFICATION COMPLETE!");
            log.info("=".repeat(80));
            log.info("Duration: {} ms ({} seconds)", duration, duration / 1000.0);
            log.info("Pages Classified: {}/{}", classifiedCount, (endPage - startPage + 1));
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("✗ CLASSIFICATION FAILED!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Page classification failed", e);
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
        log.warn("⚠ WARNING: This will DELETE all ingested domain data!");
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
            log.info("--confirm flag provided, skipping interactive prompt");
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
        log.warn("Note: Consider using --workflow for new Phase 7 workflow");
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            IngestionContext context = ingestionRunner.ingestPdf(pdfPath);

            long duration = System.currentTimeMillis() - startTime;

            log.info("=".repeat(80));
            log.info("Ingestion Complete!");
            log.info("=".repeat(80));
            log.info("Duration: {} ms ({} seconds)", duration, duration / 1000.0);
            log.info("Results:");
            log.info("  Pages Processed: {}", context.getTotalPages());
            log.info("  Images Extracted: {}", context.getTotalImagesExtracted());
            log.info("  Duplicate Images Skipped: {}", context.getDuplicateImagesSkipped());
            log.info("  Orphaned Verses: {}", context.getOrphanedVersesCount());
            log.info("  Events Processed: {}", context.getTotalEventsProcessed());
            log.info("  Page Errors: {}", context.getPageErrors().size());

            if (!context.getPageErrors().isEmpty()) {
                log.warn("Errors encountered:");
                context.getPageErrors().forEach(error ->
                        log.warn("  {}", error)
                );
            }

            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("Ingestion Failed!");
            log.error("=".repeat(80));
            log.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Legacy ingestion failed", e);
        }
    }
}
