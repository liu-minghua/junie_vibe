Below is a **complete, production‑ready, end‑to‑end ingestion test plan** for your Oahspe ingestion pipeline. It is written as a polished Markdown document you can drop directly into your project as:

**`Oahspe_Ingestion_Test_Plan.md`**

It covers unit tests, integration tests, system tests, data‑quality tests, and restart‑safety tests — everything needed to validate the entire ingestion workflow from PDF → Parser → Entities → Database.

---

# **Oahspe End‑to‑End Ingestion Test Plan**

This document defines the full testing strategy for validating the Oahspe ingestion pipeline, including:

- PDF extraction
- Parsing (state machine)
- Entity graph construction
- Image extraction
- Note–image linking
- Persistence
- Idempotency
- Restart safety
- Data integrity

The goal is to ensure the ingestion pipeline is **correct**, **deterministic**, **idempotent**, and **robust**.

---

# **1. Test Scope**

The ingestion pipeline consists of:

1. **PDF Extraction Layer**
    - Text extraction (PDFBox)
    - Image extraction (PDFBox)

2. **Parsing Layer**
    - OahspeParser (state machine)
    - Event model

3. **Mapping Layer**
    - OahspeIngestionService
    - ImageNoteLinker

4. **Persistence Layer**
    - JPA entity graph
    - Cascade rules
    - Unique constraints

5. **Orchestration Layer**
    - OahspeIngestionRunner

This test plan covers all layers.

---

# **2. Test Categories**

## **2.1 Unit Tests**
Validate individual components in isolation.

## **2.2 Integration Tests**
Validate interactions between components (parser → ingestion service → repositories).

## **2.3 System Tests**
Validate the entire ingestion pipeline using a real PDF sample.

## **2.4 Data Integrity Tests**
Validate correctness of stored data.

## **2.5 Idempotency & Restart Tests**
Validate that ingestion can be safely re‑run.

---

# **3. Unit Test Plan**

## **3.1 OahspeParser Tests**

### **Test Case P1 — Detect Book Titles**
**Input:**
```
Book of Apollo
```
**Expected:**  
`BookStart("Book of Apollo")`

---

### **Test Case P2 — Detect Chapter Titles**
**Input:**
```
Chapter 7
```
**Expected:**  
`ChapterStart("Chapter 7")`

---

### **Test Case P3 — Detect Verse Lines**
**Input:**
```
14/7.1 In the beginning...
```
**Expected:**  
`Verse("14/7.1", "In the beginning...")`

---

### **Test Case P4 — Detect Note Lines**
**Input:**
```
(1) This refers to...
```
**Expected:**  
`Note("1", "This refers to...")`

---

### **Test Case P5 — Detect Image References**
**Input:**
```
i002 Etherea Roadway
```
**Expected:**  
`ImageRef("IMG002", "Etherea Roadway")`

---

### **Test Case P6 — Continuation Lines**
**Input:**
```
14/7.1 Jehovih said...
and the Lords answered...
```
**Expected:**  
Two Verse events, second with `verseKey = null`.

---

## **3.2 ImageNoteLinker Tests**

### **Test Case L1 — Link Image to Note**
- Create Note
- Create Image
- Call `linkImageToNote`
- Assert:
    - Image saved
    - Note saved
    - Join table populated

---

### **Test Case L2 — Idempotent Linking**
Call `linkImageToNote` twice.

**Expected:**  
No duplicate join table entries.

---

## **3.3 Entity Tests**

### **Test Case E1 — Cascade Save**
- Build Book → Chapter → Verse → Note
- Save Book
- Assert all children persisted

---

### **Test Case E2 — Unique Constraints**
Attempt to insert duplicate:
- `verseKey`
- `noteKey`
- `imageKey`

**Expected:**  
Constraint violation.

---

# **4. Integration Test Plan**

## **4.1 Parser → IngestionService**

### **Test Case I1 — Parse and Ingest a Single Chapter**
Input lines:

```
Book of Apollo
Chapter 7
14/7.1 In the beginning...
(1) A footnote
i002 Etherea Roadway
```

**Expected DB State:**

| Entity | Count |
|--------|-------|
| Book | 1 |
| Chapter | 1 |
| Verse | 1 |
| Note | 1 |
| Image | 1 |
| note_images | 1 |

---

## **4.2 Image Extraction → Repository**

### **Test Case I2 — Extract Images from PDF Page**
Use a synthetic PDF with 1 embedded image.

**Expected:**
- Image saved with correct:
    - `imageKey`
    - `sourcePage`
    - `contentType`
    - `data`

---

## **4.3 Full Page Integration**

### **Test Case I3 — Parse + Ingest + Extract Images**
- Provide a PDF page with:
    - 1 chapter
    - 2 verses
    - 1 note
    - 1 image

**Expected:**  
All entities created and linked.

---

# **5. System Test Plan**

## **5.1 End‑to‑End Ingestion of a Mini‑PDF**

Use a small synthetic PDF containing:

```
Book of Osiris
Chapter 1
18/1.1 Osiris said...
(1) A note
i003 A plate
```

**Expected:**
- Full entity graph persisted
- Image extracted
- Note linked to image

---

## **5.2 Ingest a Multi‑Page PDF**

Use a 5‑page sample with:

- 2 books
- 5 chapters
- 20 verses
- 6 notes
- 3 images

Validate:

- Correct counts
- Correct ordering
- Correct linking
- No duplicates

---

# **6. Data Integrity Tests**

## **6.1 Verse Ordering**
Ensure verses are stored in the order they appear.

## **6.2 Chapter Ordering**
Ensure chapters are stored in the order they appear.

## **6.3 Note Attachment**
Ensure notes attach to the correct verse.

## **6.4 Image Metadata**
Validate:

- `sourcePage`
- `originalFilename`
- `contentType`

---

# **7. Idempotency & Restart Tests**

## **7.1 Re‑Run Ingestion on Same PDF**
Run ingestion twice.

**Expected:**
- No duplicate verses
- No duplicate notes
- No duplicate images
- No duplicate join table entries

---

## **7.2 Partial Ingestion Restart**
Simulate crash after 50% of pages.

Re‑run ingestion.

**Expected:**
- Missing entities inserted
- Existing entities untouched
- No duplicates

---

## **7.3 Corrupted Page Handling**
Inject a malformed page.

**Expected:**
- Error logged
- Ingestion continues
- No crash

---

# **8. Performance Tests**

## **8.1 Large PDF Throughput**
Run ingestion on the full 48 MB Oahspe PDF.

Measure:

- Total time
- Memory usage
- GC pressure

---

## **8.2 Image Extraction Performance**
Measure extraction time for:

- 10 images
- 100 images
- 500 images

---

# **9. Acceptance Criteria**

The ingestion pipeline is considered production‑ready when:

- All unit tests pass
- All integration tests pass
- All system tests pass
- Idempotency tests pass
- Restart tests pass
- Performance tests meet thresholds
- No data integrity issues remain

---

If you want, I can now generate **JUnit test class templates** for each section, so you can drop them directly into your test suite.