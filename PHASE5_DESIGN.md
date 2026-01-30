# Phase 5 Design - REST API & Persistence Layer

**Phase:** 5 - REST API & Persistence Layer  
**Status:** Design Phase Complete  
**Date:** 2026-01-30  
**Duration:** 1.5 hours (estimated)

---

## Executive Summary

Phase 5 extends the Phase 4 validation framework with REST API endpoints for remote validation requests, async processing, progress tracking, and persistent storage of validation results. The implementation leverages Spring Boot's REST capabilities with async/await patterns and JPA for data persistence.

---

## Phase 5 Objectives

1. ✅ Build REST API endpoints for validation service
2. ✅ Implement async validation with progress tracking
3. ✅ Add database persistence layer
4. ✅ Create DTOs for request/response
5. ✅ Implement error handling and validation
6. ✅ Document OpenAPI/Swagger contracts
7. ✅ Achieve 100% test pass rate
8. ✅ Maintain >90% code coverage

---

## API Design

### REST Endpoints

#### 1. Submit Validation Request
```
POST /api/v1/validation/submit
Content-Type: application/json

Request Body:
{
  "bookId": 1,
  "chapterId": 2,
  "verseId": 3,
  "validateNotes": true,
  "validateImages": true
}

Response (202 Accepted):
{
  "requestId": "uuid-12345",
  "status": "PROCESSING",
  "createdAt": "2026-01-30T10:30:00Z"
}
```

**Responsibilities:**
- Validate request payload
- Create validation request record
- Initiate async validation
- Return 202 ACCEPTED with request ID
- Store in database

---

#### 2. Get Validation Status
```
GET /api/v1/validation/{requestId}

Response (200 OK):
{
  "requestId": "uuid-12345",
  "status": "IN_PROGRESS",
  "progress": {
    "current": 15,
    "total": 50,
    "percentComplete": 30
  },
  "startedAt": "2026-01-30T10:30:00Z"
}

OR

Response (200 OK - Complete):
{
  "requestId": "uuid-12345",
  "status": "COMPLETED",
  "progress": {
    "current": 50,
    "total": 50,
    "percentComplete": 100
  },
  "result": {
    "totalIssues": 5,
    "errorCount": 2,
    "warningCount": 3,
    "infoCount": 0,
    "issues": [...]
  },
  "completedAt": "2026-01-30T10:35:00Z"
}
```

**Responsibilities:**
- Retrieve validation request from database
- Return current status and progress
- Return final result when complete
- Handle request not found (404)

---

#### 3. Get Validation Result
```
GET /api/v1/validation/{requestId}/result

Response (200 OK):
{
  "requestId": "uuid-12345",
  "status": "COMPLETED",
  "validationResult": {
    "totalIssues": 5,
    "errorCount": 2,
    "warningCount": 3,
    "infoCount": 0,
    "issues": [
      {
        "severity": "ERROR",
        "entityType": "VERSE",
        "entityId": 123,
        "rule": "VERSE_REFERENCE_INVALID",
        "message": "Verse reference is invalid",
        "suggestedFix": "Correct the verse reference format"
      }
    ]
  },
  "completedAt": "2026-01-30T10:35:00Z"
}
```

**Responsibilities:**
- Retrieve full validation result
- Return all validation issues
- Handle request not found (404)
- Handle incomplete validation (202)

---

#### 4. List Validation Requests
```
GET /api/v1/validation/requests?status=COMPLETED&limit=10&offset=0

Response (200 OK):
{
  "requests": [
    {
      "requestId": "uuid-12345",
      "status": "COMPLETED",
      "createdAt": "2026-01-30T10:30:00Z",
      "completedAt": "2026-01-30T10:35:00Z"
    }
  ],
  "pagination": {
    "total": 42,
    "limit": 10,
    "offset": 0
  }
}
```

**Responsibilities:**
- List validation requests with filtering
- Support pagination
- Return summary info for each request

---

## Data Model

### ValidationRequest Entity
```java
@Entity
@Table(name = "validation_requests")
public class ValidationRequest {
    @Id
    private String requestId;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ValidationStatus status;
    
    @Column(name = "book_id")
    private Integer bookId;
    
    @Column(name = "chapter_id")
    private Integer chapterId;
    
    @Column(name = "verse_id")
    private Integer verseId;
    
    @Column(name = "validate_notes")
    private Boolean validateNotes;
    
    @Column(name = "validate_images")
    private Boolean validateImages;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "validationRequest", cascade = CascadeType.ALL)
    private List<ValidationProgressUpdate> progressUpdates;
    
    @OneToOne(mappedBy = "validationRequest", cascade = CascadeType.ALL)
    private ValidationResult result;
}
```

---

### ValidationProgressUpdate Entity
```java
@Entity
@Table(name = "validation_progress")
public class ValidationProgressUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "request_id")
    private ValidationRequest validationRequest;
    
    @Column(name = "current_item")
    private Integer currentItem;
    
    @Column(name = "total_items")
    private Integer totalItems;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
```

---

