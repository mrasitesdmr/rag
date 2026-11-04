package com.rasitbank.assistant.generation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Cevaptaki [n] alıntılarını bulur (sadece 1 ile prompt'taki parça sayısı arasındakileri, ilk geçiş sırasıyla ve
 * tekrarsız), kaydedilecek cevaptan aralık dışı numaraları ve geçmişe girecek cevaplardan bütün numaraları siler.
 */
@Component
public class CitationParser {

    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,9})]");
    private static final Pattern CITATION_WITH_LEADING_SPACE = Pattern.compile("\\s*\\[\\d+]");
    private static final Pattern NUMBERED_CITATION_WITH_LEADING_SPACE = Pattern.compile("\\s*\\[(\\d{1,9})]");

    public List<Integer> parseCitations(String answer, int contextSize) {
        Set<Integer> citations = new LinkedHashSet<>();
        Matcher matcher = CITATION.matcher(answer);
        while (matcher.find()) {
            int n = Integer.parseInt(matcher.group(1));
            if (n >= 1 && n <= contextSize) {
                citations.add(n);
            }
        }
        return List.copyOf(citations);
    }

    public String removeOutOfRangeCitations(String answer, int contextSize) {
        Matcher matcher = NUMBERED_CITATION_WITH_LEADING_SPACE.matcher(answer);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            int n = Integer.parseInt(matcher.group(1));
            String replacement = n >= 1 && n <= contextSize ? matcher.group() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public String removeCitations(String answer) {
        return CITATION_WITH_LEADING_SPACE.matcher(answer).replaceAll("").strip();
    }
}
