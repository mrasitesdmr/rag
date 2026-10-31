package com.rasitbank.retrieval.ingestion.parsing;

import com.rasitbank.retrieval.config.IngestionProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Sayfaların çoğunda tekrar eden ilk ve son satırları (sayfa üst ve alt bilgisi) belgenin biçiminden bağımsız olarak
 * ayıklar; her kenarda en fazla ayarlı sayıda satır atar.
 */
@Component
public class PageMarginFilter {

    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String DIGITS_PLACEHOLDER = "#";

    private final int minPages;
    private final double minShare;
    private final int maxPerEdge;

    public PageMarginFilter(IngestionProperties properties) {
        this.minPages = properties.repeatedLineMinPages();
        this.minShare = properties.repeatedLineMinShare();
        this.maxPerEdge = properties.repeatedLineMaxPerEdge();
    }

    public List<List<String>> removeRepeatedMargins(List<List<String>> pageLines) {
        return removeRepeatedMargins(pageLines, Function.identity());
    }

    public <T> List<List<T>> removeRepeatedMargins(List<List<T>> pageLines, Function<T, String> textOf) {
        List<PageBounds<T>> pages = pageLines.stream().map(lines -> new PageBounds<>(lines, textOf)).toList();
        long textPageCount = pages.stream().filter(PageBounds::hasLines).count();
        if (textPageCount >= minPages) {
            double threshold = minShare * textPageCount;
            removeEdge(pages, threshold, true);
            removeEdge(pages, threshold, false);
        }
        return pages.stream().map(PageBounds::remainingLines).toList();
    }

    public static String marginKey(String line) {
        String withoutDigits = DIGITS.matcher(line.strip()).replaceAll(DIGITS_PLACEHOLDER);
        return WHITESPACE.matcher(withoutDigits).replaceAll(" ");
    }

    private <T> void removeEdge(List<PageBounds<T>> pages, double threshold, boolean isTop) {
        for (int round = 0; round < maxPerEdge; round++) {
            Set<String> repeatedKeys = repeatedKeys(pages, threshold, isTop);
            int removed = 0;
            for (PageBounds<T> page : pages) {
                if (page.hasLines() && repeatedKeys.contains(marginKey(page.edgeLine(isTop)))) {
                    page.dropEdgeLine(isTop);
                    removed++;
                }
            }
            if (removed == 0) {
                return;
            }
        }
    }

    private static <T> Set<String> repeatedKeys(List<PageBounds<T>> pages, double threshold, boolean isTop) {
        Map<String, Long> pageCountByKey = new HashMap<>();
        for (PageBounds<T> page : pages) {
            if (page.hasLines()) {
                pageCountByKey.merge(marginKey(page.edgeLine(isTop)), 1L, Long::sum);
            }
        }
        return pageCountByKey.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static final class PageBounds<T> {

        private final List<T> lines;
        private final Function<T, String> textOf;
        private int from;
        private int to;

        PageBounds(List<T> lines, Function<T, String> textOf) {
            this.lines = new ArrayList<>(lines);
            this.textOf = textOf;
            this.from = 0;
            this.to = lines.size();
        }

        boolean hasLines() {
            return from < to;
        }

        String edgeLine(boolean isTop) {
            return textOf.apply(isTop ? lines.get(from) : lines.get(to - 1));
        }

        void dropEdgeLine(boolean isTop) {
            if (isTop) {
                from++;
            } else {
                to--;
            }
        }

        List<T> remainingLines() {
            return List.copyOf(lines.subList(from, to));
        }
    }
}
