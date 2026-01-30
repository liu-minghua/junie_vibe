package edu.minghualiu.oahspe.service;

import edu.minghualiu.oahspe.dto.ValidationRequestDTO;
import edu.minghualiu.oahspe.dto.ValidationResponseDTO;
import edu.minghualiu.oahspe.dto.ValidationStatusDTO;
import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.repositories.ValidationRequestRepository;
import edu.minghualiu.oahspe.repositories.ValidationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationRequestService
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ValidationRequestServiceTest {

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

        testRequestDTO = new ValidationRequestDTO(
            1, 1, 1, true, true
        );
    }

    @Test
    void testSubmitValidation() {
        ValidationResponseDTO response = validationRequestService.submitValidation(testRequestDTO);

        assertNotNull(response);
        assertNotNull(response.getRequestId());
        assertEquals("Processing", response.getStatus());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void testGetValidationStatus_NotFound() {
        assertThrows(ValidationRequestNotFoundException.class, () -> {
            validationRequestService.getValidationStatus("non-existent-id");
        });
    }

    @Test
    void testGetValidationStatus_Found() {
        ValidationResponseDTO response = validationRequestService.submitValidation(testRequestDTO);
        String requestId = response.getRequestId();

        ValidationStatusDTO status = validationRequestService.getValidationStatus(requestId);

        assertNotNull(status);
        assertEquals(requestId, status.getRequestId());
        assertEquals("Processing", status.getStatus());
    }

    @Test
    void testUpdateProgress() {
        ValidationResponseDTO response = validationRequestService.submitValidation(testRequestDTO);
        String requestId = response.getRequestId();

        validationRequestService.updateProgress(requestId, 10, 50);

        ValidationStatusDTO status = validationRequestService.getValidationStatus(requestId);
        assertNotNull(status.getProgress());
        assertEquals(10, status.getProgress().getCurrent());
        assertEquals(50, status.getProgress().getTotal());
    }

    @Test
    void testListValidationRequests() {
        validationRequestService.submitValidation(testRequestDTO);
        validationRequestService.submitValidation(testRequestDTO);

        Page<ValidationRequest> page = validationRequestService.listValidationRequests(null, 10, 0);

        assertNotNull(page);
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void testListValidationRequestsByStatus() {
        validationRequestService.submitValidation(testRequestDTO);

        Page<ValidationRequest> page = validationRequestService.listValidationRequests("PROCESSING", 10, 0);

        assertNotNull(page);
        assertTrue(page.getTotalElements() > 0);
    }

    @Test
    void testGetValidationResult_NotFound() {
        assertThrows(ValidationRequestNotFoundException.class, () -> {
            validationRequestService.getValidationResult("non-existent-id");
        });
    }

    @Test
    void testGetValidationResult_Incomplete() {
        ValidationResponseDTO response = validationRequestService.submitValidation(testRequestDTO);
        String requestId = response.getRequestId();

        assertThrows(ValidationIncompleteException.class, () -> {
            validationRequestService.getValidationResult(requestId);
        });
    }

    @Test
    void testStoreResult() {
        ValidationResponseDTO response = validationRequestService.submitValidation(testRequestDTO);
        String requestId = response.getRequestId();

        ValidationResultEntity result = new ValidationResultEntity(
            requestId, 5, 2, 2, 1, LocalDateTime.now()
        );

        validationRequestService.storeResult(requestId, result);

        ValidationStatusDTO status = validationRequestService.getValidationStatus(requestId);
        assertEquals("Completed", status.getStatus());
        assertNotNull(status.getResult());
    }
}
