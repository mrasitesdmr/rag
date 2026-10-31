package com.rasitbank.retrieval.fixture;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * Testte PDFBox ile yer imli, sayfa başına satırlı küçük PDF'ler üretir.
 */
public final class PdfFixtures {

    private PdfFixtures() {
    }

    public record PageSpec(List<String> lines, String bookmark) {
        public static PageSpec of(String bookmark, String... lines) {
            return new PageSpec(List.of(lines), bookmark);
        }
    }

    public static byte[] pdf(boolean withBookmarks, PageSpec... pages) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDDocumentOutline outline = new PDDocumentOutline();
            document.getDocumentCatalog().setDocumentOutline(outline);
            var font = PDType0Font.load(document, PdfFixtures.class.getResourceAsStream("/fonts/LiberationSans-Regular.ttf"));
            var boldFont = loadSemibold(document, pages);
            PDOutlineItem lastTopLevel = null;
            for (PageSpec spec : pages) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(font, 11);
                    stream.newLineAtOffset(40, 780);
                    for (String line : spec.lines()) {
                        showLine(stream, font, boldFont, line);
                        stream.newLineAtOffset(0, -16);
                    }
                    stream.endText();
                }
                if (withBookmarks && spec.bookmark() != null) {
                    for (String title : spec.bookmark().split("\\|", -1)) {
                        boolean isChild = title.startsWith(">");
                        PDOutlineItem item = new PDOutlineItem();
                        item.setTitle(isChild ? title.substring(1) : title);
                        PDPageFitDestination destination = new PDPageFitDestination();
                        destination.setPage(page);
                        item.setDestination(destination);
                        if (isChild && lastTopLevel != null) {
                            lastTopLevel.addLast(item);
                        } else {
                            outline.addLast(item);
                            lastTopLevel = item;
                        }
                    }
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Satırın tamamı kalın yazı tipiyle basılsın diye satırın başına konur. */
    public static final String BOLD = "{b}";
    /** Satırın yalnız başı kalın olsun diye kalın kısmı kapatır; ör. {@code {b}Kur alarmı:{/b} düz metin}. */
    public static final String BOLD_END = "{/b}";

    private static final Path SEMIBOLD_FONT = Path.of("../docs/rehber/kaynak/fontlar/SourceSans3-Semibold.ttf");

    private static PDType0Font loadSemibold(PDDocument document, PageSpec... pages) throws IOException {
        boolean isNeeded = java.util.Arrays.stream(pages).flatMap(page -> page.lines().stream())
                .anyMatch(line -> line.startsWith(BOLD));
        if (!isNeeded) {
            return null;
        }
        try (InputStream input = Files.newInputStream(SEMIBOLD_FONT)) {
            return PDType0Font.load(document, input);
        }
    }

    private static void showLine(PDPageContentStream stream, PDType0Font font, PDType0Font boldFont, String line)
            throws IOException {
        if (!line.startsWith(BOLD)) {
            stream.showText(line);
            return;
        }
        String rest = line.substring(BOLD.length());
        int end = rest.indexOf(BOLD_END);
        stream.setFont(boldFont, 11);
        stream.showText(end < 0 ? rest : rest.substring(0, end));
        if (end >= 0) {
            stream.setFont(font, 11);
            stream.showText(rest.substring(end + BOLD_END.length()));
        }
        stream.setFont(font, 11);
    }

    private static final byte[] VALID_PDF = pdf(true,
            PageSpec.of("Giriş", "Giriş", "Gövde satırı bir.", "Gövde satırı iki."));

    // PDFBox her kayıtta rastgele belge kimliği yazar; aynı baytların karşılaştırılabilmesi için tek örnek döner.
    public static byte[] validPdf() {
        return VALID_PDF.clone();
    }

    public static byte[] imageOnlyPdf(boolean withBookmark) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.drawImage(pdfImage, 40, 600, 200, 200);
            }
            if (withBookmark) {
                PDDocumentOutline outline = new PDDocumentOutline();
                document.getDocumentCatalog().setDocumentOutline(outline);
                PDOutlineItem item = new PDOutlineItem();
                item.setTitle("Taranmış bölüm");
                PDPageFitDestination destination = new PDPageFitDestination();
                destination.setPage(page);
                item.setDestination(destination);
                outline.addLast(item);
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static byte[] encryptedPdf() {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "user", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static byte[] corruptPdf() {
        return ("%PDF-1.7\n" + "bu dosya bir pdf degil ".repeat(20)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
