package edu.minghualiu.oahspe.ingestion.loader;

import edu.minghualiu.oahspe.entities.PageContent;
import edu.minghualiu.oahspe.entities.PageImage;
import edu.minghualiu.oahspe.entities.PageRangeContentSummary;
import edu.minghualiu.oahspe.enums.PageCategory;
import edu.minghualiu.oahspe.ingestion.runner.IngestionContext;
import edu.minghualiu.oahspe.ingestion.runner.PDFExtractionException;
import edu.minghualiu.oahspe.ingestion.classifier.Phase1Classifier;
import edu.minghualiu.oahspe.ingestion.runner.PDFTextExtractor;
import edu.minghualiu.oahspe.ingestion.runner.ProgressCallback;
import edu.minghualiu.oahspe.ingestion.util.PdfPageUtil;
import edu.minghualiu.oahspe.records.PageClassificationResult;
import edu.minghualiu.oahspe.repositories.PageContentRepository;
import edu.minghualiu.oahspe.repositories.PageImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final Phase1Classifier phase1Classifier;

    private static final int BATCH_SIZE = 100;

    public IngestionContext loadAllPages(ProgressCallback callback) {
        log.info("Starting page loading from PDF: {}", PdfPageUtil.getPdfPath());

        int totalPages;
        try {
            totalPages = PdfPageUtil.getPageCount(pdfTextExtractor);
        } catch (PDFExtractionException e) {
            log.error("Failed to get page count: {}", e.getMessage());
            throw new RuntimeException("Cannot load pages: " + e.getMessage(), e);
        }

        IngestionContext context = new IngestionContext(PdfPageUtil.getPdfPath(), totalPages);
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

                            loadSinglePage(page);
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
    private PageContent loadSinglePage(int pageNumber) {
        log.debug("Loading page {}", pageNumber);

        return pageContentRepository.findByPageNumber(pageNumber)
                .orElseGet(() -> {

                    // ---- Extract raw text ----
                    String rawText;
                    try {
                        rawText = PdfPageUtil.loadPageText(pdfTextExtractor, pageNumber);
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
                    List<PageImage> images = PdfPageUtil.extractImagesFromPage(pageNumber, pageContent);
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

    /**
     * Classifies pages in the given range using Phase1Classifier.
     * Updates each PageContent with pageType and needsGeometry flags.
     *
     * @param startPageNumber inclusive start page number
     * @param endPageNumber inclusive end page number
     * @return count of pages successfully classified
     */
    public int classifyPageRange(int startPageNumber, int endPageNumber) {
        log.info("Starting classification for pages {}-{}", startPageNumber, endPageNumber);

        int classifiedCount = 0;

        for (int pageNum = startPageNumber; pageNum <= endPageNumber; pageNum++) {
            try {
                var pageContentOpt = pageContentRepository.findByPageNumber(pageNum);

                if (pageContentOpt.isPresent()) {
                    PageContent pageContent = pageContentOpt.get();

                    // Classify the page
                    PageClassificationResult result = phase1Classifier.classify(pageContent);

                    // Update the PageContent with classification results
                    pageContent.setPageType(result.pageType());
                    pageContent.setNeedsGeometry(result.needsGeometry());

                    // Save the updated PageContent
                    pageContentRepository.save(pageContent);

                    classifiedCount++;

                    if (pageNum % 50 == 0) {
                        log.debug("Classified page {}: {} (needs geometry: {})",
                                pageNum, result.pageType(), result.needsGeometry());
                    }
                } else {
                    log.warn("PageContent not found for page number {}", pageNum);
                }
            } catch (Exception e) {
                log.error("Failed to classify page {}: {}", pageNum, e.getMessage(), e);
            }
        }

        log.info("Classification complete. Pages classified: {}/{}",
                classifiedCount, (endPageNumber - startPageNumber + 1));

        return classifiedCount;
    }
}
