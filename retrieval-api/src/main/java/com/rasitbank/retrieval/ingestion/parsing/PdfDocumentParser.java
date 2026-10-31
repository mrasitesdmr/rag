package com.rasitbank.retrieval.ingestion.parsing;

import com.rasitbank.retrieval.exception.UnprocessableContentException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * PDF'i sayfa sayfa okur, tekrar eden üst/alt bilgiyi atar, satırları yer imlerine göre başlıklı kısımlara ayırır ve
 * soru-cevap kısmındaki tamamen kalın satırları soru olarak işaretler; açılamayan, metinsiz ya da yer imsiz PDF'i reddeder.
 */
@Component
public class PdfDocumentParser {

    private static final String FAQ_HEADING = "Sık sorulanlar";
    private static final List<String> FAQ_TITLES = List.of("Sık sorulanlar", "Sık sorulan sorular",
            "Sıkça sorulan sorular", "SSS", "FAQ", "Frequently asked questions");
    private static final float MIN_BOLD_FONT_WEIGHT = 600;
    private static final List<String> BOLD_FONT_NAME_MARKERS = List.of("bold", "semibold", "demi", "black", "heavy");
    private static final char SUBSET_PREFIX_END = '+';
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
            List<List<ExtractedLine>> rawPageLines = pageLines(document);
            List<List<ExtractedLine>> pageLines = pageMarginFilter.removeRepeatedMargins(rawPageLines,
                    ExtractedLine::text);
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

    private static List<List<ExtractedLine>> pageLines(PDDocument document) throws IOException {
        StyledLineStripper stripper = new StyledLineStripper();
        stripper.setSortByPosition(false);
        stripper.setLineSeparator(LINE_SEPARATOR);
        List<List<ExtractedLine>> pageLines = new ArrayList<>(document.getNumberOfPages());
        for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            pageLines.add(stripper.extractLines(document));
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

    private static List<ParsedSection> splitIntoSections(List<List<ExtractedLine>> pageLines,
            List<Bookmark> bookmarks) {
        Map<Integer, List<Bookmark>> bookmarksByPage = new HashMap<>();
        for (Bookmark bookmark : bookmarks) {
            bookmarksByPage.computeIfAbsent(bookmark.pageNumber(), page -> new ArrayList<>()).add(bookmark);
        }
        int firstContentPage = bookmarks.stream().mapToInt(Bookmark::pageNumber).min().orElseThrow();
        SectionCollector collector = new SectionCollector();
        for (int pageNumber = firstContentPage; pageNumber <= pageLines.size(); pageNumber++) {
            List<ExtractedLine> lines = pageLines.get(pageNumber - 1);
            List<Bookmark> pageBookmarks = bookmarksByPage.getOrDefault(pageNumber, List.of());
            List<Bookmark> unprinted = unprintedBookmarks(pageBookmarks, lines);
            unprinted.forEach(collector::startHeading);
            List<Bookmark> pendingHeadings = new ArrayList<>(pageBookmarks);
            unprinted.forEach(pendingHeadings::remove);
            for (ExtractedLine line : lines) {
                Bookmark heading = takeHeading(pendingHeadings, line.text());
                if (heading != null) {
                    collector.startHeading(heading);
                } else if (FAQ_HEADING.equals(line.text()) && collector.hasChapter()) {
                    collector.startFaq();
                } else {
                    collector.addLine(pageNumber, line);
                }
            }
        }
        return collector.sections();
    }

    private static List<Bookmark> unprintedBookmarks(List<Bookmark> pageBookmarks, List<ExtractedLine> lines) {
        List<Bookmark> pending = new ArrayList<>(pageBookmarks);
        for (ExtractedLine line : lines) {
            takeHeading(pending, line.text());
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

    private static boolean isFaqTitle(String title) {
        return FAQ_TITLES.stream().anyMatch(title::equalsIgnoreCase);
    }

    private static boolean isBoldFont(PDFont font) {
        PDFontDescriptor descriptor = font.getFontDescriptor();
        if (descriptor != null && (descriptor.getFontWeight() >= MIN_BOLD_FONT_WEIGHT || descriptor.isForceBold())) {
            return true;
        }
        String name = font.getName();
        if (name == null) {
            return false;
        }
        String baseName = name.substring(name.indexOf(SUBSET_PREFIX_END) + 1).toLowerCase(Locale.ROOT);
        return BOLD_FONT_NAME_MARKERS.stream().anyMatch(baseName::contains);
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

    private static int lineCount(List<List<ExtractedLine>> pageLines) {
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
        private boolean currentIsFaq;

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
            currentIsFaq = isFaqTitle(heading.title());
        }

        void startFaq() {
            flush();
            currentHeading = shortenHeading(currentChapter + FAQ_HEADING_SEPARATOR + FAQ_HEADING);
            currentIsFaq = true;
        }

        boolean hasChapter() {
            return currentChapter != null;
        }

        void addLine(int pageNumber, ExtractedLine line) {
            if (currentHeading != null) {
                currentLines.add(new PageLine(pageNumber, line.text(), currentIsFaq && line.isBold()));
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

    private static final class StyledLineStripper extends PDFTextStripper {

        private static final byte NO_STYLE = 0;
        private static final byte BOLD = 1;
        private static final byte REGULAR = 2;

        private final Map<PDFont, Boolean> boldByFont = new IdentityHashMap<>();
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder styles = new StringBuilder();
        private byte currentStyle = NO_STYLE;

        List<ExtractedLine> extractLines(PDDocument document) throws IOException {
            text.setLength(0);
            styles.setLength(0);
            writeText(document, new StyleRecordingWriter());
            return splitLines();
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            currentStyle = isBold(textPositions) ? BOLD : REGULAR;
            try {
                super.writeString(string, textPositions);
            } finally {
                currentStyle = NO_STYLE;
            }
        }

        private boolean isBold(List<TextPosition> textPositions) {
            for (TextPosition position : textPositions) {
                String unicode = position.getUnicode();
                if (unicode != null && !unicode.isBlank() && !isBoldPosition(position)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isBoldPosition(TextPosition position) {
            PDFont font = position.getFont();
            return font != null && boldByFont.computeIfAbsent(font, PdfDocumentParser::isBoldFont);
        }

        private List<ExtractedLine> splitLines() {
            List<ExtractedLine> lines = new ArrayList<>();
            int lineStart = 0;
            for (int index = 0; index <= text.length(); index++) {
                boolean isEnd = index == text.length();
                char current = isEnd ? '\n' : text.charAt(index);
                if (current == '\n' || current == '\r') {
                    addLine(lines, lineStart, index);
                    if (current == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                        index++;
                    }
                    lineStart = index + 1;
                }
            }
            return lines;
        }

        private void addLine(List<ExtractedLine> lines, int from, int to) {
            String line = text.substring(from, to).strip();
            if (line.isEmpty()) {
                return;
            }
            boolean hasBold = false;
            boolean hasRegular = false;
            for (int index = from; index < to; index++) {
                if (Character.isWhitespace(text.charAt(index))) {
                    continue;
                }
                byte style = (byte) styles.charAt(index);
                hasBold |= style == BOLD;
                hasRegular |= style != BOLD;
            }
            lines.add(new ExtractedLine(line, hasBold && !hasRegular));
        }

        private final class StyleRecordingWriter extends Writer {

            @Override
            public void write(char[] buffer, int offset, int length) {
                text.append(buffer, offset, length);
                for (int index = 0; index < length; index++) {
                    styles.append((char) currentStyle);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        }
    }
}
