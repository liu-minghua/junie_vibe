package edu.minghualiu.oahspe.ingestion.classifier;

import edu.minghualiu.oahspe.records.PageClassificationResult;
import edu.minghualiu.oahspe.entities.PageContent;
import edu.minghualiu.oahspe.enums.PageType;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class Phase1Classifier implements PageClassifier {

        private static final Pattern VERSE_PATTERN =
            Pattern.compile("\\b\\d{1,2}/(?:[A-Z]{1,3}|\\d{1,2})\\.\\d{1,3}\\b");
        private static final Pattern FOOTNOTE_NUMBER_PATTERN = Pattern.compile("(?m)^\\s*\\d{1,4}\\s*$");
        private static final Pattern PLATE_CAPTION_PATTERN = Pattern.compile("\\bi\\d{3,4}\\b");
    
    @Override 
    public PageClassificationResult classify(PageContent pc) { 
        String text = pc.getRawText();
        
        if (text == null || text.isEmpty()) {
            return new PageClassificationResult(PageType.DECORATIVE_OR_SYMBOLIC, true);
        }
        
        boolean hasVerses = VERSE_PATTERN.matcher(text).find();
        boolean hasStandaloneNumbers = FOOTNOTE_NUMBER_PATTERN.matcher(text).find();
        boolean hasPlateCaption = PLATE_CAPTION_PATTERN.matcher(text).find();
        boolean hasSaphahMarkers = text.contains("Poit") || text.contains("Panic") ||
            text.contains("Se'moin") || text.contains("35/B") ||
            text.contains("Ebra") || text.contains("Chine") ||
            text.contains("Vede") || text.contains("Algonquin");
        boolean isTitlePage = isTitlePage(text);
        
        PageType pageType;
        boolean needsGeometry = false;
        
        // 1. Plate-only pages 
        if (pc.getContainsImages() && !hasVerses) { 
            pageType = PageType.PLATE;
            needsGeometry = true;
        } 
        // 2. Plate caption pages 
        else if (hasPlateCaption && !hasVerses) { 
            pageType = PageType.PLATE_CAPTION;
            needsGeometry = true;
        } 
        // 3. Title pages
        else if (isTitlePage) {
            pageType = PageType.TITLE_PAGE;
            needsGeometry = true;
        }
        // 4. Scripture with footnotes 
        else if (hasVerses && hasStandaloneNumbers) { 
            pageType = PageType.SCRIPTURE_WITH_FOOTNOTES;
            // Check if verse and footnote parsing would be accurate
            needsGeometry = !canAccuratelyParseVerseAndFootnotes(text);
        } 
        // 5. Scripture without footnotes 
        else if (hasVerses) { 
            pageType = PageType.SCRIPTURE;
            // Check if verse parsing would be accurate
            needsGeometry = !canAccuratelyParseVerses(text);
        } 
        // 6. Saphah commentary pages 
        else if (hasSaphahMarkers) { 
            pageType = PageType.SAPHAH_COMMENTARY;
            needsGeometry = !isTextQualityGood(text);
        } 
        // 7. Decorative or symbolic pages 
        else if (text.length() < 80) { 
            pageType = PageType.DECORATIVE_OR_SYMBOLIC;
            needsGeometry = true;
        } 
        // 8. Fallback 
        else {
            pageType = PageType.MIXED_CONTENT;
            needsGeometry = true;
        }
        
        return new PageClassificationResult(pageType, needsGeometry);
    }
    
    /**
     * Checks if text quality is good enough for accurate text-based parsing.
     */
    private boolean isTextQualityGood(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Check for minimum meaningful content
        String trimmed = text.trim();
        if (trimmed.length() < 20) {
            return false;
        }
        
        // Check whitespace ratio - too much whitespace suggests formatting issues
        long whitespaceCount = text.chars().filter(Character::isWhitespace).count();
        double whitespaceRatio = (double) whitespaceCount / text.length();
        if (whitespaceRatio > 0.7) {
            return false;
        }
        
        // Check for excessive special characters (suggests OCR issues)
        long specialCharCount = text.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
                .count();
        double specialCharRatio = (double) specialCharCount / text.length();
        if (specialCharRatio > 0.3) {
            return false;
        }
        
        return true;
    }

    private boolean isTitlePage(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String lower = text.toLowerCase();
        return lower.contains("oahspe")
                && (lower.contains("practical guidebook")
                || lower.contains("standard edition")
                || lower.contains("spiritual life"));
    }
    
    /**
     * Checks if verses can be accurately parsed from the text.
     */
    private boolean canAccuratelyParseVerses(String text) {
        if (!isTextQualityGood(text)) {
            return false;
        }
        
        // Count verse markers
        long verseCount = VERSE_PATTERN.matcher(text).results().count();
        
        // Should have at least one verse
        if (verseCount == 0) {
            return false;
        }
        
        // Check for reasonable verse density (verses shouldn't be too sparse)
        int textLength = text.length();
        double charsPerVerse = (double) textLength / verseCount;
        
        // If more than 2000 chars per verse, might be parsing issues
        if (charsPerVerse > 2000) {
            return false;
        }
        
        // If less than 10 chars per verse, likely formatting corruption
        if (charsPerVerse < 10) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if verses and footnotes can be accurately separated.
     */
    private boolean canAccuratelyParseVerseAndFootnotes(String text) {
        if (!canAccuratelyParseVerses(text)) {
            return false;
        }
        
        // Split by lines to check footnote structure
        String[] lines = text.split("\\r?\\n");
        
        // Count lines that are just numbers (potential footnotes)
        long footnoteLines = 0;
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (FOOTNOTE_NUMBER_PATTERN.matcher(trimmedLine).matches()) {
                footnoteLines++;
            }
        }
        
        // Should have some footnote markers
        if (footnoteLines == 0) {
            return false;
        }
        
        // Check for reasonable footnote-to-content ratio
        double footnoteRatio = (double) footnoteLines / lines.length;
        
        // Too many footnote lines suggests parsing issues
        if (footnoteRatio > 0.5) {
            return false;
        }
        
        // Look for clear separator between verses and footnotes
        // Footnotes typically appear after the main text
        boolean foundFootnoteSection = false;
        int lastVerseIndex = -1;
        int firstFootnoteIndex = -1;
        
        for (int i = 0; i < lines.length; i++) {
            if (VERSE_PATTERN.matcher(lines[i]).find()) {
                lastVerseIndex = i;
            }
            if (FOOTNOTE_NUMBER_PATTERN.matcher(lines[i].trim()).matches() && firstFootnoteIndex == -1) {
                firstFootnoteIndex = i;
            }
        }
        
        // Footnotes should appear after verses
        if (firstFootnoteIndex > 0 && lastVerseIndex > 0 && firstFootnoteIndex > lastVerseIndex) {
            foundFootnoteSection = true;
        }
        
        return foundFootnoteSection;
    }
}
