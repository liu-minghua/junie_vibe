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
        private static final Pattern FOOTNOTE_WITH_TEXT_PATTERN = Pattern.compile("(?m)^\\s*\\d{1,4}(?:[).:-])?\\s+\\S.*$");
        private static final Pattern PLATE_CAPTION_PATTERN = Pattern.compile("\\bi\\d{3,4}\\b");
    
    @Override 
    public PageClassificationResult classify(PageContent pc) { 
        String text = pc.getRawText();
        
        if (text == null || text.isEmpty()) {
            return new PageClassificationResult(PageType.DECORATIVE_OR_SYMBOLIC, true);
        }
        
        boolean hasVerses = VERSE_PATTERN.matcher(text).find();
        boolean hasPlateCaption = PLATE_CAPTION_PATTERN.matcher(text).find();
        boolean hasSaphahMarkers = text.contains("Poit") || text.contains("Panic") ||
            text.contains("Se'moin") || text.contains("35/B") ||
            text.contains("Ebra") || text.contains("Chine") ||
            text.contains("Vede") || text.contains("Algonquin");
        boolean isTitlePage = isTitlePage(text);
        
        PageType pageType;
        boolean needsGeometry = false;
        
        // 1. Plate-only pages (images without verses)
        if (pc.getContainsImages() && !hasVerses) { 
            pageType = PageType.PLATE;
            needsGeometry = true;
        } 
        // 2. Plate caption pages (captions without verses)
        else if (hasPlateCaption && !hasVerses) { 
            pageType = PageType.PLATE_CAPTION;
            needsGeometry = pc.getContainsImages();  // Only needs geometry if has images
        } 
        // 3. Title pages
        else if (isTitlePage) {
            pageType = PageType.TITLE_PAGE;
            needsGeometry = false;  // Title pages are purely text
        }
        // 4/5. Scripture pages (with or without footnotes)
        else if (hasVerses) { 
            boolean hasFootnotes = hasFootnoteLines(text);
            boolean canSplitFootnotes = hasFootnotes && canAccuratelyParseVerseAndFootnotes(text);

            if (canSplitFootnotes) {
                pageType = PageType.SCRIPTURE_WITH_FOOTNOTES;
                needsGeometry = false;
            } else {
                pageType = PageType.SCRIPTURE;
                // If footnotes exist but can't be split cleanly, require geometry
                needsGeometry = !canAccuratelyParseVerses(text) || hasFootnotes;
            }
        } 
        // 6. Saphah commentary pages (glossaries, definitions)
        else if (hasSaphahMarkers) { 
            pageType = PageType.SAPHAH_COMMENTARY;
            // Good text quality = no geometry needed (pure text ingestion)
            needsGeometry = !isTextQualityGood(text);
        } 
        // 7. Mixed content or poor-quality text pages
        else if (!isTextQualityGood(text) || pc.getContainsImages()) {
            pageType = PageType.MIXED_CONTENT;
            needsGeometry = true;  // Poor text or images = need geometry
        }
        // 8. Decorative or symbolic pages (minimal text)
        else if (text.length() < 80) { 
            pageType = PageType.DECORATIVE_OR_SYMBOLIC;
            needsGeometry = true;
        } 
        // 9. Fallback to mixed content
        else {
            pageType = PageType.MIXED_CONTENT;
            needsGeometry = false;  // If text quality is ok, don't require geometry
        }
        
        return new PageClassificationResult(pageType, needsGeometry);
    }
    
    /**
     * Checks if text quality is good enough for accurate text-based parsing.
     * Uses lenient thresholds since OCR documents often have formatting issues.
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
        
        // Lenient whitespace check - allow up to 80% whitespace (PDFs have lots of formatting)
        long whitespaceCount = text.chars().filter(Character::isWhitespace).count();
        double whitespaceRatio = (double) whitespaceCount / text.length();
        if (whitespaceRatio > 0.85) {
            return false;  // Nearly empty pages
        }
        
        // Lenient special character check - allow up to 50% special chars (OCR may add punctuation)
        long specialCharCount = text.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
                .count();
        double specialCharRatio = (double) specialCharCount / text.length();
        if (specialCharRatio > 0.5) {
            return false;  // Likely OCR garbage
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

    private boolean isFootnoteLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return FOOTNOTE_NUMBER_PATTERN.matcher(trimmed).matches()
                || FOOTNOTE_WITH_TEXT_PATTERN.matcher(trimmed).matches();
    }

    private boolean hasFootnoteLines(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (isFootnoteLine(line)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if verses can be accurately parsed from the text.
     * Uses lenient thresholds - if verses are detected, assume they can be parsed.
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
        
        // Very lenient verse density checks (5 chars min, 5000 chars max per verse)
        int textLength = text.length();
        double charsPerVerse = (double) textLength / verseCount;
        
        // If more than 5000 chars per verse, probably not scripture
        if (charsPerVerse > 5000) {
            return false;
        }
        
        // If less than 5 chars per verse, likely garbage
        if (charsPerVerse < 5) {
            return false;
        }
        
        // If verse detected and reasonable text quality, assume it's parseable
        return true;
    }
    
    /**
     * Checks if verses and footnotes can be accurately separated.
     * Uses lenient thresholds - if both are detected, assume they can be parsed.
     */
    private boolean canAccuratelyParseVerseAndFootnotes(String text) {
        if (!canAccuratelyParseVerses(text)) {
            return false;
        }
        
        String[] lines = text.split("\\r?\\n");
        int lastVerseIndex = -1;
        int firstFootnoteIndex = -1;
        int footnoteCount = 0;
        int verseLineCount = 0;
        int verseLinesAfterBoundary = 0;
        int footnotesBeforeBoundary = 0;
        int footnotesAfterBoundary = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean isVerseLine = VERSE_PATTERN.matcher(line).find();
            boolean isFootnote = isFootnoteLine(line);
            
            if (isVerseLine) {
                verseLineCount++;
                lastVerseIndex = i;
            }
            if (isFootnote) {
                footnoteCount++;
                if (firstFootnoteIndex == -1) {
                    firstFootnoteIndex = i;
                }
            }
        }
        
        if (footnoteCount < 2 || lastVerseIndex == -1) {
            return false;
        }
        
        // Require a clear boundary: footnotes start after the last verse line
        if (firstFootnoteIndex <= lastVerseIndex) {
            return false;
        }
        
        for (int i = 0; i < lines.length; i++) {
            boolean isFootnote = isFootnoteLine(lines[i]);
            boolean isVerseLine = VERSE_PATTERN.matcher(lines[i]).find();
            if (i < firstFootnoteIndex && isFootnote) {
                footnotesBeforeBoundary++;
            }
            if (i >= firstFootnoteIndex) {
                if (isFootnote) {
                    footnotesAfterBoundary++;
                }
                if (isVerseLine) {
                    verseLinesAfterBoundary++;
                }
            }
        }
        
        int linesBefore = Math.max(firstFootnoteIndex, 1);
        int linesAfter = Math.max(lines.length - firstFootnoteIndex, 1);
        double beforeFootnoteRatio = (double) footnotesBeforeBoundary / linesBefore;
        double afterFootnoteRatio = (double) footnotesAfterBoundary / linesAfter;
        double afterVerseRatio = (double) verseLinesAfterBoundary / linesAfter;
        
        // Keep footnotes largely in the trailing block and verses largely before it
        if (beforeFootnoteRatio > 0.25) {
            return false;
        }
        if (afterFootnoteRatio < 0.05) {
            return false;
        }
        if (afterVerseRatio > 0.2) {
            return false;
        }
        
        // Text quality already checked in canAccuratelyParseVerses
        return true;
    }
}
