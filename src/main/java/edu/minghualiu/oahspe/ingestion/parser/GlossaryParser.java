package edu.minghualiu.oahspe.ingestion.parser;

import edu.minghualiu.oahspe.entities.GlossaryTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for glossary pages (pages 1669-1690).
 * Extracts term-definition pairs from glossary text.
 */
@Slf4j
@Service
public class GlossaryParser {
    
    // Pattern for glossary entries - typically term in uppercase/bold followed by definition
    // Example: "JEHOVIH: The Creator; the Supreme Being"
    private static final Pattern TERM_DEFINITION_PATTERN = 
            Pattern.compile("^([A-Z][A-Z\\s'-]+):\\s*(.+)$", Pattern.MULTILINE);
    
    // Pattern for multi-line definitions (continuation lines)
    private static final Pattern CONTINUATION_PATTERN = 
            Pattern.compile("^\\s{2,}(.+)$");
    
    /**
     * Parses a glossary page and extracts term-definition pairs.
     * 
     * @param rawText the extracted text from the glossary page
     * @param pageNumber the source page number
     * @return list of GlossaryTerm entities
     */
    public List<GlossaryTerm> parseGlossaryPage(String rawText, int pageNumber) {
        List<GlossaryTerm> terms = new ArrayList<>();
        
        if (rawText == null || rawText.trim().isEmpty()) {
            return terms;
        }
        
        String[] lines = rawText.split("\n");
        GlossaryTerm currentTerm = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty()) {
                continue;
            }
            
            // Try to match a new term-definition entry
            Matcher termMatcher = TERM_DEFINITION_PATTERN.matcher(line);
            if (termMatcher.matches()) {
                // Save previous term if exists
                if (currentTerm != null) {
                    terms.add(currentTerm);
                }
                
                // Start new term
                String term = termMatcher.group(1).trim();
                String definition = termMatcher.group(2).trim();
                
                currentTerm = GlossaryTerm.builder()
                        .term(term)
                        .definition(definition)
                        .pageNumber(pageNumber)
                        .termType(categorizeTermType(term, definition))
                        .usageCount(0)
                        .build();
                
                log.debug("Extracted glossary term: {} on page {}", term, pageNumber);
            } 
            // Check if this is a continuation of the current definition
            else if (currentTerm != null && !line.matches("^[A-Z][A-Z\\s'-]+:.*")) {
                // Append to current definition
                String updatedDefinition = currentTerm.getDefinition() + " " + line;
                currentTerm.setDefinition(updatedDefinition);
            }
        }
        
        // Add the last term
        if (currentTerm != null) {
            terms.add(currentTerm);
        }
        
        log.info("Parsed {} glossary terms from page {}", terms.size(), pageNumber);
        return terms;
    }
    
    /**
     * Extracts a single term-definition pair from a line.
     * Used for single-line glossary entries.
     * 
     * @param line the text line
     * @return GlossaryTerm or null if no match
     */
    public GlossaryTerm extractTermDefinition(String line) {
        Matcher matcher = TERM_DEFINITION_PATTERN.matcher(line.trim());
        
        if (matcher.matches()) {
            String term = matcher.group(1).trim();
            String definition = matcher.group(2).trim();
            
            return GlossaryTerm.builder()
                    .term(term)
                    .definition(definition)
                    .termType(categorizeTermType(term, definition))
                    .usageCount(0)
                    .build();
        }
        
        return null;
    }
    
    /**
     * Categorizes a term based on its content and definition.
     * Categories: spiritual, person, place, concept
     * 
     * @param term the glossary term
     * @param definition the definition text
     * @return the term type category
     */
    private String categorizeTermType(String term, String definition) {
        String lowerDef = definition.toLowerCase();
        String lowerTerm = term.toLowerCase();
        
        // Person indicators
        if (lowerDef.contains("god") || lowerDef.contains("deity") || 
            lowerDef.contains("creator") || lowerDef.contains("lord") ||
            lowerDef.contains("angel") || lowerDef.contains("spirit")) {
            return "spiritual";
        }
        
        // Place indicators
        if (lowerDef.contains("heaven") || lowerDef.contains("realm") ||
            lowerDef.contains("world") || lowerDef.contains("region") ||
            lowerDef.contains("kingdom") || lowerDef.contains("place")) {
            return "place";
        }
        
        // Person names (capitalized multi-word terms)
        if (term.contains(" ") && Character.isUpperCase(term.charAt(0))) {
            if (lowerDef.contains("person") || lowerDef.contains("prophet") ||
                lowerDef.contains("teacher") || lowerDef.contains("leader")) {
                return "person";
            }
        }
        
        // Default to concept
        return "concept";
    }
}
