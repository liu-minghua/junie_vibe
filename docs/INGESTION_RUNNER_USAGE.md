# OahspeIngestionRunner Usage Guide

## Overview

The **OahspeIngestionRunner** is the orchestrator service for end-to-end PDF ingestion into the Oahspe database. It brings together three phases of implementation:

- **Phase 1 (OahspeParser):** Converts raw text into structured events
- **Phase 2 (OahspeIngestionService):** Transforms events into database entities
- **Phase 3 (OahspeIngestionRunner):** Orchestrates PDF extraction, parsing, and ingestion

**Key Features:**
- Complete workflow automation: PDF → Text → Events → Database
- Page-by-page processing for memory efficiency
- Error recovery with per-page tracking
- Optional progress monitoring via callbacks
- Transactional integrity for database operations

---

## Architecture Overview

```
PDF File
    ↓ [PDFTextExtractor + PDFBox]
String text (per page)
    ↓ [OahspeParser]
List<OahspeEvent>
    ↓ [OahspeIngestionService]
Database entities (Book, Chapter, Verse, Note, Image)
```

### Component Responsibilities

**OahspeIngestionRunner (Orchestrator)**
- Manages workflow coordination
- Handles page iteration
- Tracks ingestion progress via IngestionContext
- Invokes progress callbacks
- Recovers from page-level errors

**PDFTextExtractor**
- Opens PDF files using Apache PDFBox
- Extracts text from individual pages
- Handles file/format validation
- Throws PDFExtractionException on errors

**OahspeParser (from Phase 1)**
- Parses page text into OahspeEvent objects
- Recognizes: BookStart, ChapterStart, Verse, Note, ImageRef, PageBreak
- Maintains finite state machine for parsing context

**OahspeIngestionService (from Phase 2)**
- Consumes OahspeEvent objects
- Creates/updates database entities
- Manages entity relationships
- Maintains ingestion state between pages

---

## Basic Usage

### Simple PDF Ingestion

```java
@Autowired
private OahspeIngestionRunner runner;

public void ingestOahspePdf(String filePath) throws PDFExtractionException {
    // Process entire PDF without callbacks
    IngestionContext context = runner.ingestPdf(filePath);
    
    // Check results
    System.out.println("Pages processed: " + context.getTotalPages());
    System.out.println("Events created: " + context.getTotalEventsProcessed());
    System.out.println("Errors: " + context.getTotalErrorsEncountered());
    System.out.println("Success: " + context.isSuccessful());
    
    if (!context.isSuccessful()) {
        for (String error : context.getPageErrors()) {
            System.err.println(error);
        }
    }
}
```

### With Progress Monitoring

```java
public void ingestWithProgress(String filePath) throws PDFExtractionException {
    ProgressCallback callback = new ProgressCallback() {
        @Override
        public void onPageStart(int pageNumber, int totalPages) {
            System.out.printf("Processing page %d of %d%n", pageNumber, totalPages);
        }

        @Override
        public void onPageComplete(int pageNumber, int eventsProcessed) {
            System.out.printf("Page %d: %d events%n", pageNumber, eventsProcessed);
        }

        @Override
        public void onPageError(int pageNumber, Exception e) {
            System.err.printf("Page %d error: %s%n", pageNumber, e.getMessage());
        }

        @Override
        public void onIngestionComplete(IngestionContext context) {
            System.out.printf("Ingestion complete: %s%n", context.isSuccessful() 
                ? "SUCCESS" : "FAILED");
        }
    };
    
    IngestionContext context = runner.ingestPdfWithProgress(filePath, callback);
    // Process results...
}
```

### Minimal Example

```java
// Single line to ingest a PDF
IngestionContext result = runner.ingestPdf("/path/to/oahspe.pdf");
```

---

## Return Value: IngestionContext

The `IngestionContext` object contains complete ingestion metrics and diagnostics:

