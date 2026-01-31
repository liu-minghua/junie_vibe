package edu.minghualiu.oahspe.ingestion.runner;

import edu.minghualiu.oahspe.entities.Image;
import edu.minghualiu.oahspe.repositories.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF image extraction component using Apache PDFBox.
 * Extracts embedded images from PDF pages and saves them to the database.
 *
 * This component handles:
 * - Extracting embedded images from PDF pages using PDFBox
 * - Converting images to byte arrays with proper format detection
 * - Generating unique, deterministic image keys for restart-safe ingestion
 * - Idempotent persistence (no duplicates on re-run)
 *
 * Image Key Format: "IMG{page}_{objectName}"
 * Example: "IMG42_Im1" for first image object on page 42
 *
 * @see PDFTextExtractor
 * @see Image
 * @see ImageRepository
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PDFImageExtractor {

    private final ImageRepository imageRepository;

    /**
     * Extracts all images from a specific page of a PDF file.
     * Images are saved to the database with unique keys for restart safety.
     *
     * Uses idempotent save strategy: if an image with the same key exists,
     * it returns the existing image instead of creating a duplicate.
     *
     * @param pdfFilePath the path to the PDF file
     * @param pageNumber the page number to extract images from (1-indexed)
     * @return list of extracted and persisted Image entities
     * @throws PDFExtractionException if file not found, invalid PDF, or extraction fails
     */
    public List<Image> extractImagesFromPage(String pdfFilePath, int pageNumber)
            throws PDFExtractionException {

        File file = new File(pdfFilePath);
        if (!file.exists()) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    String.format("File not found: %s", pdfFilePath)
            );
        }

        List<Image> extractedImages = new ArrayList<>();

        try (PDDocument document = PDDocument.load(file)) {
            if (pageNumber < 1 || pageNumber > document.getNumberOfPages()) {
                throw new PDFExtractionException(
                        pdfFilePath,
                        pageNumber,
                        String.format("Page number out of range. Document has %d pages.",
                                document.getNumberOfPages())
                );
            }

            PDPage page = document.getPage(pageNumber - 1); // PDFBox uses 0-indexed
            PDResources resources = page.getResources();

            if (resources == null) {
                log.debug("Page {} has no resources", pageNumber);
                return extractedImages;
            }

            // Iterate through all XObjects (potential images) on the page
            for (COSName name : resources.getXObjectNames()) {
                try {
                    PDXObject xobject = resources.getXObject(name);

                    if (xobject instanceof PDImageXObject imageXObject) {
                        Image image = extractAndSaveImage(imageXObject, name.getName(), pageNumber);
                        if (image != null) {
                            extractedImages.add(image);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract image {} from page {}: {}",
                            name.getName(), pageNumber, e.getMessage());
                    // Continue processing other images on the page
                }
            }

            if (!extractedImages.isEmpty()) {
                log.info("Extracted {} images from page {}", extractedImages.size(), pageNumber);
            } else {
                log.debug("No images found on page {}", pageNumber);
            }

        } catch (PDFExtractionException e) {
            throw e;
        } catch (IOException e) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    pageNumber,
                    "Failed to extract images from page",
                    e
            );
        } catch (Exception e) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    pageNumber,
                    "Unexpected error during PDF image extraction",
                    e
            );
        }

        return extractedImages;
    }

    /**
     * Extracts a single image from a PDImageXObject and persists it.
     * Uses idempotent save strategy to prevent duplicates on restart.
     *
     * @param imageXObject the PDFBox image object
     * @param objectName the PDF object name (e.g., "Im0", "Image1")
     * @param pageNumber the page number (for key generation and tracking)
     * @return the persisted Image entity, or null if extraction failed
     */
    private Image extractAndSaveImage(PDImageXObject imageXObject, String objectName,
                                      int pageNumber) {
        try {
            // Generate unique, deterministic key
            String imageKey = generateImageKey(pageNumber, objectName);

            // Check if image already exists (restart safety / idempotent)
            return imageRepository.findByImageKey(imageKey)
                    .orElseGet(() -> {
                        try {
                            // Extract image data
                            byte[] imageData = extractImageBytes(imageXObject);
                            String format = imageXObject.getSuffix();
                            if (format == null || format.isEmpty()) {
                                format = "png"; // Default format
                            }
                            String contentType = "image/" + format.toLowerCase();

                            // Create new image entity
                            Image image = Image.builder()
                                    .imageKey(imageKey)
                                    .title("Image " + imageKey)
                                    .description("Extracted from page " + pageNumber)
                                    .sourcePage(pageNumber)
                                    .originalFilename(objectName + "." + format)
                                    .contentType(contentType)
                                    .data(imageData)
                                    .build();

                            Image saved = imageRepository.save(image);
                            log.debug("Saved new image: {} (page {}, {} bytes, {})",
                                    imageKey, pageNumber, imageData.length, contentType);
                            return saved;

                        } catch (IOException e) {
                            log.error("Failed to extract image data for {}: {}",
                                    imageKey, e.getMessage());
                            return null;
                        }
                    });

        } catch (Exception e) {
            log.error("Error processing image {} on page {}: {}",
                    objectName, pageNumber, e.getMessage());
            return null;
        }
    }

    /**
     * Extracts image bytes from a PDImageXObject.
     * Renders the image to a BufferedImage and converts to byte array.
     *
     * @param imageXObject the PDFBox image object
     * @return byte array of the image data
     * @throws IOException if image extraction fails
     */
    private byte[] extractImageBytes(PDImageXObject imageXObject) throws IOException {
        BufferedImage bufferedImage = imageXObject.getImage();
        String format = imageXObject.getSuffix();
        if (format == null || format.isEmpty()) {
            format = "png";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, format, baos);
        return baos.toByteArray();
    }

    /**
     * Generates a unique, deterministic image key based on page number and object name.
     * Format: "IMG{page}_{objectName}"
     *
     * This ensures:
     * - No key collisions between images
     * - Restart-safe ingestion (same PDF produces same keys)
     * - Traceability to source page
     *
     * @param pageNumber the page number (1-indexed)
     * @param objectName the PDF object name
     * @return unique image key (e.g., "IMG42_Im0")
     */
    public String generateImageKey(int pageNumber, String objectName) {
        return "IMG" + pageNumber + "_" + objectName;
    }

    /**
     * Extracts all images from all pages of a PDF file.
     * Useful for bulk extraction operations.
     *
     * @param pdfFilePath the path to the PDF file
     * @return list of all extracted images from all pages
     * @throws PDFExtractionException if file not found or extraction fails
     */
    public List<Image> extractAllImages(String pdfFilePath) throws PDFExtractionException {
        List<Image> allImages = new ArrayList<>();

        File file = new File(pdfFilePath);
        if (!file.exists()) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    String.format("File not found: %s", pdfFilePath)
            );
        }

        try (PDDocument document = PDDocument.load(file)) {
            int pageCount = document.getNumberOfPages();
            log.info("Starting image extraction from {} pages", pageCount);

            for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                try {
                    List<Image> pageImages = extractImagesFromPage(pdfFilePath, pageNum);
                    allImages.addAll(pageImages);
                } catch (PDFExtractionException e) {
                    log.warn("Failed to extract images from page {}: {}",
                            pageNum, e.getMessage());
                    // Continue processing other pages
                }
            }

            log.info("Extracted {} total images from {} pages",
                    allImages.size(), pageCount);

        } catch (IOException e) {
            throw new PDFExtractionException(
                    pdfFilePath,
                    "Failed to load PDF document for image extraction",
                    e
            );
        }

        return allImages;
    }
}
