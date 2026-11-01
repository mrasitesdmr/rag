package com.rasitbank.assistant.generation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Cevaptaki [n] alıntılarını bulur; sadece 1 ile prompt'taki parça sayısı arasındakileri, cevaptaki ilk geçiş
 * sırasıyla ve tekrarsız döner.
 */
@Component
public class CitationParser {

    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,9})]");

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
}
