package edu.minghualiu.oahspe.ingestion.util;

import edu.minghualiu.oahspe.entities.PageContent;
import edu.minghualiu.oahspe.entities.PageImage;
import edu.minghualiu.oahspe.ingestion.runner.PDFExtractionException;
import edu.minghualiu.oahspe.ingestion.runner.PDFTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class PdfPageUtil {

    private PdfPageUtil() {
    }

    public static final String PDF_PATH = "data/OAHSPE_Standard_Edition.pdf";

    public static String getPdfPath() {
        return PDF_PATH;
    }

    public static int getPageCount(PDFTextExtractor pdfTextExtractor) throws PDFExtractionException {
        return pdfTextExtractor.getPageCount(PDF_PATH);
    }

    public static String loadPageText(PDFTextExtractor pdfTextExtractor, int pageNumber)
            throws PDFExtractionException {
        return pdfTextExtractor.extractText(PDF_PATH, pageNumber);
    }

    public static List<PageImage> extractImagesFromPage(int pageNumber, PageContent pageContent) {
        List<PageImage> pageImages = new ArrayList<>();

        File file = new File(PDF_PATH);
        if (!file.exists()) {
            log.warn("PDF file not found for image extraction: {}", PDF_PATH);
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
}
