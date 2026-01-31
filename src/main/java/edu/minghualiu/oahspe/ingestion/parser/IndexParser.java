package edu.minghualiu.oahspe.ingestion.parser;

import edu.minghualiu.oahspe.entities.IndexEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for index pages (pages 1691-1831).
 * Extracts topic-page reference mappings from index text.
 */
@Slf4j
@Service
public class IndexParser {
    
    // Pattern for index entries - topic followed by page numbers
    // Example: "Angels, 42, 108, 234-240"
    // Example: "Creation story, see also Genesis, 15, 67"
    private static final Pattern INDEX_ENTRY_PATTERN = 
            Pattern.compile("^(.+?),\\s*([0-9,\\s-]+)$");
    
    // Pattern for cross-references
    private static final Pattern CROSS_REF_PATTERN = 
            Pattern.compile("(.+?),\\s*see\\s+also\\s+(.+?),\\s*([0-9,\\s-]+)$", Pattern.CASE_INSENSITIVE);
    
    /**
     * Parses an index page and extracts topic-page mappings.
     * 
     * @param rawText the extracted text from the index page
     * @param pageNumber the source page number
     * @return list of IndexEntry entities
     */
    public List<IndexEntry> parseIndexPage(String rawText, int pageNumber) {
        List<IndexEntry> entries = new ArrayList<>();
        
        if (rawText == null || rawText.trim().isEmpty()) {
            return entries;
        }
        
        String[] lines = rawText.split("\n");
        String currentTopic = null;
        StringBuilder currentReferences = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty()) {
                // Save accumulated entry
                if (currentTopic != null && currentReferences.length() > 0) {
                    IndexEntry entry = createIndexEntry(currentTopic, 
                            currentReferences.toString(), pageNumber);
                    if (entry != null) {
                        entries.add(entry);
                    }
                    currentTopic = null;
                    currentReferences = new StringBuilder();
                }
                continue;
            }
            
            // Try to extract a complete index entry
            IndexEntry extracted = extractTopicReferences(line);
            if (extracted != null) {
                // Save previous accumulated entry if exists
                if (currentTopic != null && currentReferences.length() > 0) {
                    IndexEntry entry = createIndexEntry(currentTopic, 
                            currentReferences.toString(), pageNumber);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
                
                // Use the extracted entry
                extracted.setExtractedFromPage(pageNumber);
                entries.add(extracted);
                currentTopic = null;
                currentReferences = new StringBuilder();
                
                log.debug("Extracted index entry: {} on page {}", 
                        extracted.getTopic(), pageNumber);
            } 
            // Check if this is a topic line (no page numbers yet)
            else if (!line.matches(".*\\d+.*") && currentTopic == null) {
                currentTopic = line;
            } 
            // Continuation line with page numbers
            else if (currentTopic != null) {
                if (currentReferences.length() > 0) {
                    currentReferences.append(", ");
                }
                currentReferences.append(line);
            }
        }
        
        // Add the last entry if exists
        if (currentTopic != null && currentReferences.length() > 0) {
            IndexEntry entry = createIndexEntry(currentTopic, 
                    currentReferences.toString(), pageNumber);
            if (entry != null) {
                entries.add(entry);
            }
        }
        
        log.info("Parsed {} index entries from page {}", entries.size(), pageNumber);
        return entries;
    }
    
    /**
     * Extracts a single index entry from a line.
     * 
     * @param line the text line
     * @return IndexEntry or null if no match
     */
    public IndexEntry extractTopicReferences(String line) {
        line = line.trim();
        
        // Try cross-reference pattern first
        Matcher crossRefMatcher = CROSS_REF_PATTERN.matcher(line);
        if (crossRefMatcher.matches()) {
            String topic = crossRefMatcher.group(1).trim();
            String crossRef = crossRefMatcher.group(2).trim();
            String pageRefs = crossRefMatcher.group(3).trim();
            
            // Include cross-reference in topic
            String fullTopic = topic + " (see also: " + crossRef + ")";
            
            return IndexEntry.builder()
                    .topic(fullTopic)
                    .pageReferences(pageRefs)
                    .build();
        }
        
        // Try standard index entry pattern
        Matcher matcher = INDEX_ENTRY_PATTERN.matcher(line);
        if (matcher.matches()) {
            String topic = matcher.group(1).trim();
            String pageRefs = matcher.group(2).trim();
            
            return IndexEntry.builder()
                    .topic(topic)
                    .pageReferences(pageRefs)
                    .build();
        }
        
        return null;
    }
    
    /**
     * Creates an IndexEntry from topic and references.
     * 
     * @param topic the topic text
     * @param references the page references
     * @param pageNumber the source page
     * @return IndexEntry or null if invalid
     */
    private IndexEntry createIndexEntry(String topic, String references, int pageNumber) {
        if (topic == null || topic.trim().isEmpty()) {
            return null;
        }
        
        if (references == null || references.trim().isEmpty()) {
            return null;
        }
        
        return IndexEntry.builder()
                .topic(topic.trim())
                .pageReferences(references.trim())
                .extractedFromPage(pageNumber)
                .build();
    }
}
