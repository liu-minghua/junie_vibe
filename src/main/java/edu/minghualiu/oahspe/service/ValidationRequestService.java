package edu.minghualiu.oahspe.service;

import edu.minghualiu.oahspe.dto.*;
import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.repositories.ValidationRequestRepository;
import edu.minghualiu.oahspe.repositories.ValidationResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing validation requests.
 * Handles submission, status tracking, and result retrieval.
 */
@Service
@Transactional
public class ValidationRequestService {

    @Autowired
    private ValidationRequestRepository validationRequestRepository;

    @Autowired
    private ValidationResultRepository validationResultRepository;

    @Autowired
    private AsyncValidationService asyncValidationService;

    /**
     * Submit a new validation request.
     *
     * @param requestDTO the validation request data
     * @return response with request ID and status
     */
    public ValidationResponseDTO submitValidation(ValidationRequestDTO requestDTO) {
        // Generate unique request ID
        String requestId = UUID.randomUUID().toString();
        
        // Create validation request entity
        ValidationRequest request = new ValidationRequest(
            requestId,
            ValidationStatus.PROCESSING,
            requestDTO.getBookId(),
            requestDTO.getChapterId(),
            requestDTO.getVerseId(),
            requestDTO.getValidateNotes(),
            requestDTO.getValidateImages(),
            LocalDateTime.now()
        );
        
        // Save request to database
        validationRequestRepository.save(request);
        
        // Start async validation
        asyncValidationService.processValidationAsync(requestId, requestDTO);
        
        // Return response
        return new ValidationResponseDTO(
            requestId,
            ValidationStatus.PROCESSING.getDisplayName(),
            request.getCreatedAt()
        );
    }

    /**
     * Get the current status of a validation request.
     *
     * @param requestId the request ID
     * @return status with progress information
     */
    public ValidationStatusDTO getValidationStatus(String requestId) {
        Optional<ValidationRequest> requestOpt = validationRequestRepository.findById(requestId);
        
        if (requestOpt.isEmpty()) {
            throw new ValidationRequestNotFoundException("Request not found: " + requestId);
        }
        
        ValidationRequest request = requestOpt.get();
        
        // Get latest progress update
        ProgressDTO progress = null;
        if (!request.getProgressUpdates().isEmpty()) {
            ValidationProgressUpdate latestProgress = request.getProgressUpdates()
                .stream()
                .max((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .orElse(null);
            
            if (latestProgress != null) {
                progress = new ProgressDTO(latestProgress.getCurrentItem(), latestProgress.getTotalItems());
            }
        }
        
        ValidationStatusDTO status = new ValidationStatusDTO(
            requestId,
            request.getStatus().getDisplayName(),
            progress,
            request.getCreatedAt()
        );
        
        status.setCompletedAt(request.getCompletedAt());
        
        // Include result if validation is completed
        if (request.getResult() != null) {
            status.setResult(convertToValidationResultDTO(request.getResult()));
        }
        
        return status;
    }

    /**
     * Get the full validation result.
     *
     * @param requestId the request ID
     * @return validation result with all issues
     */
    public ValidationResultDTO getValidationResult(String requestId) {
        Optional<ValidationRequest> requestOpt = validationRequestRepository.findById(requestId);
        
        if (requestOpt.isEmpty()) {
            throw new ValidationRequestNotFoundException("Request not found: " + requestId);
        }
        
        ValidationRequest request = requestOpt.get();
        
        if (request.getResult() == null) {
            throw new ValidationIncompleteException("Validation still in progress for request: " + requestId);
        }
        
        return convertToValidationResultDTO(request.getResult());
    }

    /**
     * List validation requests with pagination and filtering.
     *
     * @param status the validation status to filter by (optional)
     * @param limit the page size
     * @param offset the page offset
     * @return page of validation requests
     */
    public Page<ValidationRequest> listValidationRequests(String status, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset, limit);
        
        if (status != null && !status.isEmpty()) {
            try {
                ValidationStatus validationStatus = ValidationStatus.valueOf(status.toUpperCase());
                return validationRequestRepository.findByStatus(validationStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }
        
        return validationRequestRepository.findAll(pageable);
    }

    /**
     * Update progress for a validation request.
     *
     * @param requestId the request ID
     * @param currentItem the current item number
     * @param totalItems the total number of items
     */
    public void updateProgress(String requestId, int currentItem, int totalItems) {
        Optional<ValidationRequest> requestOpt = validationRequestRepository.findById(requestId);
        
        if (requestOpt.isEmpty()) {
            return; // Silently ignore if request not found
        }
        
        ValidationRequest request = requestOpt.get();
        
        ValidationProgressUpdate progress = new ValidationProgressUpdate(
            request,
            currentItem,
            totalItems,
            LocalDateTime.now()
        );
        
        request.getProgressUpdates().add(progress);
        validationRequestRepository.save(request);
    }

    /**
     * Store validation result.
     *
     * @param requestId the request ID
     * @param result the validation result entity
     */
    public void storeResult(String requestId, ValidationResultEntity result) {
        Optional<ValidationRequest> requestOpt = validationRequestRepository.findById(requestId);
        
        if (requestOpt.isEmpty()) {
            return; // Silently ignore if request not found
        }
        
        ValidationRequest request = requestOpt.get();
        request.setResult(result);
        request.setStatus(ValidationStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        
        validationRequestRepository.save(request);
    }

    /**
     * Convert ValidationResultEntity to ValidationResultDTO.
     */
    private ValidationResultDTO convertToValidationResultDTO(ValidationResultEntity entity) {
        List<ValidationIssueDTO> issueDTOs = entity.getIssues().stream()
            .map(issue -> new ValidationIssueDTO(
                issue.getSeverity(),
                issue.getEntityType(),
                issue.getEntityId(),
                issue.getRule(),
                issue.getMessage(),
                issue.getSuggestedFix()
            ))
            .collect(Collectors.toList());
        
        return new ValidationResultDTO(
            entity.getRequestId(),
            "COMPLETED",
            entity.getTotalIssues(),
            entity.getErrorCount(),
            entity.getWarningCount(),
            entity.getInfoCount(),
            issueDTOs,
            entity.getCompletedAt()
        );
    }
}
