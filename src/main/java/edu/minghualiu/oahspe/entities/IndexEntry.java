package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores index entries extracted from the Index section (pages 1691-1831).
 * Provides cross-reference validation and QA support for translation.
 */
@Entity
@Table(name = "index_entries",
    indexes = {
        @Index(name = "idx_topic", columnList = "topic"),
        @Index(name = "idx_glossary_term", columnList = "glossary_term_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String topic;

    /**
     * Page references as extracted from index (e.g., "42, 108, 234-240").
     */
    @Lob
    @Column(name = "page_references", columnDefinition = "TEXT")
    private String pageReferences;

    /**
     * Link to glossary if this topic exists in the glossary.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "glossary_term_id")
    private GlossaryTerm glossaryTerm;

    /**
     * The page number where this index entry was extracted from.
     */
    @Column(name = "extracted_from_page")
    private Integer extractedFromPage;

    // Future translation field (Phase 8+)
    /*
    @Column(name = "chinese_topic", length = 500)
    private String chineseTopic;
    */

    /**
     * Returns true if this index entry is linked to a glossary term.
     */
    public boolean hasGlossaryLink() {
        return glossaryTerm != null;
    }

    /**
     * Returns a count of how many page references this entry has.
     * Estimates by counting commas (rough approximation).
     */
    public int getEstimatedReferenceCount() {
        if (pageReferences == null || pageReferences.isEmpty()) {
            return 0;
        }
        return pageReferences.split(",").length;
    }

    /**
     * Returns a brief summary for logging.
     */
    public String getSummary() {
        return String.format("IndexEntry[%s] - %d refs - %s",
                topic,
                getEstimatedReferenceCount(),
                hasGlossaryLink() ? "linked to glossary" : "standalone");
    }
}
