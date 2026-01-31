package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores glossary terms extracted from the Glossaries section (pages 1669-1690).
 * Critical for maintaining translation consistency across the entire Oahspe.
 */
@Entity
@Table(name = "glossary_terms",
    indexes = {
        @Index(name = "idx_term", columnList = "term", unique = true),
        @Index(name = "idx_term_type", columnList = "term_type"),
        @Index(name = "idx_page_number", columnList = "page_number")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlossaryTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String term;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(name = "page_number")
    private Integer pageNumber;

    /**
     * Categorization: spiritual, person, place, concept, etc.
     */
    @Column(name = "term_type", length = 50)
    private String termType;

    /**
     * Usage count from INDEX if available (how many times this term is referenced).
     */
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    // Future translation fields (Phase 8+)
    // Commented out for now - will be activated in Phase 8
    /*
    @Column(name = "chinese_simplified", length = 255)
    private String chineseSimplified;

    @Column(name = "chinese_traditional", length = 255)
    private String chineseTraditional;

    @Column(length = 255)
    private String pinyin;
    */

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Increments the usage count (when found in INDEX).
     */
    public void incrementUsage() {
        this.usageCount = (this.usageCount != null ? this.usageCount : 0) + 1;
    }

    /**
     * Returns a brief summary for logging.
     */
    public String getSummary() {
        return String.format("GlossaryTerm[%s] - %s - page %d - used %dx",
                term,
                termType != null ? termType : "uncategorized",
                pageNumber,
                usageCount);
    }
}
