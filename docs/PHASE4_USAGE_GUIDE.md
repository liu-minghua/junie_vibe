# Phase 4 Usage Guide: OahspeDataValidator

## Overview

The OahspeDataValidator provides a comprehensive framework for validating ingested Oahspe data. It validates individual entities, cross-entity relationships, and business rule compliance, producing detailed reports with categorized issues.

**Key Components:**
- `Severity` enum: categorizes issues (INFO, WARNING, ERROR, CRITICAL)
- `ValidationIssue`: represents a single validation problem
- `EntityValidator`: validates individual entity properties
- `CrossEntityValidator`: validates relationships between entities

---

## Basic Usage

### Validating a Single Entity

```java
import edu.minghualiu.oahspe.entities.Book;
import edu.minghualiu.oahspe.ingestion.validator.EntityValidator;
import edu.minghualiu.oahspe.ingestion.validator.ValidationIssue;
import java.util.List;

// Create validator
EntityValidator validator = new EntityValidator();

// Create entity to validate
Book book = new Book();
book.setId(1);
book.setTitle("The Oahspe");
book.setDescription("Sacred text of the Oahspe");

// Validate the book
List<ValidationIssue> issues = validator.validateBook(book);

// Check results
if (issues.isEmpty()) {
    System.out.println("Book is valid!");
} else {
    for (ValidationIssue issue : issues) {
        System.out.println("Issue: " + issue.getMessage());
        System.out.println("Severity: " + issue.getSeverity());
        System.out.println("Suggestion: " + issue.getSuggestedFix());
    }
}
```

### Validating Cross-Entity Relationships

```java
import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.ingestion.validator.CrossEntityValidator;
import edu.minghualiu.oahspe.ingestion.validator.Severity;
import java.util.ArrayList;
import java.util.List;

// Create validator
CrossEntityValidator validator = new CrossEntityValidator();

// Create entities
List<Book> books = new ArrayList<>();
List<Chapter> chapters = new ArrayList<>();
List<Verse> verses = new ArrayList<>();
List<Note> notes = new ArrayList<>();
List<Image> images = new ArrayList<>();

Book book = new Book();
book.setId(1);
book.setTitle("The Oahspe");
books.add(book);

Chapter chapter = new Chapter();
chapter.setId(1L);
chapter.setTitle("First Chapter");
chapter.setBook(book);
chapters.add(chapter);

// Add more verses, notes, images...

// Validate all relationships
List<ValidationIssue> issues = validator.validateAll(books, chapters, verses, notes, images);

// Report results
long criticalCount = issues.stream()
    .filter(i -> i.getSeverity() == Severity.CRITICAL)
    .count();
    
System.out.println("Total issues: " + issues.size());
System.out.println("Critical issues: " + criticalCount);
```

---

## Advanced Usage

### Filtering Issues by Severity

```java
import edu.minghualiu.oahspe.ingestion.validator.Severity;

// Get only critical issues
List<ValidationIssue> criticalIssues = issues.stream()
    .filter(i -> i.getSeverity() == Severity.CRITICAL)
    .collect(Collectors.toList());

// Get warnings and errors
List<ValidationIssue> significantIssues = issues.stream()
    .filter(i -> i.getSeverity() == Severity.WARNING || i.getSeverity() == Severity.ERROR)
    .collect(Collectors.toList());
```

### Filtering Issues by Entity Type

```java
// Get all issues related to verses
List<ValidationIssue> verseIssues = issues.stream()
    .filter(i -> i.getEntityType().equals("VERSE"))
    .collect(Collectors.toList());

// Get all issues for a specific entity
long entityId = 42L;
List<ValidationIssue> entityIssues = issues.stream()
    .filter(i -> i.getEntityId() == entityId)
    .collect(Collectors.toList());
```

### Validating All Entity Types

```java
import edu.minghualiu.oahspe.entities.*;

// Create validator
EntityValidator validator = new EntityValidator();

// Validate different entity types
List<ValidationIssue> bookIssues = validator.validateBook(myBook);
List<ValidationIssue> chapterIssues = validator.validateChapter(myChapter);
List<ValidationIssue> verseIssues = validator.validateVerse(myVerse);
List<ValidationIssue> noteIssues = validator.validateNote(myNote);
List<ValidationIssue> imageIssues = validator.validateImage(myImage);

// Aggregate all issues
List<ValidationIssue> allIssues = new ArrayList<>();
allIssues.addAll(bookIssues);
allIssues.addAll(chapterIssues);
allIssues.addAll(verseIssues);
allIssues.addAll(noteIssues);
allIssues.addAll(imageIssues);
```

---

## Integration with Phase 3

### Using with OahspeIngestionRunner

```java
import edu.minghualiu.oahspe.ingestion.runner.OahspeIngestionRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataValidationService {
    
    @Autowired
    private OahspeIngestionRunner ingestionRunner;
    
    @Autowired
    private EntityValidator entityValidator;
    
    @Autowired
    private CrossEntityValidator crossValidator;
    
    public void validateIngestedData(String pdfPath) {
        // Run ingestion (produces entities in database)
        ingestionRunner.runIngestion(pdfPath);
        
        // Retrieve all ingested entities from repositories
        List<Book> books = bookRepository.findAll();
        List<Chapter> chapters = chapterRepository.findAll();
        List<Verse> verses = verseRepository.findAll();
        List<Note> notes = noteRepository.findAll();
        List<Image> images = imageRepository.findAll();
        
        // Validate cross-entity relationships
        List<ValidationIssue> issues = crossValidator.validateAll(
            books, chapters, verses, notes, images
        );
        
        // Report results
        generateValidationReport(issues);
    }
}
```