### ValidationResult Entity
```java
@Entity
@Table(name = "validation_results")
public class ValidationResult {
    @Id
    private String requestId;
    
    @OneToOne
    @JoinColumn(name = "request_id")
    private ValidationRequest validationRequest;
    
    @Column(name = "total_issues")
    private Integer totalIssues;
    
    @Column(name = "error_count")
    private Integer errorCount;
    
    @Column(name = "warning_count")
    private Integer warningCount;
    
    @Column(name = "info_count")
    private Integer infoCount;
    
    @OneToMany(mappedBy = "validationResult", cascade = CascadeType.ALL)
    private List<ValidationIssueRecord> issues;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
```

---

### ValidationIssueRecord Entity
```java
@Entity
@Table(name = "validation_issue_records")
public class ValidationIssueRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "result_id")
    private ValidationResult validationResult;
    
    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    @Column(name = "entity_type")
    private String entityType;
    
    @Column(name = "entity_id")
    private Integer entityId;
    
    @Column(name = "rule")
    private String rule;
    
    @Column(name = "message")
    private String message;
    
    @Column(name = "suggested_fix")
    private String suggestedFix;
}
```

---

## DTO Design

### ValidationRequestDTO
```java
public class ValidationRequestDTO {
    private Integer bookId;
    private Integer chapterId;
    private Integer verseId;
    private Boolean validateNotes;
    private Boolean validateImages;
}
```

### ValidationResponseDTO
```java
public class ValidationResponseDTO {
    private String requestId;
    private ValidationStatus status;
    private LocalDateTime createdAt;
}
```

### ValidationStatusDTO
```java
public class ValidationStatusDTO {
    private String requestId;
    private ValidationStatus status;
    private ProgressDTO progress;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private ValidationResultDTO result;
}

public class ProgressDTO {
    private Integer current;
    private Integer total;
    private Integer percentComplete;
}
```

### ValidationResultDTO
```java
public class ValidationResultDTO {
    private String requestId;
    private ValidationStatus status;
    private Integer totalIssues;
    private Integer errorCount;
    private Integer warningCount;
    private Integer infoCount;
    private List<ValidationIssueDTO> issues;
    private LocalDateTime completedAt;
}

public class ValidationIssueDTO {
    private String severity;
    private String entityType;
    private Integer entityId;
    private String rule;
    private String message;
    private String suggestedFix;
}
```

---

## Service Design

### ValidationRequestService
**Responsibilities:**
- Create validation request
- Submit validation task (async)
- Update progress
- Retrieve request status
- Store result

**Methods:**
- `submitValidation(ValidationRequestDTO): ValidationResponseDTO`
- `getValidationStatus(String requestId): ValidationStatusDTO`
- `getValidationResult(String requestId): ValidationResultDTO`
- `listValidationRequests(status, limit, offset): Page<ValidationRequest>`

---

### AsyncValidationService
**Responsibilities:**
- Process validation asynchronously
- Update progress callback
- Store final result
- Handle errors and timeouts

**Methods:**
- `processValidationAsync(String requestId, ValidationRequestDTO): CompletableFuture<Void>`
- `updateProgress(String requestId, int current, int total): void`
- `storeResult(String requestId, ValidationResult): void`

---

### ValidationRepository
**Responsibilities:**
- Database operations for validation requests
- Query by status
- Pagination support

**Methods:**
- `save(ValidationRequest): ValidationRequest`
- `findById(String requestId): Optional<ValidationRequest>`
- `findByStatus(ValidationStatus): List<ValidationRequest>`
- `findByStatusPageable(ValidationStatus, Pageable): Page<ValidationRequest>`

---

## Controller Design

### ValidationController
**Endpoints:**
1. `POST /api/v1/validation/submit` → Submit validation
2. `GET /api/v1/validation/{requestId}` → Get status
3. `GET /api/v1/validation/{requestId}/result` → Get result
4. `GET /api/v1/validation/requests` → List requests

**Responsibilities:**
- Request validation
- DTO conversion
- Error handling (400, 404, 500)
- Response formatting

---

## Error Handling

### HTTP Status Codes
- `200 OK` - Success (completed result)
- `202 ACCEPTED` - Async request accepted
- `400 BAD REQUEST` - Invalid request format
- `404 NOT FOUND` - Request not found
- `500 INTERNAL SERVER ERROR` - Server error

### Error Response Format
```json
{
  "timestamp": "2026-01-30T10:30:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Invalid request format",
  "details": {
    "field": "bookId",
    "issue": "must be positive"
  }
}
```

---

## Async Processing Flow

### Request Lifecycle
```
1. Client submits validation request via POST
   └─ ValidationController.submitValidation()
   
2. Controller validates request
   └─ Check required fields
   └─ Return 202 ACCEPTED with requestId
   
3. ValidationRequestService creates entity
   └─ Generate UUID
   └─ Store in database with status=PROCESSING
   
4. AsyncValidationService starts processing
   └─ @Async method processes validation
   └─ Calls Phase 4 validator
   └─ Updates progress periodically
   
5. Client polls for status via GET
   └─ ValidationController.getStatus()
   └─ Returns current progress
   
6. Processing completes
   └─ Stores result in database
   └─ Updates status=COMPLETED
   
7. Client retrieves result via GET
   └─ ValidationController.getResult()
   └─ Returns full validation result
```

