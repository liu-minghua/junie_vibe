package edu.minghualiu.oahspe.ingestion.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationIssue Tests")
class ValidationIssueTest {

    @Test
    @DisplayName("should create ValidationIssue with valid parameters")
    void testConstructor() {
        ValidationIssue issue = new ValidationIssue(
            Severity.ERROR,
            "BOOK",
            1L,
            "TestRule",
            "Test message",
            "Test suggestion"
        );
        
        assertNotNull(issue);
        assertEquals(Severity.ERROR, issue.getSeverity());
        assertEquals("BOOK", issue.getEntityType());
        assertEquals(1L, issue.getEntityId());
        assertEquals("TestRule", issue.getRule());
        assertEquals("Test message", issue.getMessage());
        assertEquals("Test suggestion", issue.getSuggestedFix());
    }

    @Test
    @DisplayName("should have valid severity levels")
    void testSeverityLevels() {
        assertNotNull(Severity.INFO);
        assertNotNull(Severity.WARNING);
        assertNotNull(Severity.ERROR);
        assertNotNull(Severity.CRITICAL);
    }

    @Test
    @DisplayName("should generate meaningful toString")
    void testToString() {
        ValidationIssue issue = new ValidationIssue(
            Severity.WARNING,
            "CHAPTER",
            2L,
            "Completeness",
            "Missing verses",
            "Add verses"
        );
        
        String toString = issue.toString();
        assertNotNull(toString);
        assertFalse(toString.isEmpty());
    }

    @Test
    @DisplayName("should return suggested fix")
    void testSuggestedFix() {
        ValidationIssue issue = new ValidationIssue(
            Severity.ERROR,
            "VERSE",
            1L,
            "TextRequired",
            "Verse text is empty",
            "Add verse text content"
        );
        
        assertEquals("Add verse text content", issue.getSuggestedFix());
    }

    @Test
    @DisplayName("should distinguish between severity levels")
    void testSeverityComparison() {
        ValidationIssue info = new ValidationIssue(Severity.INFO, "E", 1L, "R", "M", "S");
        ValidationIssue warning = new ValidationIssue(Severity.WARNING, "E", 1L, "R", "M", "S");
        ValidationIssue error = new ValidationIssue(Severity.ERROR, "E", 1L, "R", "M", "S");
        ValidationIssue critical = new ValidationIssue(Severity.CRITICAL, "E", 1L, "R", "M", "S");
        
        assertNotEquals(info.getSeverity(), warning.getSeverity());
        assertNotEquals(warning.getSeverity(), error.getSeverity());
        assertNotEquals(error.getSeverity(), critical.getSeverity());
    }
}
