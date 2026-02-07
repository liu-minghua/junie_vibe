package edu.minghualiu.oahspe.entities;

import edu.minghualiu.oahspe.enums.PageCategory;
import lombok.*;

/**
 * Summary of a single page's content and ingestion status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageContentSummary {
    private Integer pageNumber;
    private PageCategory category;
    private boolean hasText;
    private int imageCount;
    private boolean ingested;
    private boolean hasError;
    private String errorMessage;
    
    /**
     * Returns a status indicator for display.
     */
    public String getStatusIndicator() {
        if (hasError) return "❌ ERROR";
        if (ingested) return "✓ INGESTED";
        return "⏳ PENDING";
    }
    
    /**
     * Returns a brief one-line summary.
     */
    public String getOneLine() {
        return String.format("Page %d [%s] - %s - %d images - %s",
                pageNumber,
                category,
                hasText ? "has text" : "no text",
                imageCount,
                getStatusIndicator());
    }
}
