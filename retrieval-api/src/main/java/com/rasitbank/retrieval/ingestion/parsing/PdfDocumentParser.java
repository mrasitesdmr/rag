package com.rasitbank.retrieval.ingestion.parsing;

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
 * PDF'i sayfa sayfa okur, tekrar eden üst/alt bilgiyi atar ve satırları yer imlerine göre başlıklı kısımlara ayırır;
 * açılamayan, metinsiz ya da yer imsiz PDF'i reddeder.
 */
@Component
public class PdfDocumentParser {

    private static final String FAQ_HEADING = "Sık sorulanlar";
    private static final String FAQ_HEADING_SEPARATOR = " · ";
    private static final String LINE_SEPARATOR = "\n";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int MAX_HEADING_LENGTH = 200;
    private static final String ELLIPSIS = "…";

    private final PageMarginFilter pageMarginFilter;

    public PdfDocumentParser(PageMarginFilter pageMarginFilter) {
        this.pageMarginFilter = pageMarginFilter;
    }

    public ParsedDocument parse(byte[] content) {
        try (PDDocument document = load(content)) {
            List<List<String>> rawPageLines = pageLines(document);
            List<List<String>> pageLines = pageMarginFilter.removeRepeatedMargins(rawPageLines);
            if (pageLines.stream().allMatch(List::isEmpty)) {
                throw noText();
            }
            List<Bookmark> bookmarks = bookmarks(document);
            if (bookmarks.isEmpty()) {
                throw noBookmarks();
            }
            List<ParsedSection> sections = splitIntoSections(pageLines, bookmarks);
            if (sections.isEmpty()) {
                throw noBookmarks();
            }
            int chapterCount = (int) bookmarks.stream().filter(Bookmark::isChapter).count();
            return new ParsedDocument(document.getNumberOfPages(), chapterCount, sections,
                    lineCount(rawPageLines) - lineCount(pageLines));
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

    private static List<List<String>> pageLines(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(false);
        stripper.setLineSeparator(LINE_SEPARATOR);
        List<List<String>> pageLines = new ArrayList<>(document.getNumberOfPages());
        for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            pageLines.add(stripper.getText(document).lines().map(String::strip).filter(line -> !line.isEmpty())
                    .toList());
        }
        return pageLines;
    }

    private static List<Bookmark> bookmarks(PDDocument document) throws IOException {
        List<Bookmark> bookmarks = new ArrayList<>();
        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        if (outline != null) {
            collectBookmarks(document, outline, true, bookmarks);
        }
        return bookmarks;
    }

    private static void collectBookmarks(PDDocument document, PDOutlineNode node, boolean isChapterLevel,
            List<Bookmark> bookmarks) throws IOException {
        for (PDOutlineItem item : node.children()) {
            String title = item.getTitle() == null ? "" : normalize(item.getTitle());
            PDPage page = item.findDestinationPage(document);
            int pageIndex = page == null ? -1 : document.getPages().indexOf(page);
            boolean isUsable = !title.isEmpty() && pageIndex >= 0;
            if (isUsable) {
                bookmarks.add(new Bookmark(title, pageIndex + 1, isChapterLevel));
            }
            collectBookmarks(document, item, isChapterLevel && !isUsable, bookmarks);
        }
    }

    private static List<ParsedSection> splitIntoSections(List<List<String>> pageLines, List<Bookmark> bookmarks) {
        Map<Integer, List<Bookmark>> bookmarksByPage = new HashMap<>();
        for (Bookmark bookmark : bookmarks) {
            bookmarksByPage.computeIfAbsent(bookmark.pageNumber(), page -> new ArrayList<>()).add(bookmark);
        }
        int firstContentPage = bookmarks.stream().mapToInt(Bookmark::pageNumber).min().orElseThrow();
        SectionCollector collector = new SectionCollector();
        for (int pageNumber = firstContentPage; pageNumber <= pageLines.size(); pageNumber++) {
            List<String> lines = pageLines.get(pageNumber - 1);
            List<Bookmark> pageBookmarks = bookmarksByPage.getOrDefault(pageNumber, List.of());
            List<Bookmark> unprinted = unprintedBookmarks(pageBookmarks, lines);
            unprinted.forEach(collector::startHeading);
            List<Bookmark> pendingHeadings = new ArrayList<>(pageBookmarks);
            unprinted.forEach(pendingHeadings::remove);
            for (String line : lines) {
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

    private static List<Bookmark> unprintedBookmarks(List<Bookmark> pageBookmarks, List<String> lines) {
        List<Bookmark> pending = new ArrayList<>(pageBookmarks);
        for (String line : lines) {
            takeHeading(pending, line);
        }
        return pending;
    }

    private static @Nullable Bookmark takeHeading(List<Bookmark> pendingHeadings, String line) {
        String normalizedLine = normalize(line);
        for (int index = 0; index < pendingHeadings.size(); index++) {
            if (pendingHeadings.get(index).title().equals(normalizedLine)) {
                return pendingHeadings.remove(index);
            }
        }
        return null;
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text.strip()).replaceAll(" ");
    }

    private static String shortenHeading(String heading) {
        if (heading.codePointCount(0, heading.length()) <= MAX_HEADING_LENGTH) {
            return heading;
        }
        String head = heading.substring(0, heading.offsetByCodePoints(0, MAX_HEADING_LENGTH - 1));
        int lastSpace = head.lastIndexOf(' ');
        String cut = lastSpace > 0 ? head.substring(0, lastSpace).strip() : head;
        return cut + ELLIPSIS;
    }

    private static int lineCount(List<List<String>> pageLines) {
        return pageLines.stream().mapToInt(List::size).sum();
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

    private static UnprocessableContentException noText() {
        return new UnprocessableContentException("PDF_NO_TEXT", "The PDF has no extractable text");
    }

    private static UnprocessableContentException noBookmarks() {
        return new UnprocessableContentException("PDF_NO_BOOKMARKS",
                "The PDF has no usable bookmarks or no text under them");
    }

    private record Bookmark(String title, int pageNumber, boolean isChapter) {
    }

    private static final class SectionCollector {

        private final List<ParsedSection> sections = new ArrayList<>();
        private final List<PageLine> currentLines = new ArrayList<>();
        private @Nullable String currentHeading;
        private @Nullable String currentChapter;

        void startHeading(Bookmark heading) {
            if (!heading.isChapter() && FAQ_HEADING.equals(heading.title()) && hasChapter()) {
                startFaq();
                return;
            }
            flush();
            String shortened = shortenHeading(heading.title());
            if (heading.isChapter()) {
                currentChapter = shortened;
            }
            currentHeading = shortened;
        }

        void startFaq() {
            flush();
            currentHeading = shortenHeading(currentChapter + FAQ_HEADING_SEPARATOR + FAQ_HEADING);
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
