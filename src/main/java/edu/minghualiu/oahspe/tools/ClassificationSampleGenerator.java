package edu.minghualiu.oahspe.tools;

import edu.minghualiu.oahspe.entities.PageContent;
import edu.minghualiu.oahspe.enums.PageType;
import edu.minghualiu.oahspe.ingestion.classifier.Phase1Classifier;
import edu.minghualiu.oahspe.records.PageClassificationResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ClassificationSampleGenerator {

    private static final String DB_URL = "jdbc:h2:file:F:/junie_vibe/oahspe/data/oahspe-db;AUTO_SERVER=TRUE";
    private static final String DB_USER = "oahspe";
    private static final String DB_PASSWORD = "oahspe";
    private static final String PDF_PATH = "F:/junie_vibe/oahspe/data/OAHSPE_Standard_Edition.pdf";
    private static final String OUTPUT_ROOT = "F:/junie_vibe/oahspe/data/classification-samples/page-types";

    private static final int START_PAGE = 1;
    private static final int END_PAGE = 1831;
    private static final int RENDER_DPI = 150;

        private static final String[] GLYPH_KEYWORDS = {
            "glyph",
            "tablet",
            "saphah",
            "se'moin"
        };

    public static void main(String[] args) throws Exception {
        System.out.println("Checking access to H2 DB and PDF...");
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to H2 DB.");
        }

        try (PDDocument document = PDDocument.load(Path.of(PDF_PATH).toFile())) {
            System.out.println("Opened PDF. Total pages: " + document.getNumberOfPages());
        }

        System.out.println("Running classification and sample export...");
        generateSamples();
    }

    private static void generateSamples() throws SQLException, IOException {
        Phase1Classifier classifier = new Phase1Classifier();
        Random random = new Random();

        Map<PageType, List<PageRow>> byType = new EnumMap<>(PageType.class);
        for (PageType type : PageType.values()) {
            byType.put(type, new ArrayList<>());
        }

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            loadCandidates(connection, classifier, byType);
        }

        try (PDDocument document = PDDocument.load(Path.of(PDF_PATH).toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (PageType type : PageType.values()) {
                List<PageRow> candidates = byType.get(type);
                if (candidates == null || candidates.isEmpty()) {
                    System.out.println("No candidates found for PageType " + type);
                    continue;
                }

                PageRow row = candidates.get(random.nextInt(candidates.size()));
                writeSample(type, row, renderer);
            }
        }
    }

    private static void loadCandidates(Connection connection, Phase1Classifier classifier,
            Map<PageType, List<PageRow>> byType) throws SQLException {
        String sql = "SELECT page_number, raw_text, contains_images "
                + "FROM page_contents "
                + "WHERE page_number BETWEEN ? AND ? "
                + "AND raw_text IS NOT NULL";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, START_PAGE);
            ps.setInt(2, END_PAGE);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int pageNumber = rs.getInt("page_number");
                    String rawText = rs.getString("raw_text");
                    Boolean containsImages = rs.getObject("contains_images", Boolean.class);

                    PageContent pc = new PageContent();
                    pc.setRawText(rawText);
                    pc.setContainsImages(containsImages != null ? containsImages : Boolean.FALSE);

                    PageClassificationResult result = classifier.classify(pc);
                    PageType type = result.pageType();

                    if (type != PageType.GLYPH_TABLET && isGlyphTabletCandidate(rawText)) {
                        byType.get(PageType.GLYPH_TABLET).add(new PageRow(pageNumber, rawText));
                    } else {
                        byType.get(type).add(new PageRow(pageNumber, rawText));
                    }
                }
            }
        }
    }

    private static boolean isGlyphTabletCandidate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return false;
        }

        String lower = rawText.toLowerCase(Locale.ROOT);
        for (String keyword : GLYPH_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private static void writeSample(PageType type, PageRow row, PDFRenderer renderer) throws IOException {
        String folderName = type.name().toLowerCase(Locale.ROOT).replace("_", "-");
        Path outputDir = Path.of(OUTPUT_ROOT, folderName);
        Files.createDirectories(outputDir);

        String fileBase = String.format("page-%04d", row.pageNumber());
        Path textPath = outputDir.resolve(fileBase + ".txt");
        Path imagePath = outputDir.resolve(fileBase + ".png");

        Files.writeString(textPath, row.rawText(), StandardCharsets.UTF_8);

        BufferedImage image = renderer.renderImageWithDPI(row.pageNumber() - 1, RENDER_DPI);
        ImageIO.write(image, "PNG", imagePath.toFile());

        System.out.println("Wrote sample for " + type + " -> " + textPath.getFileName());
    }

    private record PageRow(int pageNumber, String rawText) {
    }
}
