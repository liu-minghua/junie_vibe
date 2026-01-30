package edu.minghualiu.oahspe.service;

import edu.minghualiu.oahspe.dto.ValidationRequestDTO;
import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.ingestion.validator.OahspeDataValidator;
import edu.minghualiu.oahspe.ingestion.validator.ValidationResult;
import edu.minghualiu.oahspe.ingestion.validator.ValidationProgressCallback;
import edu.minghualiu.oahspe.ingestion.validator.ValidationIssue;
import edu.minghualiu.oahspe.ingestion.validator.Severity;
import edu.minghualiu.oahspe.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Async service for processing validation requests.
 * Handles long-running validation operations asynchronously.
 */
@Service
public class AsyncValidationService {

    @Autowired
    private OahspeDataValidator validator;

    @Autowired
    private ValidationRequestService validationRequestService;

    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private VerseRepository verseRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private ValidationResultRepository validationResultRepository;

    /**
     * Process validation asynchronously.
     *
     * @param requestId the validation request ID
     * @param requestDTO the validation request data
     */
    @Async
    @Transactional
    public void processValidationAsync(String requestId, ValidationRequestDTO requestDTO) {
        try {
            // Create progress callback for tracking
            ValidationProgressCallback progressCallback = new ValidationProgressCallback() {
                @Override
                public void onValidationStart(int totalEntities) {
                    // Start progress at 0
                }

                @Override
                public void onEntityValidated(String entityType, int count) {
                    // Update progress
                    validationRequestService.updateProgress(requestId, count, count);
                }

                @Override
                public void onValidationComplete(ValidationResult result) {
                    // Validation complete - will be stored separately
                }
            };

            // Perform validation using Phase 4 validator
            ValidationResult validationResult = validator.validateAll(progressCallback);

            // Store result
            if (validationResult != null) {
                storeValidationResult(requestId, validationResult);
            }

        } catch (Exception e) {
            // Handle error - mark request as failed
            System.err.println("Error processing validation for request " + requestId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Store validation result in database.
     */
    private void storeValidationResult(String requestId, ValidationResult validationResult) {
        // Create result entity
        ValidationResultEntity resultEntity = new ValidationResultEntity(
            requestId,
            validationResult.getTotalIssuesFound(),
            validationResult.getErrorIssues().size(),
            validationResult.getWarningIssues().size(),
            validationResult.getIssuesBySeverity(Severity.INFO).size(),
            LocalDateTime.now()
        );

        // Convert issues to records
        List<ValidationIssueRecord> issueRecords = new ArrayList<>();
        for (ValidationIssue issue : validationResult.getAllIssues()) {
            ValidationIssueRecord record = new ValidationIssueRecord(
                issue.getSeverity().toString(),
                issue.getEntityType(),
                Math.toIntExact(issue.getEntityId()),
                issue.getRule(),
                issue.getMessage(),
                issue.getSuggestedFix()
            );
            record.setValidationResult(resultEntity);
            issueRecords.add(record);
        }

        resultEntity.setIssues(issueRecords);
        validationResultRepository.save(resultEntity);

        // Update request with result
        validationRequestService.storeResult(requestId, resultEntity);
    }
}
