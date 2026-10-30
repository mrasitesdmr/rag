package com.rasitbank.retrieval.ingestion.parsing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * Testte PDFBox ile yer imli, sayfa başına satırlı küçük PDF'ler üretir.
 */
final class PdfFixtures {

    private PdfFixtures() {
    }

    record PageSpec(List<String> lines, String bookmark) {
        static PageSpec of(String bookmark, String... lines) {
            return new PageSpec(List.of(lines), bookmark);
        }
    }

    static byte[] pdf(boolean withBookmarks, PageSpec... pages) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDDocumentOutline outline = new PDDocumentOutline();
            document.getDocumentCatalog().setDocumentOutline(outline);
            var font = PDType0Font.load(document, PdfFixtures.class.getResourceAsStream("/fonts/LiberationSans-Regular.ttf"));
            List<PDPage> created = new ArrayList<>();
            for (PageSpec spec : pages) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                created.add(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(font, 11);
                    stream.newLineAtOffset(40, 780);
                    for (String line : spec.lines()) {
                        stream.showText(line);
                        stream.newLineAtOffset(0, -16);
                    }
                    stream.endText();
                }
                if (withBookmarks && spec.bookmark() != null) {
                    for (String title : spec.bookmark().split("\\|")) {
                        PDOutlineItem item = new PDOutlineItem();
                        item.setTitle(title);
                        PDPageFitDestination destination = new PDPageFitDestination();
                        destination.setPage(page);
                        item.setDestination(destination);
                        outline.addLast(item);
                    }
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
