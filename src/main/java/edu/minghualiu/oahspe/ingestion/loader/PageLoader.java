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

    private static final int BATCH_SIZE = 100;

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

            if (batch.size() >= BATCH_SIZE || pageNum == totalPages) {
                final List<Integer> pagesToLoad = new ArrayList<>(batch);

                transactionTemplate.executeWithoutResult(status -> {
                    for (Integer page : pagesToLoad) {
                        try {
                            if (callback != null && page % 50 == 0) {
                                callback.onPageStart(page, totalPages);
                            }

                            loadSinglePage(pdfPath, page);
                            context.incrementEventsProcessed();

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
     */
    private PageContent loadSinglePage(String pdfPath, int pageNumber) {
        log.debug("Loading page {}", pageNumber);

        return pageContentRepository.findByPageNumber(pageNumber)
                .orElseGet(() -> {

                    // ---- Extract raw text ----
                    String rawText;
                    try {
                        rawText = pdfTextExtractor.extractText(pdfPath, pageNumber);
                    } catch (PDFExtractionException e) {
                        log.error("Text extraction failed for page {}: {}", pageNumber, e.getMessage());
                        rawText = "";
                    }

                    PageCategory category = PageCategory.fromPageNumber(pageNumber);

                    // ---- Cheap metadata extraction ----
                    int textLength = rawText != null ? rawText.length() : 0;
                    int lineCount = rawText != null && !rawText.isEmpty()
                            ? rawText.split("\n").length
                            : 0;

                    int verseCount = 0;
                    if (rawText != null) {
                        var matcher = PDFTextExtractor.VERSE_PATTERN.matcher(rawText);
                        while (matcher.find()) verseCount++;
                    }

                    boolean hasFootnoteMarkers =
                            rawText.contains("—") ||
                            rawText.matches("(?m)^\\d{1,2}$");

                    boolean hasIllustrationKeywords =
                            rawText.contains("Plate") ||
                            rawText.contains("Fig") ||
                            rawText.contains("Figure") ||
                            rawText.contains("Illustration") ||
                            rawText.contains("Tablet of") ||
                            rawText.contains("Explanation of Plate");

                    boolean hasSaphahKeywords =
                            rawText.contains("Se'moin") ||
                            rawText.contains("Saphah") ||
                            rawText.contains("Glyph") ||
                            rawText.contains("Tablet");

                    // ---- Build PageContent ----
                    PageContent pageContent = PageContent.builder()
                            .pageNumber(pageNumber)
                            .category(category)
                            .rawText(rawText)
                            .extractedAt(LocalDateTime.now())
                            .ingested(false)
                            .textLength(textLength)
                            .lineCount(lineCount)
                            .verseCount(verseCount)
                            .hasFootnoteMarkers(hasFootnoteMarkers)
                            .hasIllustrationKeywords(hasIllustrationKeywords)
                            .hasSaphahKeywords(hasSaphahKeywords)
                            .containsImages(false) // updated after extraction
                            .build();

                    pageContent = pageContentRepository.save(pageContent);

                    // ---- Extract images ----
                    List<PageImage> images = extractImagesFromPage(pdfPath, pageNumber, pageContent);
                    pageImageRepository.saveAll(images);

                    // Update containsImages
                    if (!images.isEmpty()) {
                        pageContent.setContainsImages(true);
                        pageContentRepository.save(pageContent);
                    }

                    log.debug("Loaded page {} [{}] - {} chars, {} images",
                            pageNumber, category, rawText.length(), images.size());

                    return pageContent;
                });
    }

    /**
     * Extracts images from a PDF page.
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

            PDPage page = document.getPage(pageNumber - 1);
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
