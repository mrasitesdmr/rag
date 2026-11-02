package com.rasitbank.assistant.suggestion.domain.dto;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Öneri metinlerini web'in eşleştirmede kullandığı biçime getirir; benzersizlik kontrolü bu biçim üzerinden yapılır.
 */
public final class SuggestionTextNormalizer {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private SuggestionTextNormalizer() {
    }

    public static String normalize(String text) {
        String lowerCase = text.toLowerCase(TURKISH);
        String lettersAndDigits = NON_WORD.matcher(lowerCase).replaceAll(" ");
        return WHITESPACE_RUN.matcher(lettersAndDigits).replaceAll(" ").strip();
    }
}