---

## Example: Complete Validation Workflow

```java
import edu.minghualiu.oahspe.entities.*;
import edu.minghualiu.oahspe.ingestion.validator.*;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationWorkflow {
    
    public static void main(String[] args) {
        // Create validators
        EntityValidator entityValidator = new EntityValidator();
        CrossEntityValidator crossValidator = new CrossEntityValidator();
        
        // Create test data
        Book book = createTestBook();
        Chapter chapter = createTestChapter(book);
        Verse verse = createTestVerse(chapter);
        
        // Validate individual entities
        List<ValidationIssue> entityIssues = new ArrayList<>();
        entityIssues.addAll(entityValidator.validateBook(book));
        entityIssues.addAll(entityValidator.validateChapter(chapter));
        entityIssues.addAll(entityValidator.validateVerse(verse));
        
        // Log entity-level issues
        System.out.println("=== Entity Validation Results ===");
        entityIssues.forEach(issue -> {
            System.out.println(String.format(
                "[%s] %s - %s",
                issue.getSeverity(),
                issue.getEntityType(),
                issue.getMessage()
            ));
        });
        
        // Validate cross-entity relationships
        List<ValidationIssue> crossIssues = crossValidator.validateAll(
            List.of(book),
            List.of(chapter),
            List.of(verse),
            new ArrayList<>(),
            new ArrayList<>()
        );
        
        // Log cross-entity issues
        System.out.println("\n=== Cross-Entity Validation Results ===");
        crossIssues.forEach(issue -> {
            System.out.println(String.format(
                "[%s] %s (#%d) - %s",
                issue.getSeverity(),
                issue.getEntityType(),
                issue.getEntityId(),
                issue.getMessage()
            ));
        });
        
        // Summary statistics
        long totalIssues = entityIssues.size() + crossIssues.size();
        long criticalIssues = getAllIssues(entityIssues, crossIssues).stream()
            .filter(i -> i.getSeverity() == Severity.CRITICAL)
            .count();
        
        System.out.println(String.format("\n=== Summary ==="));
        System.out.println("Total issues: " + totalIssues);
        System.out.println("Critical issues: " + criticalIssues);
        System.out.println("Validation " + (criticalIssues == 0 ? "PASSED" : "FAILED"));
    }
    
    private static List<ValidationIssue> getAllIssues(
            List<ValidationIssue> entityIssues,
            List<ValidationIssue> crossIssues) {
        List<ValidationIssue> all = new ArrayList<>(entityIssues);
        all.addAll(crossIssues);
        return all;
    }
    
    private static Book createTestBook() {
        Book book = new Book();
        book.setId(1);
        book.setTitle("Test Book");
        return book;
    }
    
    private static Chapter createTestChapter(Book book) {
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setTitle("Test Chapter");
        chapter.setBook(book);
        return chapter;
    }
    
    private static Verse createTestVerse(Chapter chapter) {
        Verse verse = new Verse();
        verse.setId(1);
        verse.setVerseKey("1:1");
        verse.setText("Test verse");
        verse.setChapter(chapter);
        return verse;
    }
}
```

---

## Severity Levels

| Severity | Description | Action Required |
|----------|-------------|-----------------|
| **INFO** | Informational message | No action needed |
| **WARNING** | Issue that might affect data quality | Review and consider fixing |
| **ERROR** | Issue that prevents proper functioning | Must be fixed |
| **CRITICAL** | Issue that breaks system integrity | Must be fixed immediately |

---

## Performance Considerations

1. **Entity Validation**: O(1) per entity - very fast
2. **Cross-Entity Validation**: O(n) where n = total number of entities
3. **Large Datasets**: For datasets with >100,000 entities, consider:
   - Validating in batches
   - Running validation asynchronously
   - Caching validation results

---

## Common Issues and Solutions

### Issue: No Issues Detected, But Data Looks Wrong

- Validators check structural integrity, not semantic correctness
- Add custom validation rules for business logic
- Use Spring validation annotations for additional checks

### Issue: Too Many Warnings

- Filter by severity to focus on critical issues first
- Some warnings may be acceptable for your use case
- Adjust validation rules as needed

### Issue: Performance Degradation with Large Datasets

- Consider pagination or batch processing
- Cache validation results where appropriate
- Run validation asynchronously with progress callbacks

---

## API Reference Summary

### ValidationIssue
```java
ValidationIssue(
    Severity severity,      // CRITICAL, ERROR, WARNING, or INFO
    String entityType,      // BOOK, CHAPTER, VERSE, NOTE, or IMAGE
    Long entityId,          // ID of affected entity
    String rule,            // Name of violated rule
    String message,         // Human-readable description
    String suggestedFix     // How to fix the issue
)

// Getters
getSeverity()      // Returns Severity enum
getEntityType()    // Returns String
getEntityId()      // Returns Long
getRule()          // Returns String
getMessage()       // Returns String
getSuggestedFix()  // Returns String
```

### EntityValidator
```java
List<ValidationIssue> validateBook(Book book)
List<ValidationIssue> validateChapter(Chapter chapter)
List<ValidationIssue> validateVerse(Verse verse)
List<ValidationIssue> validateNote(Note note)
List<ValidationIssue> validateImage(Image image)
```

### CrossEntityValidator
```java
List<ValidationIssue> validateAll(
    List<Book> books,
    List<Chapter> chapters,
    List<Verse> verses,
    List<Note> notes,
    List<Image> images
)
```

---

For more information, see [PHASE4_API_REFERENCE.md](PHASE4_API_REFERENCE.md) and [PHASE4_DESIGN.md](PHASE4_DESIGN.md).
