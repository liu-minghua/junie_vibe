package edu.minghualiu.oahspe.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PageCategory enum.
 */
class PageCategoryTest {
    
    @ParameterizedTest
    @CsvSource({
        "1, COVER",
        "2, COVER",
        "3, COVER",
        "4, TABLE_OF_CONTENTS",
        "5, IMAGE_LIST",
        "6, IMAGE_LIST",
        "7, OAHSPE_BOOKS",
        "100, OAHSPE_BOOKS",
        "1668, OAHSPE_BOOKS",
        "1669, GLOSSARIES",
        "1680, GLOSSARIES",
        "1690, GLOSSARIES",
        "1691, INDEX",
        "1700, INDEX",
        "1831, INDEX"
    })
    void testFromPageNumber(int pageNumber, PageCategory expected) {
        PageCategory actual = PageCategory.fromPageNumber(pageNumber);
        assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    void testFromPageNumber_boundaryPages() {
        // Test exact boundaries
        assertThat(PageCategory.fromPageNumber(1)).isEqualTo(PageCategory.COVER);
        assertThat(PageCategory.fromPageNumber(3)).isEqualTo(PageCategory.COVER);
        assertThat(PageCategory.fromPageNumber(4)).isEqualTo(PageCategory.TABLE_OF_CONTENTS);
        assertThat(PageCategory.fromPageNumber(5)).isEqualTo(PageCategory.IMAGE_LIST);
        assertThat(PageCategory.fromPageNumber(6)).isEqualTo(PageCategory.IMAGE_LIST);
        assertThat(PageCategory.fromPageNumber(7)).isEqualTo(PageCategory.OAHSPE_BOOKS);
        assertThat(PageCategory.fromPageNumber(1668)).isEqualTo(PageCategory.OAHSPE_BOOKS);
        assertThat(PageCategory.fromPageNumber(1669)).isEqualTo(PageCategory.GLOSSARIES);
        assertThat(PageCategory.fromPageNumber(1690)).isEqualTo(PageCategory.GLOSSARIES);
        assertThat(PageCategory.fromPageNumber(1691)).isEqualTo(PageCategory.INDEX);
        assertThat(PageCategory.fromPageNumber(1831)).isEqualTo(PageCategory.INDEX);
    }
    
    @Test
    void testFromPageNumber_invalidPages() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> 
            PageCategory.fromPageNumber(0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> 
            PageCategory.fromPageNumber(-1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> 
            PageCategory.fromPageNumber(1832));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> 
            PageCategory.fromPageNumber(9999));
    }
    
    @Test
    void testShouldIngest_coverPagesNotIngested() {
        assertThat(PageCategory.COVER.shouldIngest()).isFalse();
    }
    
    @Test
    void testShouldIngest_tableOfContentsNotIngested() {
        assertThat(PageCategory.TABLE_OF_CONTENTS.shouldIngest()).isFalse();
    }
    
    @Test
    void testShouldIngest_imageListNotIngested() {
        assertThat(PageCategory.IMAGE_LIST.shouldIngest()).isFalse();
    }
    
    @Test
    void testShouldIngest_oahspeBooksIngested() {
        assertThat(PageCategory.OAHSPE_BOOKS.shouldIngest()).isTrue();
    }
    
    @Test
    void testShouldIngest_glossariesIngested() {
        assertThat(PageCategory.GLOSSARIES.shouldIngest()).isTrue();
    }
    
    @Test
    void testShouldIngest_indexIngested() {
        assertThat(PageCategory.INDEX.shouldIngest()).isTrue();
    }
    
    @Test
    void testRequiresSpecialParser_oahspeBooksFalse() {
        assertThat(PageCategory.OAHSPE_BOOKS.requiresSpecialParser()).isFalse();
    }
    
    @Test
    void testRequiresSpecialParser_glossariesTrue() {
        assertThat(PageCategory.GLOSSARIES.requiresSpecialParser()).isTrue();
    }
    
    @Test
    void testRequiresSpecialParser_indexTrue() {
        assertThat(PageCategory.INDEX.requiresSpecialParser()).isTrue();
    }
    
    @Test
    void testPageRanges_coverPages() {
        assertThat(PageCategory.COVER.getStartPage()).isEqualTo(1);
        assertThat(PageCategory.COVER.getEndPage()).isEqualTo(3);
    }
    
    @Test
    void testPageRanges_tableOfContents() {
        assertThat(PageCategory.TABLE_OF_CONTENTS.getStartPage()).isEqualTo(4);
        assertThat(PageCategory.TABLE_OF_CONTENTS.getEndPage()).isEqualTo(4);
    }
    
    @Test
    void testPageRanges_oahspeBooks() {
        assertThat(PageCategory.OAHSPE_BOOKS.getStartPage()).isEqualTo(7);
        assertThat(PageCategory.OAHSPE_BOOKS.getEndPage()).isEqualTo(1668);
    }
    
    @Test
    void testPageRanges_glossaries() {
        assertThat(PageCategory.GLOSSARIES.getStartPage()).isEqualTo(1668);  // Overlaps with OAHSPE_BOOKS
        assertThat(PageCategory.GLOSSARIES.getEndPage()).isEqualTo(1690);
    }
    
    @Test
    void testPageRanges_index() {
        assertThat(PageCategory.INDEX.getStartPage()).isEqualTo(1691);
        assertThat(PageCategory.INDEX.getEndPage()).isEqualTo(1831);
    }
}
