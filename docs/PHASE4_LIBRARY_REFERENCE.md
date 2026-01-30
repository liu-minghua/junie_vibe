# Phase 4 Library Reference: External Dependencies

**Date:** 2026-01-30  
**Version:** 1.0  
**Purpose:** Quick reference for external libraries used in OahspeDataValidator

---

## Overview

Phase 4 uses **no new external dependencies** beyond what already exists in the Oahspe project. All validation logic is implemented using:
- Java standard library (java.util, java.time)
- Spring Framework (already in project)
- Spring Data JPA (already in project)

This minimizes risk and compatibility issues while keeping OahspeDataValidator lightweight and maintainable.

---

## Existing Dependencies Used

### Java Standard Library

#### `java.util.List<T>`
**Purpose:** Store collections of entities and validation issues  
**Usage Pattern:**
```java
List<Verse> verses = verseRepository.findAll();  // Load all verses
List<ValidationIssue> issues = new ArrayList<>();  // Collect issues
```

**Common Gotchas:**
- `List` is mutable; don't modify returned collections (or make copies)
- `findAll()` returns mutable list; safe to add/remove from copy

---

#### `java.util.Map<K, V>`
**Purpose:** Organize issues by severity or entity type  
**Usage Pattern:**
```java
Map<Severity, List<ValidationIssue>> issuesBySeverity = new HashMap<>();
issuesBySeverity.put(Severity.ERROR, errorList);
issuesBySeverity.get(Severity.ERROR);  // Retrieve errors
```

**Common Gotchas:**
- `.get()` returns null if key doesn't exist (check before using)
- Use `.getOrDefault()` to provide fallback value

---

#### `java.util.stream.Stream<T>`
**Purpose:** Filter and transform collections (functional style)  
**Usage Pattern:**
```java
result.getIssuesBySeverity(Severity.ERROR)
    .stream()
    .filter(issue -> issue.getEntityType().equals("VERSE"))
    .forEach(issue -> System.out.println(issue.getMessage()));
```

**Common Gotchas:**
- Streams are lazy; must call terminal operation (.forEach, .collect, etc.)
- Streams are single-use; can't iterate same stream twice

---

#### `java.util.Comparator<T>`
**Purpose:** Sort collections (e.g., issues by severity)  
**Usage Pattern:**
```java
List<ValidationIssue> sorted = issues.stream()
    .sorted((a, b) -> a.getSeverity().compareTo(b.getSeverity()))
    .collect(Collectors.toList());
```

**Common Gotchas:**
- Use `compareTo()` for enum/comparable types
- Return negative (a < b), zero (a == b), positive (a > b)

---

#### `java.time.LocalDateTime`
**Purpose:** Track validation start/end times  
**Usage Pattern:**
```java
LocalDateTime startTime = LocalDateTime.now();
// ... do validation
long elapsedMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
```

**Common Gotchas:**
- `now()` is timezone-aware; use same for start and end
- Can't subtract LocalDateTime directly; use Duration

---

### Spring Framework

#### `@Component` Annotation
**Purpose:** Register OahspeDataValidator as Spring bean  
**Usage:**
```java
@Component
public class OahspeDataValidator {
    // Spring auto-instantiates and injects dependencies
}
```

**Common Gotchas:**
- Requires Spring context to be running (@SpringBootTest for tests)
- Default scope is singleton (same instance across app)

---

#### `@RequiredArgsConstructor` (Lombok)
**Purpose:** Auto-generate constructor for dependency injection  
**Usage:**
```java
@Component
@RequiredArgsConstructor
public class OahspeDataValidator {
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    // Lombok generates constructor automatically
}
```

**Common Gotchas:**
- Must mark fields as `final` for Lombok to include in constructor
- Lombok must be in pom.xml (already is in Oahspe project)

---

#### `@Transactional` Annotation
**Purpose:** Mark methods that read from database (optional but good practice)  
**Usage:**
```java
@Transactional(readOnly = true)
public ValidationResult validateAll(ValidationProgressCallback callback) {
    // Tells Spring: this is read-only, optimize database access
}
```

**Common Gotchas:**
- Without @Transactional, each repository call opens/closes connection
- With @Transactional, single transaction for entire method (more efficient)
- Add `readOnly = true` to hint that data won't be modified

---

### Spring Data JPA

#### `JpaRepository<Entity, ID>` Interface
**Purpose:** Database access for entities  
**Repositories Already Exist:**
- `BookRepository extends JpaRepository<Book, Long>`
- `ChapterRepository extends JpaRepository<Chapter, Long>`
- `VerseRepository extends JpaRepository<Verse, Long>`
- `NoteRepository extends JpaRepository<Note, Long>`
- `ImageRepository extends JpaRepository<Image, Long>`

**Common Methods Used:**
```java
List<Book> books = bookRepository.findAll();  // Get all books
Optional<Book> book = bookRepository.findById(1L);  // Get by ID
boolean exists = bookRepository.existsById(1L);  // Check existence
long count = bookRepository.count();  // Total count
```

**Common Gotchas:**
- `findById()` returns Optional (must check `.isPresent()` before `.get()`)
- `findAll()` loads entire table into memory (OK for Oahspe size)
- `existsById()` is more efficient than `findById().isPresent()`

---

#### Optional<T> Handling
**Purpose:** Null-safe container for values that might not exist  
**Usage Pattern:**
```java
Optional<Book> book = bookRepository.findById(bookId);
if (book.isPresent()) {
    Book b = book.get();
    // Use book
} else {
    // Book doesn't exist - handle error
}

// Or using functional style:
book.ifPresent(b -> System.out.println(b.getTitle()));
```

**Common Gotchas:**
- Never call `.get()` without checking `.isPresent()` (throws NoSuchElementException)
- `.orElse(null)` is valid but check for null before using
- `.orElseThrow()` to throw exception if missing

