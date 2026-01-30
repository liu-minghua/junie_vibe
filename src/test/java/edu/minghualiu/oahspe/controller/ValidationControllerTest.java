package edu.minghualiu.oahspe.controller;

import edu.minghualiu.oahspe.dto.ValidationRequestDTO;
import edu.minghualiu.oahspe.dto.ValidationResponseDTO;
import edu.minghualiu.oahspe.service.ValidationRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ValidationController REST endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class ValidationControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ValidationRequestService validationRequestService;

    @Test
    void testSubmitValidation() {
        ValidationRequestDTO request = new ValidationRequestDTO(1, 1, 1, true, true);

        ResponseEntity<ValidationResponseDTO> response = restTemplate.postForEntity(
            "/api/validation/submit", request, ValidationResponseDTO.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getRequestId());
        assertEquals("Processing", response.getBody().getStatus());
    }

    @Test
    void testGetValidationStatus() {
        ValidationRequestDTO request = new ValidationRequestDTO(1, 1, 1, true, true);
        ValidationResponseDTO submitted = validationRequestService.submitValidation(request);

        ResponseEntity<?> response = restTemplate.getForEntity(
            "/api/validation/{id}", Object.class, submitted.getRequestId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetValidationStatus_NotFound() {
        ResponseEntity<?> response = restTemplate.getForEntity(
            "/api/validation/{id}", Object.class, "non-existent-id");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testListValidationRequests() {
        ResponseEntity<?> response = restTemplate.getForEntity(
            "/api/validation/requests?page=0&size=10", Object.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
