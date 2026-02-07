package edu.minghualiu.oahspe.entities;

import org.junit.jupiter.api.Test;

import edu.minghualiu.oahspe.enums.PageCategory;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PageContent entity.
 */
class PageContentTest {
    
    @Test
    void testBuilder() {
        PageContent page = PageContent.builder()
                .pageNumber(100)
                .category(PageCategory.OAHSPE_BOOKS)
                .rawText("Sample text")
                .build();
        
        assertThat(page.getPageNumber()).isEqualTo(100);
        assertThat(page.getCategory()).isEqualTo(PageCategory.OAHSPE_BOOKS);
        assertThat(page.getRawText()).isEqualTo("Sample text");
        assertThat(page.getIngested()).isFalse();
        assertThat(page.hasError()).isFalse();
    }
    
    @Test
    void testMarkIngested() {
        PageContent page = PageContent.builder()
                .pageNumber(100)
                .category(PageCategory.OAHSPE_BOOKS)
                .rawText("Sample text")
                .build();
        
        assertThat(page.getIngested()).isFalse();
        assertThat(page.getIngestedAt()).isNull();
        
        page.markIngested();
        
        assertThat(page.getIngested()).isTrue();
        assertThat(page.getIngestedAt()).isNotNull();
        assertThat(page.getIngestedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    void testMarkError() {
        PageContent page = PageContent.builder()
                .pageNumber(100)
                .category(PageCategory.OAHSPE_BOOKS)
                .rawText("Sample text")
                .build();
        
        assertThat(page.hasError()).isFalse();
        assertThat(page.getErrorMessage()).isNull();
        
        page.markError("Test error");
        
        assertThat(page.hasError()).isTrue();
        assertThat(page.getErrorMessage()).isEqualTo("Test error");
    }
    
    @Test
    void testHasError_withError() {
        PageContent page = PageContent.builder()
                .pageNumber(100)
                .category(PageCategory.OAHSPE_BOOKS)
                .rawText("Sample text")
                .errorMessage("Error occurred")
                .build();
        
        assertThat(page.hasError()).isTrue();
    }
    
    @Test
    void testHasError_withoutError() {
        PageContent page = PageContent.builder()
                .pageNumber(100)
                .category(PageCategory.OAHSPE_BOOKS)
                .rawText("Sample text")
                .build();
        
        assertThat(page.hasError()).isFalse();
    }
    
    @Test
    void testExtractedAtDefault() {
        PageContent page = PageContent.builder()
                .pageNumber(100)
                .category(PageCategory.OAHSPE_BOOKS)
                .rawText("Sample text")
                .build();
        
        // extractedAt should be set automatically via @CreationTimestamp
        // In unit test without JPA, it will be null
        // This will be tested in integration tests
        assertThat(page.getExtractedAt()).isNull();
    }
}
