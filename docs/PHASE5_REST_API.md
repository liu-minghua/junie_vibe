# Phase 5: REST API Documentation

**⚠️ DEPRECATED - This validation API has been removed as of Phase 7.**

**Reason:** The async validation REST API was not needed for the actual workflow. Phase 7's WorkflowOrchestrator includes built-in verification gates that handle validation internally without exposing a REST API. Users do not need to interact with validation.

**Replaced by:** Phase 7 verification gates (automatic, CLI-driven, no REST API needed)

**Files Removed:** 29 files (~3,288 LOC) including ValidationController, services, repositories, entities, DTOs, validators

---

## Historical Documentation (for reference only)

## Overview
Phase 5 implemented a comprehensive REST API for asynchronous text validation requests. The API followed REST conventions with proper HTTP status codes and async processing patterns.

## Base URL
```
/api/validation
```

## API Endpoints

### 1. Submit Validation Request
**Endpoint:** `POST /api/validation/submit`

**Description:** Submit a new validation request for asynchronous processing.

**Request Body:**
```json
{
  "bookId": 1,
  "chapterId": 1,
  "verseId": 1,
  "checkFormatting": true,
  "checkCompleteness": true
}
```

**Response (202 ACCEPTED):**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "Processing",
  "createdAt": "2024-01-30T10:15:30Z"
}
```

**Example (curl):**
```bash
curl -X POST http://localhost:8080/api/validation/submit \
  -H "Content-Type: application/json" \
  -d '{"bookId": 1, "chapterId": 1, "verseId": 1, "checkFormatting": true, "checkCompleteness": true}'
```

---

### 2. Get Validation Status
**Endpoint:** `GET /api/validation/{id}`

**Description:** Get the current status of a validation request.

**Path Parameters:**
- `id` (required): The validation request ID returned from submit

**Response (200 OK):**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "Processing",
  "progress": {
    "current": 25,
    "total": 100,
    "percentComplete": 25.0
  },
  "createdAt": "2024-01-30T10:15:30Z",
  "completedAt": null,
  "result": null
}
```

**Status Values:**
- `PROCESSING` - Validation is in progress
- `COMPLETED` - Validation completed successfully
- `FAILED` - Validation failed with errors
- `CANCELLED` - Validation was cancelled

**Example (curl):**
```bash
curl -X GET http://localhost:8080/api/validation/550e8400-e29b-41d4-a716-446655440000
```

---

### 3. Get Validation Result
**Endpoint:** `GET /api/validation/{id}/result`

**Description:** Get the complete validation result. Only available after validation completes.

**Path Parameters:**
- `id` (required): The validation request ID

**Response (200 OK):**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "Completed",
  "totalIssues": 5,
  "errorCount": 2,
  "warningCount": 2,
  "infoCount": 1,
  "issues": [
    {
      "severity": "ERROR",
      "entityType": "Verse",
      "entityId": 42,
      "rule": "VERSE_COMPLETENESS",
      "message": "Verse is incomplete",
      "suggestedFix": "Add missing content"
    },
    {
      "severity": "WARNING",
      "entityType": "Chapter",
      "entityId": 3,
      "rule": "FORMATTING_CONSISTENCY",
      "message": "Inconsistent formatting detected",
      "suggestedFix": "Apply standard formatting"
    }
  ],
  "completedAt": "2024-01-30T10:20:15Z"
}
```

**Example (curl):**
```bash
curl -X GET http://localhost:8080/api/validation/550e8400-e29b-41d4-a716-446655440000/result
```

---

### 4. List All Validation Requests
**Endpoint:** `GET /api/validation/requests`

**Description:** List all validation requests with optional filtering and pagination.

**Query Parameters:**
- `status` (optional): Filter by status (PROCESSING, COMPLETED, FAILED, CANCELLED)
- `page` (optional, default: 0): Page number for pagination
- `size` (optional, default: 10): Number of items per page

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "bookId": 1,
      "chapterId": 1,
      "verseId": 1,
      "status": "Completed",
      "createdAt": "2024-01-30T10:15:30Z",
      "completedAt": "2024-01-30T10:20:15Z"
    }
  ],
  "totalPages": 1,
  "totalElements": 1,
  "currentPage": 0
}
```

**Examples (curl):**
```bash
# List all requests (page 0, 10 per page)
curl -X GET "http://localhost:8080/api/validation/requests?page=0&size=10"

# List only completed requests
curl -X GET "http://localhost:8080/api/validation/requests?status=COMPLETED&page=0&size=10"

# List processing requests
curl -X GET "http://localhost:8080/api/validation/requests?status=PROCESSING"
```

---

## HTTP Status Codes

