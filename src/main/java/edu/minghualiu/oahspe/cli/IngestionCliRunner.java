package edu.minghualiu.oahspe.cli;

import edu.minghualiu.oahspe.ingestion.runner.IngestionContext;
import edu.minghualiu.oahspe.ingestion.runner.OahspeIngestionRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Command-line interface for manual testing of PDF ingestion.
 * Runs automatically when Spring Boot starts if a PDF path is provided.
 * 
 * Usage:
 *   mvn spring-boot:run -Dspring-boot.run.arguments="data/OAHSPE_Standard_Edition.pdf"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionCliRunner implements CommandLineRunner {
    
    private final OahspeIngestionRunner ingestionRunner;
    
    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            log.info("No PDF file specified. Skipping ingestion.");
            log.info("Usage: mvn spring-boot:run -Dspring-boot.run.arguments=\"data/OAHSPE_Standard_Edition.pdf\"");
            return;
        }
        
        String pdfPath = args[0];
        log.info("=".repeat(80));
        log.info("Starting Oahspe PDF Ingestion");
        log.info("=".repeat(80));
        log.info("PDF File: {}", pdfPath);
        
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
