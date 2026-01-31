package edu.minghualiu.oahspe.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GlossaryTerm entity.
 */
class GlossaryTermTest {
    
    @Test
    void testBuilder() {
        GlossaryTerm term = GlossaryTerm.builder()
                .term("JEHOVIH")
                .definition("The Creator")
                .pageNumber(1670)
                .termType("spiritual")
                .build();
        
        assertThat(term.getTerm()).isEqualTo("JEHOVIH");
        assertThat(term.getDefinition()).isEqualTo("The Creator");
        assertThat(term.getPageNumber()).isEqualTo(1670);
        assertThat(term.getTermType()).isEqualTo("spiritual");
        assertThat(term.getUsageCount()).isZero();
    }
    
    @Test
    void testIncrementUsage() {
        GlossaryTerm term = GlossaryTerm.builder()
                .term("JEHOVIH")
                .definition("The Creator")
                .pageNumber(1670)
                .build();
        
        assertThat(term.getUsageCount()).isZero();
        
        term.incrementUsage();
        assertThat(term.getUsageCount()).isEqualTo(1);
        
        term.incrementUsage();
        assertThat(term.getUsageCount()).isEqualTo(2);
    }
    
    @Test
    void testIncrementUsage_fromNonZero() {
        GlossaryTerm term = GlossaryTerm.builder()
                .term("JEHOVIH")
                .definition("The Creator")
                .pageNumber(1670)
                .usageCount(5)
                .build();
        
        term.incrementUsage();
        assertThat(term.getUsageCount()).isEqualTo(6);
    }
}