---

## Technology Stack

- **Spring Boot:** 4.0.2
- **Spring Data JPA:** For data persistence
- **Spring Web:** REST controller support
- **H2 Database:** In-memory for testing
- **Async Support:** @Async, CompletableFuture
- **Validation:** Phase 4 OahspeDataValidator
- **Testing:** JUnit 5, Mockito
- **API Documentation:** Springdoc OpenAPI

---

## Implementation Strategy

### Phase 5.2 (Core Implementation)
1. Create repository interfaces (ValidationRequestRepository, ValidationResultRepository)
2. Create entities (ValidationRequest, ValidationResult, ValidationProgressUpdate, ValidationIssueRecord)
3. Create DTOs (ValidationRequestDTO, ValidationStatusDTO, ValidationResultDTO, ValidationIssueDTO)
4. Create service classes (ValidationRequestService, AsyncValidationService)
5. Create REST controller (ValidationController)
6. Implement error handling and exception handling
7. Create 5 atomic commits

### Phase 5.3 (Test Fixtures)
1. Create test data builders
2. Set up H2 in-memory database
3. Create mock factories

### Phase 5.4 (Testing)
1. Service layer tests (15+ tests)
2. Controller tests (8+ tests)
3. Integration tests (5+ tests)

### Phase 5.5 (Documentation)
1. OpenAPI/Swagger documentation
2. Usage guide with curl examples
3. Integration guide with Phase 4
4. Architecture update

---

## Success Criteria

- [ ] 4+ REST endpoints implemented
- [ ] Async validation working
- [ ] Progress tracking functional
- [ ] Database persistence working
- [ ] DTOs properly mapping
- [ ] Error handling comprehensive
- [ ] 100% test pass rate
- [ ] >90% code coverage
- [ ] 1000+ lines documentation
- [ ] 5+ atomic commits
- [ ] Zero regressions (Phase 1-4 tests passing)

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Async complexity | Use proven Spring @Async patterns |
| DB schema mismatch | Verify against Phase 1-3 entities first |
| Progress tracking accuracy | Test with multiple concurrent requests |
| Test flakiness | Use fixed timestamps, avoid timing dependencies |

---

## Integration with Phase 4

Phase 5 builds directly on Phase 4's validation framework:
- Uses `OahspeDataValidator` from Phase 4
- Uses `ValidationResult` from Phase 4
- Uses `ValidationIssue` from Phase 4
- No breaking changes to Phase 4 APIs

---

## OpenAPI Contract (Preview)

```yaml
openapi: 3.0.0
info:
  title: Oahspe Validation API
  version: 1.0.0
  description: REST API for Oahspe data validation

servers:
  - url: http://localhost:8080/api/v1

paths:
  /validation/submit:
    post:
      summary: Submit validation request
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ValidationRequest'
      responses:
        '202':
          description: Validation accepted for processing
        '400':
          description: Invalid request format

  /validation/{requestId}:
    get:
      summary: Get validation status
      parameters:
        - name: requestId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Current validation status
        '404':
          description: Request not found

  /validation/{requestId}/result:
    get:
      summary: Get validation result
      responses:
        '200':
          description: Validation result (only if complete)
        '202':
          description: Validation still in progress

  /validation/requests:
    get:
      summary: List validation requests
      parameters:
        - name: status
          in: query
          schema:
            type: string
        - name: limit
          in: query
          schema:
            type: integer
        - name: offset
          in: query
          schema:
            type: integer
      responses:
        '200':
          description: List of validation requests
```

---

## Timeline Estimate

- Design (current): 1.5 hours ✅
- Implementation: 2 hours
- Test Fixtures: 0.5 hours
- Testing: 1 hour
- Documentation: 1 hour
- Validation & Git: 0.5 hours
- Completion: 0.5 hours
- **Total: 6 hours**

---

## Notes

**Key Design Decisions:**
1. UUID for request IDs (ensures unique, distributed-friendly IDs)
2. Async processing with polling (avoids WebSocket complexity)
3. Separate progress and result storage (allows efficient querying)
4. DTOs for API boundary (decouples schema from API)
5. Spring JPA for persistence (leverages existing framework)

**Lessons Applied from Phase 4:**
- ✅ Design-first approach (this document)
- ✅ Verify assumptions against existing code
- ✅ Clear responsibility separation
- ✅ Comprehensive documentation
- ✅ Test-driven development planned

---

## Next Steps

1. ✅ Design complete (this document)
2. ⏳ Code review & approval
3. ⏳ Begin Phase 5.2: Core Implementation
4. ⏳ Create entities and repositories
5. ⏳ Create services and DTOs
6. ⏳ Create REST controller
7. ⏳ Write comprehensive tests
8. ⏳ Document API with Swagger
9. ⏳ Create atomic commits
10. ⏳ Phase completion & cleanup

---

*Phase 5 Design Document*  
*Status: COMPLETE*  
*Ready for Implementation*  
*Date: 2026-01-30*

