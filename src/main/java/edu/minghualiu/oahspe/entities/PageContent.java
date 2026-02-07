package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import edu.minghualiu.oahspe.enums.PageCategory;
import edu.minghualiu.oahspe.enums.PageType;

import java.time.LocalDateTime;

/**
 * Stores raw text content extracted from each PDF page.
 * Also stores lightweight metadata used for page classification
 * (to determine whether geometry extraction is needed).
 */
@Entity
@Table(name = "page_contents",
    indexes = {
        @Index(name = "idx_page_number", columnList = "page_number", unique = true),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_ingested", columnList = "ingested"),
        @Index(name = "idx_needs_geometry", columnList = "needs_geometry"),
        @Index(name = "idx_is_book_content", columnList = "is_book_content")
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", length = 50, nullable = true)
    private PageType pageType;

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

    /* ---------------------------------------------------------
     * Lightweight classification metadata (Step 1)
     * --------------------------------------------------------- */

    @Column(name = "text_length")
    private Integer textLength;

    @Column(name = "line_count")
    private Integer lineCount;

    @Column(name = "verse_count")
    private Integer verseCount;

    @Column(name = "has_footnote_markers")
    private Boolean hasFootnoteMarkers;

    @Column(name = "has_illustration_keywords")
    private Boolean hasIllustrationKeywords;

    @Column(name = "has_saphah_keywords")
    private Boolean hasSaphahKeywords;

    /**
     * Cheap image detection (does NOT store geometry).
     * True if PDFBox reports images on this page.
     */
    @Column(name = "contains_images")
    private Boolean containsImages;

    /* ---------------------------------------------------------
     * Step 2 classification results
     * --------------------------------------------------------- */

    @Column(name = "needs_geometry")
    private Boolean needsGeometry;

    @Column(name = "is_book_content")
    private Boolean isBookContent;

    /* ---------------------------------------------------------
     * Utility methods
     * --------------------------------------------------------- */

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    public void markIngested() {
        this.ingested = true;
        this.ingestedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markError(String error) {
        this.errorMessage = error;
        this.ingested = false;
        this.ingestedAt = null;
    }

    public String getSummary() {
        return String.format(
            "Page %d [%s] - %s chars - %s",
            pageNumber,
            category,
            rawText != null ? rawText.length() : 0,
            ingested ? "INGESTED" : "PENDING"
        );
    }
}
