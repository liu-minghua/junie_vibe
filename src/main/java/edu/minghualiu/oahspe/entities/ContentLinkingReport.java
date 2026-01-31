package edu.minghualiu.oahspe.entities;

import lombok.*;

/**
 * Report generated after linking page content to ingested entities.
 * Provides statistics on linking success and identifies orphaned content.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentLinkingReport {
    private int totalPages;
    private int pagesLinked;
    private int imagesLinked;
    private int failedPages;
    private int orphanedImages;
    
    private int booksWithPageNumber;
    private int chaptersWithPageNumber;
    private int versesWithPageNumber;
    private int notesWithPageNumber;
    
    private int booksWithoutPageNumber;
    private int chaptersWithoutPageNumber;
    private int versesWithoutPageNumber;
    private int notesWithoutPageNumber;
    
    /**
     * Returns the overall success rate as a percentage.
     */
    public double getSuccessRate() {
        if (totalPages == 0) return 0.0;
        return (pagesLinked * 100.0) / totalPages;
    }
    
    /**
     * Returns true if all expected entities have pageNumber populated.
     */
    public boolean isFullyLinked() {
        return booksWithoutPageNumber == 0
                && chaptersWithoutPageNumber == 0
                && versesWithoutPageNumber == 0
                && notesWithoutPageNumber == 0;
    }
    
    /**
     * Returns a formatted summary string.
     */
    public String getSummary() {
        return String.format(
                """
                Content Linking Report:
                  Pages: %d total, %d linked (%.1f%%)
                  Images: %d linked, %d orphaned
                  Books: %d with page, %d without
                  Chapters: %d with page, %d without
                  Verses: %d with page, %d without
                  Notes: %d with page, %d without
                  Status: %s
                """,
                totalPages, pagesLinked, getSuccessRate(),
                imagesLinked, orphanedImages,
                booksWithPageNumber, booksWithoutPageNumber,
                chaptersWithPageNumber, chaptersWithoutPageNumber,
                versesWithPageNumber, versesWithoutPageNumber,
                notesWithPageNumber, notesWithoutPageNumber,
                isFullyLinked() ? "FULLY LINKED ✓" : "INCOMPLETE"
        );
    }
}
