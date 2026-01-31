package edu.minghualiu.oahspe.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ContentLinkingReport.
 */
class ContentLinkingReportTest {
    
    @Test
    void testBuilder() {
        ContentLinkingReport report = ContentLinkingReport.builder()
                .totalPages(1831)
                .pagesLinked(1800)
                .booksWithPageNumber(43)
                .booksWithoutPageNumber(0)
                .chaptersWithPageNumber(250)
                .chaptersWithoutPageNumber(10)
                .versesWithPageNumber(5000)
                .versesWithoutPageNumber(100)
                .build();
        
        assertThat(report.getTotalPages()).isEqualTo(1831);
        assertThat(report.getPagesLinked()).isEqualTo(1800);
        assertThat(report.getBooksWithPageNumber()).isEqualTo(43);
        assertThat(report.getChaptersWithPageNumber()).isEqualTo(250);
    }
    
    @Test
    void testGetSuccessRate_perfectLinking() {
        ContentLinkingReport report = ContentLinkingReport.builder()
                .totalPages(1831)
                .pagesLinked(1831)
                .build();
        
        assertThat(report.getSuccessRate()).isEqualTo(100.0);
    }
    
    @Test
    void testGetSuccessRate_partialLinking() {
        ContentLinkingReport report = ContentLinkingReport.builder()
                .totalPages(100)
                .pagesLinked(80)
                .build();
        
        assertThat(report.getSuccessRate()).isEqualTo(80.0);
    }
    
    @Test
    void testGetSuccessRate_noContent() {
        ContentLinkingReport report = ContentLinkingReport.builder()
                .totalPages(0)
                .pagesLinked(0)
                .build();
        
        assertThat(report.getSuccessRate()).isEqualTo(0.0);
    }
    
    @Test
    void testIsFullyLinked_true() {
        ContentLinkingReport report = ContentLinkingReport.builder()
                .booksWithoutPageNumber(0)
                .chaptersWithoutPageNumber(0)
                .versesWithoutPageNumber(0)
                .notesWithoutPageNumber(0)
                .build();
        
        assertThat(report.isFullyLinked()).isTrue();
    }
    
    @Test
    void testIsFullyLinked_false_booksUnlinked() {
        ContentLinkingReport report = ContentLinkingReport.builder()
                .booksWithoutPageNumber(1)
                .chaptersWithoutPageNumber(0)
                .versesWithoutPageNumber(0)
                .notesWithoutPageNumber(0)
                .build();
        
        assertThat(report.isFullyLinked()).isFalse();
    }
    
    @Test
    void testIsFullyLinked_false_versesUnlinked() {
        ContentLinkingReport report = ContentLinkingReport.builder()
                .booksWithoutPageNumber(0)
                .chaptersWithoutPageNumber(0)
                .versesWithoutPageNumber(1)
                .notesWithoutPageNumber(0)
                .build();
        
        assertThat(report.isFullyLinked()).isFalse();
    }
    
    @Test
    void testGetSummary() {
        ContentLinkingReport report = ContentLinkingReport.builder()
                .totalPages(1831)
                .pagesLinked(1800)
                .imagesLinked(150)
                .orphanedImages(5)
                .booksWithPageNumber(40)
                .booksWithoutPageNumber(3)
                .chaptersWithPageNumber(240)
                .chaptersWithoutPageNumber(10)
                .versesWithPageNumber(4900)
                .versesWithoutPageNumber(100)
                .notesWithPageNumber(95)
                .notesWithoutPageNumber(5)
                .build();
        
        String summary = report.getSummary();
        
        assertThat(summary).contains("Books: 40 with page, 3 without");
        assertThat(summary).contains("Chapters: 240 with page, 10 without");
        assertThat(summary).contains("Verses: 4900 with page, 100 without");
        assertThat(summary).contains("Notes: 95 with page, 5 without");
    }
}
