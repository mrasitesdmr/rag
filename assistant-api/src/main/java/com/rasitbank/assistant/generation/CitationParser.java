package com.rasitbank.assistant.generation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Cevaptaki [n] alıntılarını bulur (sadece 1 ile prompt'taki parça sayısı arasındakileri, ilk geçiş sırasıyla ve
 * tekrarsız) ve geçmişe girecek cevaplardan alıntı numaralarını siler.
 */
@Component
public class CitationParser {

    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,9})]");
    private static final Pattern CITATION_WITH_LEADING_SPACE = Pattern.compile("\\s*\\[\\d+]");

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

    public String removeCitations(String answer) {
        return CITATION_WITH_LEADING_SPACE.matcher(answer).replaceAll("").strip();
    }
}
