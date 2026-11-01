package com.rasitbank.assistant.generation;

import com.rasitbank.assistant.config.AssistantProperties;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Cevabın sabit "bulamadım" cümlesini içerip içermediğine bakar; harf büyüklüğü ve boşluk farkları yok sayılır.
 */
@Component
public class NotFoundAnswerDetector {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final String normalizedSentence;

    public NotFoundAnswerDetector(AssistantProperties properties) {
        this.normalizedSentence = normalize(properties.generation().notFoundAnswer());
    }

    public boolean isNotFoundAnswer(String answer) {
        return normalize(answer).contains(normalizedSentence);
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text.toLowerCase(TURKISH)).replaceAll(" ").strip();
    }
}
