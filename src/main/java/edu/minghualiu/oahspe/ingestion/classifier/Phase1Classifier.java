package edu.minghualiu.oahspe.ingestion.classifier;

import edu.minghualiu.oahspe.records.PageClassificationResult;
import edu.minghualiu.oahspe.entities.PageContent;
import edu.minghualiu.oahspe.enums.PageType;
import java.util.regex.Pattern;

public class Phase1Classifier implements PageClassifier {

    private static final Pattern VERSE_PATTERN = Pattern.compile("[0-9]{2}/[0-9]{1,2}\\.[0-9]{1,2}"); 
    private static final Pattern FOOTNOTE_NUMBER_PATTERN = Pattern.compile("(?m)^[0-9]{1,3}$"); 
    private static final Pattern PLATE_CAPTION_PATTERN = Pattern.compile("i[0-9]{3}"); 
    
    @Override 
    public PageClassificationResult classify(PageContent pc) { 
        String text = pc.getRawText(); 
        boolean hasVerses = VERSE_PATTERN.matcher(text).find(); 
        boolean hasStandaloneNumbers = FOOTNOTE_NUMBER_PATTERN.matcher(text).find(); 
        boolean hasPlateCaption = PLATE_CAPTION_PATTERN.matcher(text).find(); 
        boolean hasSaphahMarkers = text.contains("Poit") || text.contains("Panic") || 
        text.contains("Se'moin") || text.contains("35/B"); 
        
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
        // 3. Scripture with footnotes 
        else if (hasVerses && hasStandaloneNumbers) { 
            pageType = PageType.SCRIPTURE_WITH_FOOTNOTES;
        } 
        // 4. Scripture without footnotes 
        else if (hasVerses) { 
            pageType = PageType.SCRIPTURE;
        } 
        // 5. Saphah commentary pages 
        else if (hasSaphahMarkers) { 
            pageType = PageType.SAPHAH_COMMENTARY;
        } 
        // 6. Decorative or symbolic pages 
        else if (text.length() < 80) { 
            pageType = PageType.DECORATIVE_OR_SYMBOLIC;
        } 
        // 7. Fallback 
        else {
            pageType = PageType.MIXED_CONTENT;
        }
        
        return new PageClassificationResult(pageType, needsGeometry);
    }
}
