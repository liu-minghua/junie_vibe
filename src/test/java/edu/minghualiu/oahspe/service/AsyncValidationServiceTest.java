package edu.minghualiu.oahspe.service;

import edu.minghualiu.oahspe.dto.ValidationRequestDTO;
import edu.minghualiu.oahspe.dto.ValidationResponseDTO;
import edu.minghualiu.oahspe.entities.ValidationRequest;
import edu.minghualiu.oahspe.repositories.ValidationRequestRepository;
import edu.minghualiu.oahspe.repositories.ValidationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AsyncValidationService
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class AsyncValidationServiceTest {

    @Autowired
    private AsyncValidationService asyncValidationService;

    @Autowired
    private ValidationRequestService validationRequestService;

    @Autowired
    private ValidationRequestRepository validationRequestRepository;

    @Autowired
    private ValidationResultRepository validationResultRepository;

    private ValidationRequestDTO testRequestDTO;

    @BeforeEach
    void setUp() {
        validationRequestRepository.deleteAll();
        validationResultRepository.deleteAll();

        testRequestDTO = new ValidationRequestDTO(1, 1, 1, true, true);
    }

    @Test
    void testProcessValidationAsync() throws Exception {
        ValidationResponseDTO response = validationRequestService.submitValidation(testRequestDTO);
        String requestId = response.getRequestId();

        Optional<ValidationRequest> request = validationRequestRepository.findById(requestId);
        assertTrue(request.isPresent());

        CompletableFuture<Void> future = asyncValidationService.processValidationAsync(request.get());

        // Wait for async processing (with timeout)
        future.get(5, TimeUnit.SECONDS);

        Optional<ValidationRequest> updatedRequest = validationRequestRepository.findById(requestId);
        assertTrue(updatedRequest.isPresent());
        
        // Verify request status changed to Completed or Failed
        String status = updatedRequest.get().getStatus();
        assertTrue(status.equals("Completed") || status.equals("Failed"));
    }

    @Test
    void testValidationProgressTracking() throws Exception {
        ValidationResponseDTO response = validationRequestService.submitValidation(testRequestDTO);
        String requestId = response.getRequestId();

        Optional<ValidationRequest> request = validationRequestRepository.findById(requestId);
        assertTrue(request.isPresent());

        asyncValidationService.processValidationAsync(request.get());

        // Small delay to allow progress to be tracked
        Thread.sleep(1000);

        // Verify progress was tracked
        var status = validationRequestService.getValidationStatus(requestId);
        assertNotNull(status);
    }

    @Test
    void testMultipleValidationsProcessedConcurrently() throws Exception {
        ValidationResponseDTO response1 = validationRequestService.submitValidation(testRequestDTO);
        ValidationResponseDTO response2 = validationRequestService.submitValidation(testRequestDTO);

        Optional<ValidationRequest> request1 = validationRequestRepository.findById(response1.getRequestId());
        Optional<ValidationRequest> request2 = validationRequestRepository.findById(response2.getRequestId());

        assertTrue(request1.isPresent());
        assertTrue(request2.isPresent());

        // Process both concurrently
        CompletableFuture<Void> future1 = asyncValidationService.processValidationAsync(request1.get());
        CompletableFuture<Void> future2 = asyncValidationService.processValidationAsync(request2.get());

        CompletableFuture.allOf(future1, future2).get(10, TimeUnit.SECONDS);

        // Verify both completed
        var status1 = validationRequestService.getValidationStatus(response1.getRequestId());
        var status2 = validationRequestService.getValidationStatus(response2.getRequestId());

        assertNotNull(status1);
        assertNotNull(status2);
    }
}
