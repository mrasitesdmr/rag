package com.rasitbank.retrieval.ingestion.parsing;

import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.exception.UnprocessableContentException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * PDF'i sayfa sayfa okur, üst/alt bilgiyi atar ve numaralı yer imlerine göre satırları başlıklı kısımlara ayırır.
 */
@Component
public class PdfDocumentParser {

    private static final Pattern NUMBERED_HEADING = Pattern.compile("^\\d+(\\.\\d+)? .+");
    private static final String FAQ_HEADING = "Sık sorulanlar";
    private static final String FAQ_HEADING_SEPARATOR = " · ";
    private static final String LINE_SEPARATOR = "\n";

    private final Pattern pageHeaderPattern;
    private final Pattern pageFooterPattern;

    public PdfDocumentParser(IngestionProperties properties) {
        this.pageHeaderPattern = Pattern.compile(properties.pageHeaderPattern());
        this.pageFooterPattern = Pattern.compile(properties.pageFooterPattern());
    }

    public ParsedDocument parse(byte[] content) {
        try (PDDocument document = load(content)) {
            List<Bookmark> bookmarks = numberedBookmarks(document);
            List<Bookmark> chapters = bookmarks.stream().filter(Bookmark::isChapter).toList();
            if (chapters.isEmpty()) {
                throw noSections();
            }
            int firstContentPage = chapters.stream().mapToInt(Bookmark::pageNumber).min().orElseThrow();
            List<ParsedSection> sections = splitIntoSections(document, firstContentPage, bookmarks);
            if (sections.isEmpty()) {
                throw noSections();
            }
            return new ParsedDocument(document.getNumberOfPages(), chapters.size(), sections);
        } catch (IOException exception) {
            throw unreadable();
        }
    }

    private static PDDocument load(byte[] content) {
        PDDocument document;
        try {
            document = Loader.loadPDF(content);
        } catch (IOException exception) {
            throw unreadable();
        }
        if (document.isEncrypted()) {
            closeQuietly(document);
            throw unreadable();
        }
        return document;
    }

    private static List<Bookmark> numberedBookmarks(PDDocument document) throws IOException {
        List<Bookmark> bookmarks = new ArrayList<>();
        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        if (outline != null) {
            collectBookmarks(document, outline, bookmarks);
        }
        return bookmarks;
    }

    private static void collectBookmarks(PDDocument document, PDOutlineNode node, List<Bookmark> bookmarks)
            throws IOException {
        for (PDOutlineItem item : node.children()) {
            String title = item.getTitle() == null ? "" : item.getTitle().strip();
            PDPage page = item.findDestinationPage(document);
            if (page != null && NUMBERED_HEADING.matcher(title).matches()) {
                bookmarks.add(new Bookmark(title, document.getPages().indexOf(page) + 1));
            }
            collectBookmarks(document, item, bookmarks);
        }
    }

    private List<ParsedSection> splitIntoSections(PDDocument document, int firstContentPage, List<Bookmark> bookmarks)
            throws IOException {
        Map<Integer, List<Bookmark>> pendingHeadingsByPage = new HashMap<>();
        for (Bookmark bookmark : bookmarks) {
            pendingHeadingsByPage.computeIfAbsent(bookmark.pageNumber(), page -> new ArrayList<>()).add(bookmark);
        }
        SectionCollector collector = new SectionCollector();
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(false);
        stripper.setLineSeparator(LINE_SEPARATOR);
        for (int pageNumber = firstContentPage; pageNumber <= document.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            List<Bookmark> pendingHeadings = pendingHeadingsByPage.getOrDefault(pageNumber, new ArrayList<>());
            for (String line : bodyLines(stripper.getText(document))) {
                Bookmark heading = takeHeading(pendingHeadings, line);
                if (heading != null) {
                    collector.startHeading(heading);
                } else if (FAQ_HEADING.equals(line) && collector.hasChapter()) {
                    collector.startFaq();
                } else {
                    collector.addLine(new PageLine(pageNumber, line));
                }
            }
        }
        return collector.sections();
    }

    private List<String> bodyLines(String pageText) {
        List<String> lines = new ArrayList<>(pageText.lines().map(String::strip).filter(line -> !line.isEmpty())
                .toList());
        if (!lines.isEmpty() && pageHeaderPattern.matcher(lines.getFirst()).matches()) {
            lines.removeFirst();
        }
        if (!lines.isEmpty() && pageFooterPattern.matcher(lines.getLast()).matches()) {
            lines.removeLast();
        }
        return lines;
    }

    private static @Nullable Bookmark takeHeading(List<Bookmark> pendingHeadings, String line) {
        for (int index = 0; index < pendingHeadings.size(); index++) {
            if (pendingHeadings.get(index).title().equals(line)) {
                return pendingHeadings.remove(index);
            }
        }
        return null;
    }

    private static void closeQuietly(PDDocument document) {
        try {
            document.close();
        } catch (IOException ignored) {
            // Closing an already rejected document has nothing left to report.
        }
    }

    private static UnprocessableContentException unreadable() {
        return new UnprocessableContentException("PDF_UNREADABLE", "The file cannot be read as a PDF");
    }

    private static UnprocessableContentException noSections() {
        return new UnprocessableContentException("PDF_NO_SECTIONS",
                "The PDF has no numbered chapter bookmarks or no text under them");
    }

    private record Bookmark(String title, int pageNumber) {

        boolean isChapter() {
            return !title.substring(0, title.indexOf(' ')).contains(".");
        }
    }

    private static final class SectionCollector {

        private final List<ParsedSection> sections = new ArrayList<>();
        private final List<PageLine> currentLines = new ArrayList<>();
        private @Nullable String currentHeading;
        private @Nullable String currentChapter;

        void startHeading(Bookmark heading) {
            flush();
            if (heading.isChapter()) {
                currentChapter = heading.title();
            }
            currentHeading = heading.title();
        }

        void startFaq() {
            flush();
            currentHeading = currentChapter + FAQ_HEADING_SEPARATOR + FAQ_HEADING;
        }

        boolean hasChapter() {
            return currentChapter != null;
        }

        void addLine(PageLine line) {
            if (currentHeading != null) {
                currentLines.add(line);
            }
        }

        List<ParsedSection> sections() {
            flush();
            return sections;
        }

        private void flush() {
            if (currentHeading != null && !currentLines.isEmpty()) {
                sections.add(new ParsedSection(currentHeading, currentLines));
            }
            currentLines.clear();
        }
    }
}
