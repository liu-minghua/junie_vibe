package edu.minghualiu.oahspe.ingestion.loader;

import edu.minghualiu.oahspe.entities.PageCategory;
import edu.minghualiu.oahspe.entities.PageContent;
import edu.minghualiu.oahspe.entities.PageImage;
import edu.minghualiu.oahspe.entities.PageRangeContentSummary;
import edu.minghualiu.oahspe.ingestion.runner.IngestionContext;
import edu.minghualiu.oahspe.ingestion.runner.PDFExtractionException;
import edu.minghualiu.oahspe.ingestion.runner.PDFTextExtractor;
import edu.minghualiu.oahspe.ingestion.runner.ProgressCallback;
import edu.minghualiu.oahspe.repositories.PageContentRepository;
import edu.minghualiu.oahspe.repositories.PageImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.cos.COSName;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading PDF pages into PageContent entities.
 * Phase 1 of the page-based ingestion workflow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageLoader {
    
    private final PDFTextExtractor pdfTextExtractor;
    private final PageContentRepository pageContentRepository;
    private final PageImageRepository pageImageRepository;
    private final TransactionTemplate transactionTemplate;
    
    private static final int BATCH_SIZE = 100;  // Commit every 100 pages
    
    /**Uses batch commits every 100 pages for progress visibility.
     * 
     * @param pdfPath absolute path to the PDF file
     * @param callback optional progress callback
     * @return ingestion context with statistics
     */
    public IngestionContext loadAllPages(String pdfPath, ProgressCallback callback) {
        log.info("Starting page loading from PDF: {}", pdfPath);
        
        int totalPages;
        try {
            totalPages = pdfTextExtractor.getPageCount(pdfPath);
        } catch (PDFExtractionException e) {
            log.error("Failed to get page count: {}", e.getMessage());
            throw new RuntimeException("Cannot load pages: " + e.getMessage(), e);
        }
        
        IngestionContext context = new IngestionContext(pdfPath, totalPages);
        List<Integer> batch = new ArrayList<>();
        
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            context.setCurrentPageNumber(pageNum);
            batch.add(pageNum);
            
            // Commit batch every BATCH_SIZE pages or at the end
            if (batch.size() >= BATCH_SIZE || pageNum == totalPages) {
                final List<Integer> pagesToLoad = new ArrayList<>(batch);
                
                transactionTemplate.executeWithoutResult(status -> {
                    for (Integer page : pagesToLoad) {
                        try {
                            if (callback != null && page % 50 == 0) {
                                callback.onPageStart(page, totalPages);
                            }
                            
                            PageContent pageContent = loadSinglePage(pdfPath, page);
                            context.setTotalEventsProcessed(context.getTotalEventsProcessed() + 1);
                            
                            if (callback != null && page % 50 == 0) {
                                callback.onPageComplete(page, 1);
                            }
                        } catch (Exception e) {
                            context.addPageError(page, e.getMessage());
                            log.error("Failed to load page {}: {}", page, e.getMessage(), e);
                        }
                    }
                });
                
                log.info("Committed batch: pages {}-{}", 
                        pagesToLoad.get(0), 
                        pagesToLoad.get(pagesToLoad.size() - 1));
                batch.clear();
            }
        }
        
        log.info("Page loading complete. Pages: {}, Errors: {}", 
                totalPages, context.getTotalErrorsEncountered());
        
        return context;
    }
    
    /**
     * Loads a single page from the PDF.
     * Must be called within a transaction context.
     * 
     * @param pdfPath absolute path to the PDF file
     * @param pageNumber 1-based page number
     * @return the created PageContent entity
     */
    private PageContent loadSinglePage(String pdfPath, int pageNumber) {
        log.debug("Loading page {}", pageNumber);
        
        // Check if page already exists
        return pageContentRepository.findByPageNumber(pageNumber)
                .orElseGet(() -> {
                    // Extract text
                    String rawText;
                    try {
                        rawText = pdfTextExtractor.extractText(pdfPath, pageNumber);
                    } catch (PDFExtractionException e) {
                        log.error("Text extraction failed for page {}: {}", pageNumber, e.getMessage());
                        rawText = "";
                    }
                    
                    // Determine category
                    PageCategory category = PageCategory.fromPageNumber(pageNumber);
                    
                    // Create PageContent
                    PageContent pageContent = PageContent.builder()
                            .pageNumber(pageNumber)
                            .category(category)
                            .rawText(rawText)
                            .extractedAt(LocalDateTime.now())
                            .ingested(false)
                            .build();
                    
                    pageContent = pageContentRepository.save(pageContent);
                    
                    // Extract images
                    List<PageImage> images = extractImagesFromPage(pdfPath, pageNumber, pageContent);
                    pageImageRepository.saveAll(images);
                    
                    log.debug("Loaded page {} [{}] - {} chars, {} images", 
                            pageNumber, category, rawText.length(), images.size());
                    
                    return pageContent;
                });
    }
    
    /**
     * Extracts images from a PDF page as PageImage objects.
     * 
     * @param pdfPath path to PDF
     * @param pageNumber 1-based page number
     * @param pageContent the parent PageContent entity
     * @return list of PageImage objects
     */
    private List<PageImage> extractImagesFromPage(String pdfPath, int pageNumber, PageContent pageContent) {
        List<PageImage> pageImages = new ArrayList<>();
        
        File file = new File(pdfPath);
        if (!file.exists()) {
            log.warn("PDF file not found for image extraction: {}", pdfPath);
            return pageImages;
        }
        
        try (PDDocument document = PDDocument.load(file)) {
            if (pageNumber < 1 || pageNumber > document.getNumberOfPages()) {
                log.warn("Page number {} out of range", pageNumber);
                return pageImages;
            }
            
            PDPage page = document.getPage(pageNumber - 1); // 0-indexed
            PDResources resources = page.getResources();
            
            if (resources == null) {
                return pageImages;
            }
            
            int sequence = 1;
            for (COSName name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);
                
                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;
                    
                    try {
                        BufferedImage bufferedImage = image.getImage();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "PNG", baos);
                        
                        PageImage pageImage = PageImage.builder()
                                .pageContent(pageContent)
                                .imageSequence(sequence++)
                                .imageData(baos.toByteArray())
                                .mimeType("image/png")
                                .build();
                        
                        pageImages.add(pageImage);
                        
                    } catch (IOException e) {
                        log.warn("Failed to extract image {} from page {}: {}", 
                                sequence, pageNumber, e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            log.error("Failed to load PDF for image extraction page {}: {}", 
                    pageNumber, e.getMessage());
        }
        
        return pageImages;
    }
    
    /**
     * Gets a summary of page loading status for a specific category.
     * 
     * @param category the page category
     * @return summary with statistics
     */
    public PageRangeContentSummary getPageRangeSummary(PageCategory category) {
        long totalPages = pageContentRepository.countByCategory(category);
        long ingestedPages = pageContentRepository.countByCategoryAndIngestedTrue(category);
        
        List<PageContent> pagesWithErrors = pageContentRepository.findByErrorMessageIsNotNull();
        long errorCount = pagesWithErrors.stream()
                .filter(pc -> pc.getCategory() == category)
                .count();
        
        List<PageContent> categoryPages = pageContentRepository.findByCategory(category);
        int totalImages = categoryPages.stream()
                .mapToInt(pc -> (int) pageImageRepository.countByPageContentId(pc.getId()))
                .sum();
        
        return PageRangeContentSummary.builder()
                .category(category)
                .pageRange(category.getStartPage() + "-" + category.getEndPage())
                .totalPages((int) totalPages)
                .ingestedPages((int) ingestedPages)
                .errorCount((int) errorCount)
                .totalImages(totalImages)
                .build();
    }
}