---

## No New Dependencies Needed

### Why Not?

**Validation Logic:** Pure Java (no library needed)
```java
if (verse.getVerseNumber() < 0) {
    issues.add(new ValidationIssue(...));
}
```

**Data Structures:** Java Collections (already available)
```java
List<ValidationIssue> issues = new ArrayList<>();
Map<Severity, List<ValidationIssue>> organized = new HashMap<>();
```

**Spring Integration:** Already configured for OahspeApplication
```java
@Component  // Spring manages lifecycle
@RequiredArgsConstructor  // Lombok handles constructor
```

**Entity Access:** Spring Data repositories (already defined)
```java
private final BookRepository bookRepository;  // Spring injects
List<Book> all = bookRepository.findAll();
```

### Added Value of No New Dependencies

✅ **Reduced Risk:** No new library versions to manage  
✅ **Faster Builds:** No new downloads/compilation  
✅ **Clear Dependencies:** Easy to understand what OahspeDataValidator uses  
✅ **Easy Testing:** No mocking complex external libraries  
✅ **Lightweight:** Validator code ~600 lines for full validation suite  

---

## Code Patterns by Library

### Pattern 1: Load and Iterate (Spring Data)

```java
List<Verse> verses = verseRepository.findAll();
for (Verse verse : verses) {
    validateVerse(verse);  // Check individual verse
}
```

**Performance:** Loads all verses into memory (acceptable for Oahspe)  
**Alternative (for future):** Use `.stream()` for lazy evaluation

---

### Pattern 2: Check Existence (Spring Data)

```java
// ✅ Good - efficient query
if (!verseRepository.existsById(verse.getChapterId())) {
    issues.add(new ValidationIssue(...));
}

// ❌ Avoid - loads entire entity
if (verseRepository.findById(verse.getChapterId()).isPresent()) {
    // ...
}
```

---

### Pattern 3: Organize Data (Java Collections)

```java
Map<Severity, List<ValidationIssue>> organized = new HashMap<>();

for (ValidationIssue issue : allIssues) {
    organized.computeIfAbsent(issue.getSeverity(), k -> new ArrayList<>())
        .add(issue);
}

// Access organized by severity
List<ValidationIssue> errors = organized.get(Severity.ERROR);
```

---

### Pattern 4: Filter Collections (Java Streams)

```java
List<ValidationIssue> criticalIssues = result.getAllIssues()
    .stream()
    .filter(issue -> issue.getSeverity() == Severity.CRITICAL)
    .collect(Collectors.toList());
```

---

## Troubleshooting Common Issues

### Issue 1: NullPointerException from findById()

**Problem:**
```java
Verse verse = verseRepository.findById(id).get();  // Crashes if not found
```

**Solution:**
```java
Optional<Verse> optional = verseRepository.findById(id);
if (optional.isPresent()) {
    Verse verse = optional.get();
    // Use verse
} else {
    issues.add(new ValidationIssue(..., "Verse not found"));
}
```

---

### Issue 2: Out of Memory with findAll()

**Problem:** Loading 1M+ records into List causes memory pressure

**Current State:** Not an issue for Oahspe (typical <100k records)

**Future Solution (Phase 5):** Use pagination or streams
```java
@Query(value = "SELECT * FROM verse", nativeQuery = true)
Stream<Verse> streamAllVerses();
```

---

### Issue 3: Duplicate Issues in Report

**Problem:** Same issue added multiple times

**Solution:** Use Set or check before adding
```java
if (!issues.contains(issue)) {
    issues.add(issue);
}
```

---

### Issue 4: N+1 Query Problem

**Problem:** Loop that queries database in each iteration
```java
for (Chapter chapter : chapters) {
    // ❌ This queries verses table N times
    List<Verse> verses = chapter.getVerses();
}
```

**Solution (Phase 5):** Load related entities eagerly or use join queries
```java
@Query("SELECT DISTINCT c FROM Chapter c LEFT JOIN FETCH c.verses")
List<Chapter> findAllWithVerses();
```

---

## Summary Table

| Library | Used For | Risk Level | Optimization Potential |
|---------|----------|-----------|----------------------|
| Java Collections | Store/organize issues | Low | Medium (streaming large datasets) |
| Java Streams | Filter/transform data | Low | High (lazy evaluation) |
| Spring Data JPA | Database access | Low | High (caching, pagination) |
| Spring Framework | Dependency injection | Low | Low |
| Lombok @RequiredArgsConstructor | Constructor generation | Low | None |

---

## Testing Considerations

### Unit Tests (No Spring Context Needed)

```java
@Test
void testValidateBook() {
    EntityValidator validator = new EntityValidator();  // Direct instantiation
    Book book = new Book();
    book.setTitle("Test");
    
    List<ValidationIssue> issues = validator.validateBook(book);
    // No Spring, no database
}
```

---

### Integration Tests (Spring Context Required)

```java
@DataJpaTest
class OahspeDataValidatorIT {
    
    @Autowired
    private OahspeDataValidator validator;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Test
    void testValidateAll() throws ValidationException {
        // Spring loads repositories, injects them
        ValidationResult result = validator.validateAll(null);
        assertNotNull(result);
    }
}
```

---

## Conclusion

Phase 4 intentionally uses **no new external dependencies**, relying instead on:
- ✅ Java standard library
- ✅ Spring Framework (already required)
- ✅ Spring Data JPA (already required)
- ✅ Lombok (already required)

**Benefits:**
- Minimal risk of library conflicts
- Fast builds and tests
- Easy to understand and maintain
- Clear separation of concerns

---

*Document Status: COMPLETE*  
*Next Step: Begin Code Implementation (Task 4.2.1)*