```java
// Diagnostic information
String pdfFilePath;          // Path to ingested PDF
int totalPages;              // Total pages in PDF
int currentPageNumber;       // Last page processed
long startTime;              // When ingestion started (ms epoch)

// Results
int totalEventsProcessed;    // Total OahspeEvents created
int totalErrorsEncountered;  // Number of errors
List<String> pageErrors;     // Per-page error messages

// Methods
boolean isSuccessful();      // true if totalErrorsEncountered == 0
long getElapsedTime();       // ms elapsed during ingestion
```

---

## Error Handling

### File-Level Errors (Thrown Immediately)

```java
try {
    runner.ingestPdf("nonexistent.pdf");
} catch (PDFExtractionException e) {
    // File not found, invalid PDF format, etc.
    System.err.println("PDF Error: " + e.getMessage());
    System.err.println("File: " + e.getPdfFilePath());
    System.err.println("Page: " + e.getPageNumber()); // 0 for file-level errors
}
```

**PDFExtractionException causes:**
- File not found
- Invalid PDF format
- PDF corrupted
- Access denied

### Page-Level Errors (Tracked in Context)

Page-level errors (from parser or ingestion) do not throw exceptions. Instead, they are:
1. Logged as warnings
2. Added to `context.pageErrors`
3. Processing continues to next page

```java
IngestionContext context = runner.ingestPdf(filePath);

if (!context.isSuccessful()) {
    System.out.println("Pages with errors:");
    for (String error : context.getPageErrors()) {
        System.out.println("  " + error);
    }
    // Example output:
    // Page 5: Unexpected content in state INSIDE_CHAPTER
    // Page 12: Database constraint violation
}
```

---

## Integration with Spring

### Bean Configuration

OahspeIngestionRunner is automatically registered as a Spring bean via `@Component` on PDFTextExtractor and dependency injection on the runner.

```java
// In your application configuration or controller
@Autowired
private OahspeIngestionRunner runner;

// Or via constructor injection (recommended)
public class OahspeController {
    private final OahspeIngestionRunner runner;
    
    public OahspeController(OahspeIngestionRunner runner) {
        this.runner = runner;
    }
}
```

### Transactional Semantics

Both `ingestPdf()` and `ingestPdfWithProgress()` are marked `@Transactional`. This ensures:
- Each page's events are atomically ingested
- If a page fails, its partial changes are rolled back
- Other pages continue processing
- No half-ingested pages in database

### Database Requirements

OahspeIngestionRunner requires the following repositories:
- BookRepository
- ChapterRepository
- VerseRepository
- NoteRepository
- ImageRepository

These are automatically injected by Spring.

---

## Advanced Usage

### Custom Progress Callback

```java
public class IngestionProgressTracker implements ProgressCallback {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final MetricsService metrics;
    
    @Override
    public void onPageStart(int pageNumber, int totalPages) {
        metrics.recordPageStart(pageNumber);
    }

    @Override
    public void onPageComplete(int pageNumber, int eventsProcessed) {
        logger.info("Page {} complete with {} events", pageNumber, eventsProcessed);
        metrics.recordPageComplete(pageNumber, eventsProcessed);
    }

    @Override
    public void onPageError(int pageNumber, Exception e) {
        logger.warn("Page {} failed: {}", pageNumber, e.getMessage(), e);
        metrics.recordPageError(pageNumber, e);
    }

    @Override
    public void onIngestionComplete(IngestionContext context) {
        logger.info("Ingestion finished: {} ms, {} events, {} errors",
            context.getElapsedTime(),
            context.getTotalEventsProcessed(),
            context.getTotalErrorsEncountered());
        metrics.recordIngestionComplete(context);
    }
}

// Usage
runner.ingestPdfWithProgress(filePath, new IngestionProgressTracker());
```

### Multiple PDFs

```java
public void ingestMultiplePdfs(List<String> pdfPaths) throws PDFExtractionException {
    for (String path : pdfPaths) {
        try {
            IngestionContext context = runner.ingestPdf(path);
            if (!context.isSuccessful()) {
                logger.warn("PDF {} had {} errors", path, context.getTotalErrorsEncountered());
            }
        } catch (PDFExtractionException e) {
            logger.error("Failed to ingest {}: {}", path, e.getMessage());
            // Continue with next PDF
        }
    }
}
```

