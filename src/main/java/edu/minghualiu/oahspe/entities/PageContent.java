package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores raw text content extracted from each PDF page.
 * Acts as the source of truth for page-by-page ingestion workflow.
 */
@Entity
@Table(name = "page_contents",
    indexes = {
        @Index(name = "idx_page_number", columnList = "page_number", unique = true),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_ingested", columnList = "ingested")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_number", nullable = false, unique = true)
    private Integer pageNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PageCategory category;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawText;

    @CreationTimestamp
    @Column(name = "extracted_at", nullable = false)
    private LocalDateTime extractedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ingested = false;

    @Column(name = "ingested_at")
    private LocalDateTime ingestedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Returns true if this page encountered an error during extraction or ingestion.
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    /**
     * Marks this page as successfully ingested.
     */
    public void markIngested() {
        this.ingested = true;
        this.ingestedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * Marks this page as failed with an error message.
     */
    public void markError(String error) {
        this.errorMessage = error;
        this.ingested = false;
        this.ingestedAt = null;
    }

    /**
     * Returns a brief summary of this page for logging.
     */
    public String getSummary() {
        return String.format("Page %d [%s] - %s chars - %s", 
                pageNumber,
                category,
                rawText != null ? rawText.length() : 0,
                ingested ? "INGESTED" : "PENDING");
    }
}
