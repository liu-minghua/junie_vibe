package edu.minghualiu.oahspe.entities;

/**
 * Categorizes pages in the Oahspe PDF by their content type and purpose.
 * Each category has defined page ranges and ingestion rules.
 */
public enum PageCategory {
    COVER("Cover Pages", 1, 3, false, false),
    TABLE_OF_CONTENTS("Table of Contents", 4, 4, false, false),
    IMAGE_LIST("Image List", 5, 6, false, false),
    OAHSPE_BOOKS("Oahspe Books - Main Content", 7, 1668, true, true),
    GLOSSARIES("Glossaries", 1669, 1690, true, false),
    INDEX("Index", 1691, 1831, true, false);
    
    private final String label;
    private final int startPage;
    private final int endPage;
    private final boolean shouldIngest;
    private final boolean isStructuredContent;  // Has books/chapters/verses structure
    
    PageCategory(String label, int start, int end, boolean ingest, boolean structured) {
        this.label = label;
        this.startPage = start;
        this.endPage = end;
        this.shouldIngest = ingest;
        this.isStructuredContent = structured;
    }
    
    public String getLabel() {
        return label;
    }
    
    public int getStartPage() {
        return startPage;
    }
    
    public int getEndPage() {
        return endPage;
    }
    
    public boolean shouldIngest() {
        return shouldIngest;
    }
    
    public boolean isStructuredContent() {
        return isStructuredContent;
    }
    
    /**
     * Returns true if this category requires a specialized parser
     * (not the standard OahspeParser for books/chapters/verses).
     */
    public boolean requiresSpecialParser() {
        return this == GLOSSARIES || this == INDEX;
    }
    
    /**
     * Determines the appropriate category for a given page number.
     * 
     * @param pageNumber the 1-based page number from the PDF
     * @return the matching PageCategory
     * @throws IllegalArgumentException if page number is out of range
     */
    public static PageCategory fromPageNumber(int pageNumber) {
        for (PageCategory category : values()) {
            if (pageNumber >= category.startPage && pageNumber <= category.endPage) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid page number: " + pageNumber + 
                ". Valid range: 1-1831");
    }
    
    /**
     * Returns a human-readable description of this category.
     */
    public String getDescription() {
        return String.format("%s (pages %d-%d)%s", 
                label, 
                startPage, 
                endPage,
                shouldIngest ? " [INGEST]" : "");
    }
}
