package edu.minghualiu.oahspe.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores embedded images extracted from PDF pages.
 * Links to both the source PageContent and the final Image entity after ingestion.
 */
@Entity
@Table(name = "page_images",
    indexes = {
        @Index(name = "idx_page_content_id", columnList = "page_content_id"),
        @Index(name = "idx_linked_image_id", columnList = "linked_image_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_content_id", nullable = false)
    private PageContent pageContent;

    @Column(name = "image_sequence", nullable = false)
    private Integer imageSequence;  // 1, 2, 3... for multiple images on same page

    @Lob
    @Column(name = "image_data", columnDefinition = "BLOB", nullable = false)
    private byte[] imageData;

    @Column(name = "mime_type", length = 50)
    private String mimeType;  // "image/jpeg", "image/png", etc.

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_image_id")
    private Image linkedImage;  // Set after ingestion links PageImage to Image

    /**
     * Returns true if this PageImage has been linked to an Image entity.
     */
    public boolean isLinked() {
        return linkedImage != null;
    }

    /**
     * Returns the page number from the parent PageContent.
     */
    public Integer getPageNumber() {
        return pageContent != null ? pageContent.getPageNumber() : null;
    }

    /**
     * Returns a brief summary for logging.
     */
    public String getSummary() {
        return String.format("PageImage[page=%d, seq=%d, size=%d bytes, linked=%s]",
                getPageNumber(),
                imageSequence,
                imageData != null ? imageData.length : 0,
                isLinked());
    }
}