| Code | Meaning | Use Case |
|------|---------|----------|
| 200  | OK | Successful GET/list requests |
| 202  | Accepted | Validation request submitted for async processing |
| 400  | Bad Request | Invalid request body or parameters |
| 404  | Not Found | Validation request ID not found |
| 500  | Internal Server Error | Server error during processing |

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid request",
  "message": "Book ID is required"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Validation request not found: abc-123-xyz"
}
```

### 202 Incomplete (Request Still Processing)
```json
{
  "error": "Validation Incomplete",
  "message": "Validation request abc-123-xyz is still being processed"
}
```

---

## Request/Response DTOs

### ValidationRequestDTO
- `bookId` (Integer, required): Book ID to validate
- `chapterId` (Integer, required): Chapter ID  
- `verseId` (Integer, required): Verse ID
- `checkFormatting` (Boolean): Whether to check formatting rules
- `checkCompleteness` (Boolean): Whether to check completeness rules

### ValidationResponseDTO
- `requestId` (String): UUID of the validation request
- `status` (String): "Processing"
- `createdAt` (LocalDateTime): Submission timestamp

### ValidationStatusDTO
- `requestId` (String): Validation request ID
- `status` (String): Current status
- `progress` (ProgressDTO): Progress information
- `createdAt` (LocalDateTime): Submission time
- `completedAt` (LocalDateTime): Completion time (null if incomplete)
- `result` (ValidationResultDTO): Result details (null if incomplete)

### ValidationResultDTO
- `requestId` (String): Validation request ID
- `status` (String): Final status
- `totalIssues` (Integer): Total number of issues found
- `errorCount` (Integer): Number of error-severity issues
- `warningCount` (Integer): Number of warning-severity issues
- `infoCount` (Integer): Number of info-severity issues
- `issues` (List<ValidationIssueDTO>): List of found issues
- `completedAt` (LocalDateTime): Completion timestamp

### ValidationIssueDTO
- `severity` (String): "ERROR", "WARNING", or "INFO"
- `entityType` (String): Type of entity (Book, Chapter, Verse, etc.)
- `entityId` (Integer): ID of the entity with the issue
- `rule` (String): The validation rule that failed
- `message` (String): Human-readable issue description
- `suggestedFix` (String): Suggestion for fixing the issue

### ProgressDTO
- `current` (Integer): Currently processed items
- `total` (Integer): Total items to process
- `percentComplete` (Double): Percentage complete (0-100)

---

## Processing Flow

### Typical Validation Workflow

1. **Submit Request**
   ```bash
   POST /api/validation/submit
   → 202 ACCEPTED with requestId
   ```

2. **Poll for Status (Optional)**
   ```bash
   GET /api/validation/{requestId}
   → 200 OK with status and progress
   ```

3. **Get Results (After Completion)**
   ```bash
   GET /api/validation/{requestId}/result
   → 200 OK with complete results
   ```

### Async Processing
- Requests are queued and processed asynchronously
- Server returns immediately with 202 ACCEPTED
- Use polling or webhooks to monitor progress
- Results available via `/result` endpoint after completion

---

## Database Schema

### validation_request
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| book_id | INTEGER | Reference to book |
| chapter_id | INTEGER | Reference to chapter |
| verse_id | INTEGER | Reference to verse |
| status | VARCHAR | Current status |
| created_at | TIMESTAMP | Submission time |
| completed_at | TIMESTAMP | Completion time |

### validation_result
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| request_id | UUID | Foreign key to validation_request |
| total_issues | INTEGER | Total issues found |
| error_count | INTEGER | Error-level issues |
| warning_count | INTEGER | Warning-level issues |
| info_count | INTEGER | Info-level issues |
| completed_at | TIMESTAMP | Completion time |

### validation_issue_record
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| request_id | UUID | Foreign key to validation_request |
| severity | VARCHAR | Issue severity |
| entity_type | VARCHAR | Entity being validated |
| entity_id | INTEGER | ID of entity |
| rule | VARCHAR | Validation rule violated |
| message | VARCHAR | Issue description |
| suggested_fix | VARCHAR | Fix suggestion |

---

## Implementation Details

### Architecture
- **Framework**: Spring Boot 4.0.2
- **Data Access**: Spring Data JPA
- **HTTP**: Spring MVC REST
- **Async**: @Async with CompletableFuture
- **Validation**: Jakarta EE validation framework

### Key Classes
- `ValidationController`: REST endpoints
- `ValidationRequestService`: Business logic
- `AsyncValidationService`: Async processing
- `ValidationRequest`: JPA entity
- `ValidationResult`: JPA entity  
- `ValidationIssueRecord`: JPA entity

### Integration with Phase 4
Phase 5 integrates with Phase 4's `OahspeDataValidator` for actual validation:
- Uses `validator.validateAll(progressCallback)`
- Processes validation results and issues
- Tracks progress via `ValidationProgressCallback`
- Persists issues to database

---

## Configuration

### application.properties
```properties
# Async processing
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=4
spring.task.execution.pool.queue-capacity=100

# Database (H2 for testing)
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

---

## Testing

### Unit Tests
- `ValidationRequestServiceTest`: Service business logic
- `AsyncValidationServiceTest`: Async processing
- `ValidationControllerTest`: REST endpoints

### Test Coverage
- All CRUD operations
- Error handling
- Validation logic
- Async processing
- Progress tracking

### Running Tests
```bash
# Run all tests
mvn clean test

# Run specific test class
mvn clean test -Dtest=ValidationControllerTest

# Run with coverage report
mvn clean test jacoco:report
```

---

## Error Handling

### Exception Handling
- `ValidationRequestNotFoundException`: Request ID not found (404)
- `ValidationIncompleteException`: Result not yet available (202)
- `IllegalArgumentException`: Invalid parameters (400)

### Validation Errors
All validation errors include:
- Error code
- User-friendly message
- Suggestions for fixes
- Timestamp of detection

---

## Future Enhancements

1. **Webhook Support**: Notify clients when validation completes
2. **Batch Processing**: Submit multiple validation requests in one call
3. **Export Results**: Download results in CSV/PDF format
4. **Real-time WebSocket Updates**: Push progress updates to clients
5. **Rate Limiting**: Implement rate limiting for API endpoints
6. **Authentication**: Add JWT or OAuth2 for API security
7. **Caching**: Cache validation results for identical requests

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-01-30 | Initial release with 4 main endpoints |

---