### REST API Integration

```java
@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {
    @Autowired
    private OahspeIngestionRunner runner;

    @PostMapping("/ingest")
    public ResponseEntity<IngestionResponse> ingestPdf(
            @RequestParam String filePath) {
        try {
            IngestionContext context = runner.ingestPdf(filePath);
            return ResponseEntity.ok(new IngestionResponse(
                context.getTotalPages(),
                context.getTotalEventsProcessed(),
                context.getTotalErrorsEncountered(),
                context.isSuccessful(),
                context.getPageErrors()
            ));
        } catch (PDFExtractionException e) {
            return ResponseEntity.badRequest().body(
                new IngestionResponse(e.getMessage()));
        }
    }
}
```

---

## Performance Considerations

### Memory Efficiency

- **Page-by-page processing:** Only one page's text in memory at a time
- **Streaming text extraction:** PDFBox streams text without loading entire PDF into memory
- **Suitable for large PDFs:** Can process 1000+ page documents on modest hardware

### Processing Speed

- **Average throughput:** ~50-100 pages/minute (depends on page content)
- **Bottleneck:** Text extraction from complex PDFs
- **Optimization:** Parser performance is very fast; most time in PDF extraction

### Database Load

- **Transactional cost:** Each page commit incurs database transaction overhead
- **Batch considerations:** Large PDFs may benefit from page batching (not currently implemented)
- **Connection pooling:** Ensure connection pool is sized for concurrent processing

---

## Troubleshooting

### Issue: "File not found"
```
PDFExtractionException: File not found: /path/to/file.pdf
```
**Solution:** Verify file path is correct and file exists
```java
File file = new File(filePath);
if (!file.exists()) {
    throw new IllegalArgumentException("File does not exist: " + filePath);
}
```

### Issue: "Invalid PDF"
```
PDFExtractionException: Failed to load PDF document
```
**Solution:** Verify file is a valid PDF, not corrupted
```bash
file -b /path/to/file.pdf  # Check file type
```

### Issue: "No events extracted"
```
IngestionContext shows totalEventsProcessed == 0
```
**Causes:**
- PDF pages are blank/image-only (no text)
- Parser doesn't recognize text format
- Text extraction failed silently

**Debug:**
```java
String pageText = pdfExtractor.extractText(filePath, pageNum);
System.out.println("Extracted text length: " + pageText.length());
System.out.println("Content: " + pageText.substring(0, Math.min(200, pageText.length())));
```

### Issue: Database errors during ingestion
```
IngestionContext shows errors in pageErrors list
```
**Causes:**
- Constraint violations (duplicate keys, foreign keys)
- Schema mismatches
- Concurrent modifications

**Debug:** Check logs for SQL errors, verify database schema

---

## Testing

### Unit Testing PDFTextExtractor

```java
@Test
void testExtractFromValidPdf() throws PDFExtractionException {
    String text = extractor.extractText("test.pdf", 1);
    assertNotNull(text);
}

@Test
void testFileNotFound() {
    assertThrows(PDFExtractionException.class, 
        () -> extractor.extractText("nonexistent.pdf", 1));
}
```

### Integration Testing Runner

```java
@DataJpaTest
class IngestionRunnerTest {
    @Autowired
    private OahspeIngestionRunner runner;
    
    @Test
    void testCompleteWorkflow() throws PDFExtractionException {
        IngestionContext context = runner.ingestPdf("test.pdf");
        assertTrue(context.isSuccessful());
    }
}
```

---

## Related Documentation

- [OahspeParser Usage Guide](PARSER_USAGE_GUIDE.md) - Text parsing details
- [OahspeIngestionService Usage Guide](INGESTION_SERVICE_USAGE.md) - Entity persistence
- [Phase 3 Architecture](PHASE3_ARCHITECTURE.md) - Technical architecture details
- [Javadoc](/api/OahspeIngestionRunner.html) - API reference

