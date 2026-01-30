Absolutely, Minghua — here is the polished, production‑ready **Markdown documentation** for the **Oahspe Ingestion Runner**, the final orchestration layer that ties together:

- PDF extraction
- OahspeParser
- OahspeIngestionService
- Image extraction
- Logging and progress tracking

You can save this as:

**`Oahspe_Ingestion_Runner.md`**

---

# **Oahspe Ingestion Runner Documentation**

The **OahspeIngestionRunner** is the top‑level orchestrator responsible for processing the entire Oahspe PDF from start to finish. It coordinates PDF extraction, parsing, ingestion, and persistence.

This component is typically implemented as a Spring Boot `CommandLineRunner` or scheduled job.

---

## **1. Purpose of the Ingestion Runner**

The ingestion runner:

1. Loads the Oahspe PDF file
2. Extracts text page‑by‑page
3. Extracts images page‑by‑page
4. Passes text to the `OahspeParser`
5. Sends parser events to `OahspeIngestionService`
6. Logs progress and errors
7. Supports restart‑safe ingestion

This is the “main engine” that drives the entire ingestion pipeline.

---

## **2. High‑Level Workflow**

```
PDF → PDFBox Extractor
        → Text Lines
        → Images
        → OahspeParser
                → Events
                → OahspeIngestionService
                        → Entities
                        → Database
```

The runner is responsible for the outer loop.

---

## **3. Runner Responsibilities**

### **3.1 PDF Loading**
- Open the PDF using PDFBox (`PDDocument`)
- Iterate through pages

### **3.2 Text Extraction**
- Use `PDFTextStripper`
- Split into lines
- Pass to parser

### **3.3 Image Extraction**
- Use `PDResources` and `PDImageXObject`
- Extract:
    - Binary data
    - Content type
    - Original filename
    - Page number

### **3.4 Event Dispatch**
- Feed parser events to `OahspeIngestionService`

### **3.5 Logging**
- Track page numbers
- Track book/chapter transitions
- Track image extraction

### **3.6 Restart Safety**
- Use unique keys (`verseKey`, `noteKey`, `imageKey`) to avoid duplicates

---

## **4. OahspeIngestionRunner Class**

Below is the conceptual structure of the runner.

```java
@Service
@RequiredArgsConstructor
public class OahspeIngestionRunner implements CommandLineRunner {

    private final OahspeParser parser;
    private final OahspeIngestionService ingestionService;
    private final ImageRepository imageRepository;

    @Value("${oahspe.pdf.path}")
    private String pdfPath;

    @Override
    public void run(String... args) throws Exception {
        processPdf(pdfPath);
    }

    public void processPdf(String path) throws Exception {

        try (PDDocument document = PDDocument.load(new File(path))) {

            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();

            for (int page = 1; page <= pageCount; page++) {

                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text = stripper.getText(document);
                List<String> lines = text.lines().toList();

                // Parse text into events
                List<OahspeEvent> events = parser.parse(lines, page);

                // Process events
                ingestionService.ingestEvents(events);

                // Extract images
                extractImagesFromPage(document, page);

                System.out.println("Processed page " + page + " / " + pageCount);
            }
        }
    }
}
```

---

## **5. Image Extraction Logic**

```java
private void extractImagesFromPage(PDDocument document, int pageNumber) throws IOException {

    PDPage page = document.getPage(pageNumber - 1);
    PDResources resources = page.getResources();

    for (COSName name : resources.getXObjectNames()) {

        PDXObject xobject = resources.getXObject(name);

        if (xobject instanceof PDImageXObject img) {

            byte[] bytes = img.getImageData();
            String format = img.getSuffix();

            String imageKey = generateImageKey(pageNumber, name.getName());

            Image image = Image.builder()
                    .imageKey(imageKey)
                    .title("Image " + imageKey)
                    .description("Extracted from page " + pageNumber)
                    .sourcePage(pageNumber)
                    .originalFilename(name.getName() + "." + format)
                    .contentType("image/" + format)
                    .data(bytes)
                    .build();

            // Idempotent save
            imageRepository.findByImageKey(imageKey)
                    .orElseGet(() -> imageRepository.save(image));
        }
    }
}
```

---

## **6. Image Key Generation**

A simple deterministic scheme:

```java
private String generateImageKey(int page, String objectName) {
    return "IMG" + page + "_" + objectName;
}
```

This ensures:

- No collisions
- Restart‑safe ingestion
- Traceability

---

## **7. Logging and Monitoring**

The runner logs:

- Page number
- Book transitions
- Chapter transitions
- Number of verses parsed
- Number of notes parsed
- Number of images extracted

This helps diagnose parsing issues.

---

## **8. Error Handling Strategy**

The runner should:

- Catch malformed lines
- Log unexpected patterns
- Continue ingestion even if a page has issues
- Never stop the entire ingestion unless the PDF is unreadable

This ensures robustness.

---

## **9. Configuration**

Add to `application.properties`:

```
oahspe.pdf.path=/path/to/oahspe.pdf
```

This allows flexible deployment.

---

## **10. Benefits of This Runner Design**

- **Modular**: Parser, ingestion, and extraction are separate.
- **Restart‑safe**: Unique keys prevent duplicates.
- **Deterministic**: Same PDF → same database.
- **Traceable**: Every image and verse is tied to a page number.
- **Extensible**: Can add translation, indexing, or search layers later.

---

If you want, I can now generate a **complete end‑to‑end ingestion test plan**, or produce a **diagram** of the entire pipeline for your documentation.