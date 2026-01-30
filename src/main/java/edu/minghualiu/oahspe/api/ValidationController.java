package edu.minghualiu.oahspe.api;

import edu.minghualiu.oahspe.dto.*;
import edu.minghualiu.oahspe.entities.ValidationRequest;
import edu.minghualiu.oahspe.service.ValidationIncompleteException;
import edu.minghualiu.oahspe.service.ValidationRequestNotFoundException;
import edu.minghualiu.oahspe.service.ValidationRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for validation API endpoints.
 */
@RestController
@RequestMapping("/api/v1/validation")
@Validated
public class ValidationController {

    @Autowired
    private ValidationRequestService validationRequestService;

    /**
     * POST /api/v1/validation/submit
     * Submit a new validation request.
     * Returns 202 ACCEPTED with request ID.
     */
    @PostMapping("/submit")
    public ResponseEntity<ValidationResponseDTO> submitValidation(
            @Valid @RequestBody ValidationRequestDTO requestDTO) {
        
        ValidationResponseDTO response = validationRequestService.submitValidation(requestDTO);
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(response);
    }

    /**
     * GET /api/v1/validation/{requestId}
     * Get validation status and progress.
     * Returns 200 OK with status and progress.
     * If validation is complete, includes result.
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ValidationStatusDTO> getValidationStatus(
            @PathVariable String requestId) {
        
        try {
            ValidationStatusDTO status = validationRequestService.getValidationStatus(requestId);
            return ResponseEntity.ok(status);
        } catch (ValidationRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/v1/validation/{requestId}/result
     * Get full validation result.
     * Returns 200 OK with result (only if complete).
     * Returns 202 ACCEPTED if still processing.
     * Returns 404 NOT FOUND if request not found.
     */
    @GetMapping("/{requestId}/result")
    public ResponseEntity<ValidationResultDTO> getValidationResult(
            @PathVariable String requestId) {
        
        try {
            ValidationResultDTO result = validationRequestService.getValidationResult(requestId);
            return ResponseEntity.ok(result);
        } catch (ValidationRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ValidationIncompleteException e) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
    }

    /**
     * GET /api/v1/validation/requests
     * List validation requests with pagination and filtering.
     */
    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> listValidationRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        try {
            Page<ValidationRequest> page = validationRequestService.listValidationRequests(status, limit, offset);
            
            Map<String, Object> response = new HashMap<>();
            response.put("requests", page.getContent());
            response.put("pagination", Map.of(
                "total", page.getTotalElements(),
                "limit", limit,
                "offset", offset
            ));
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(ValidationRequestNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ValidationRequestNotFoundException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "NOT_FOUND");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Exception handler for incomplete validations.
     */
    @ExceptionHandler(ValidationIncompleteException.class)
    public ResponseEntity<Void> handleIncomplete(ValidationIncompleteException e) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "BAD_REQUEST");
        error.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
