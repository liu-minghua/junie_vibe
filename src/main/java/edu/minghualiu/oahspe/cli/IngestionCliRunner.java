package edu.minghualiu.oahspe.cli;

import edu.minghualiu.oahspe.entities.ContentLinkingReport;
import edu.minghualiu.oahspe.entities.WorkflowState;
import edu.minghualiu.oahspe.ingestion.linker.ContentPageLinkingService;
import edu.minghualiu.oahspe.ingestion.linker.PageIngestionLinker;
import edu.minghualiu.oahspe.ingestion.loader.PageLoader;
import edu.minghualiu.oahspe.ingestion.runner.IngestionContext;
import edu.minghualiu.oahspe.ingestion.runner.OahspeIngestionRunner;
import edu.minghualiu.oahspe.ingestion.workflow.IngestionDataCleanup;
import edu.minghualiu.oahspe.ingestion.workflow.WorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Command-line interface for PDF ingestion with workflow orchestration.
 * 
 * Phase 7 Enhanced Usage:
 *   --workflow <pdf>           Run complete 3-phase workflow
 *   --load-pages <pdf>         Phase 1: Load pages from PDF
 *   --ingest-pages             Phase 3: Ingest loaded pages
 *   --verify-links             Verify content-page linking
 *   --cleanup                  Phase 2: Delete old data (with confirmation)
 *   --resume <workflow-name>   Resume interrupted workflow
 *   <pdf>                      Legacy: Run old ingestion (backward compatible)
 * 
 * Examples:
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--workflow data/OAHSPE.pdf"
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--load-pages data/OAHSPE.pdf"
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--cleanup"
 */
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
        
        // Route to appropriate handler
        switch (command) {
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
                runCleanup();
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
                // Legacy mode: assume it's a PDF path
                if (command.startsWith("--")) {
                    log.error("Unknown command: {}", command);
                    printHelp();
                } else {
                    runLegacyIngestion(command);
                }
                break;
        }
    }
    
    /**
     * Progress callback for workflow operations.
     */
    private void onProgress(int current, int total, String message) {
        if (current % 50 == 0 || current == total) {
            log.info("[{}/{}] {}", current, total, message);
        }
    }
    
    /**
     * Prints help information.
     */
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
        log.info("  --workflow <pdf>           Run complete 3-phase workflow");
        log.info("                             (load pages → cleanup → ingest → verify)");
        log.info("");
        log.info("  --load-pages <pdf>         Phase 1: Load all pages from PDF into database");
        log.info("                             Creates PageContent and PageImage entities");
        log.info("");
        log.info("  --ingest-pages             Phase 3: Ingest loaded pages into domain entities");
        log.info("                             Creates Books, Chapters, Verses, Notes, etc.");
        log.info("");
        log.info("  --verify-links             Verify content-page linking");
        log.info("                             Reports on pageNumber field population");
        log.info("");
        log.info("  --cleanup                  Phase 2: Delete old ingested data");
        log.info("                             (Requires confirmation, preserves PageContent)");
        log.info("");
        log.info("  --resume <workflow-name>   Resume an interrupted workflow");
        log.info("");
        log.info("  <pdf>                      Legacy mode: Run old ingestion");
        log.info("                             (Backward compatible)");
        log.info("");
        log.info("  --help, -h                 Show this help message");
        log.info("");
        log.info("EXAMPLES:");
        log.info("");
        log.info("  # Run complete workflow:");
        log.info("  mvn spring-boot:run -Dspring-boot.run.arguments=\"--workflow data/OAHSPE.pdf\"");
        log.info("");
        log.info("  # Load pages only:");
        log.info("  mvn spring-boot:run -Dspring-boot.run.arguments=\"--load-pages data/OAHSPE.pdf\"");
        log.info("");
        log.info("  # Cleanup and re-ingest:");
        log.info("  mvn spring-boot:run -Dspring-boot.run.arguments=\"--cleanup\"");
        log.info("  mvn spring-boot:run -Dspring-boot.run.arguments=\"--ingest-pages\"");
        log.info("");
        log.info("  # Verify linking:");
        log.info("  mvn spring-boot:run -Dspring-boot.run.arguments=\"--verify-links\"");
        log.info("");
        log.info("=".repeat(80));
        log.info("");
    }
    
    /**
     * Runs the complete 3-phase workflow.
     */
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
                    this::onProgress
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
    
    /**
     * Runs Phase 1: Load pages from PDF.
     */
    private void runPageLoading(String pdfPath) {
        log.info("=".repeat(80));
        log.info("PHASE 1: Loading Pages from PDF");
        log.info("=".repeat(80));
        log.info("PDF File: {}", pdfPath);
        log.info("");
        
        long startTime = System.currentTimeMillis();
        
        try {
            IngestionContext context = pageLoader.loadAllPages(pdfPath, this::onProgress);
            
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
    
    /**
     * Runs Phase 3: Ingest content from loaded pages.
     */
    private void runIngestion() {
        log.info("=".repeat(80));
        log.info("PHASE 3: Ingesting Content from Loaded Pages");
        log.info("=".repeat(80));
        log.info("");
        
        long startTime = System.currentTimeMillis();
        
        try {
            IngestionContext context = pageIngestionLinker.ingestAllPageContents(this::onProgress);
            
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
    
    /**
     * Runs verification of content-page linking.
     */
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
    
    /**
     * Runs Phase 2: Cleanup old data (with confirmation).
     */
    private void runCleanup() {
        log.info("=".repeat(80));
        log.info("PHASE 2: Data Cleanup");
        log.info("=".repeat(80));
        log.warn("");
        log.warn("⚠ WARNING: This will DELETE all ingested data!");
        log.warn("  - All Books, Chapters, Verses, Notes will be removed");
        log.warn("  - All Images, Glossary Terms, Index Entries will be removed");
        log.warn("  - PageContent will be PRESERVED for re-ingestion");
        log.warn("");
        
        // Request confirmation
        System.out.print("Are you sure you want to continue? (yes/no): ");
        Scanner scanner = new Scanner(System.in);
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (!confirmation.equals("yes")) {
            log.info("Cleanup cancelled.");
            return;
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
    
    /**
     * Resumes an interrupted workflow.
     */
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
    
    /**
     * Legacy ingestion mode for backward compatibility.
     */
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
            throw e;
        }
    }
}
