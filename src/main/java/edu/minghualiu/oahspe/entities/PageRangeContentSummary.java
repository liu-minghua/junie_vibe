package edu.minghualiu.oahspe.entities;

import lombok.*;

/**
 * Summary of content ingestion status for a range of pages (typically by category).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRangeContentSummary {
    private PageCategory category;
    private String pageRange;  // "7-1668"
    private int totalPages;
    private int ingestedPages;
    private int errorCount;
    private int totalImages;
    
    /**
     * Returns the ingestion progress as a percentage.
     */
    public double getProgress() {
        if (totalPages == 0) return 0.0;
        return (ingestedPages * 100.0) / totalPages;
    }
    
    /**
     * Returns true if all pages in this range have been ingested.
     */
    public boolean isComplete() {
        return ingestedPages == totalPages && errorCount == 0;
    }
    
    /**
     * Returns a status indicator for display.
     */
    public String getStatusIndicator() {
        if (errorCount > 0) return "⚠ ERRORS";
        if (isComplete()) return "✓ COMPLETE";
        if (ingestedPages > 0) return "⏳ IN PROGRESS";
        return "○ NOT STARTED";
    }
    
    /**
     * Returns a formatted summary string.
     */
    public String getSummary() {
        return String.format("%s (pages %s): %d/%d ingested (%.1f%%) - %d images - %s",
                category.getLabel(),
                pageRange,
                ingestedPages,
                totalPages,
                getProgress(),
                totalImages,
                getStatusIndicator());
    }
}
