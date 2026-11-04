package com.rasitbank.assistant.generation;

import com.rasitbank.assistant.config.AssistantProperties;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Cevabın "bulamadım" cevabı olup olmadığına bakar: sabit cümleyi içeriyorsa ya da aynı cümlede "rehberimizde …
 * bulamadım" diyorsa (harf büyüklüğü ve boşluk farkları yok sayılır); akan cevapta sabit cümleyi tutacak tur başına
 * filtreyi de üretir.
 */
@Component
public class NotFoundAnswerDetector {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PARAPHRASED_NOT_FOUND = Pattern.compile("rehberimizde[^.!?\\n]*?bulamad[ıi]m");

    private final String notFoundAnswer;
    private final String normalizedSentence;

    public NotFoundAnswerDetector(AssistantProperties properties) {
        this.notFoundAnswer = properties.generation().notFoundAnswer();
        this.normalizedSentence = normalize(notFoundAnswer);
    }

    public boolean isNotFoundAnswer(String answer) {
        String normalized = normalize(answer);
        return normalized.contains(normalizedSentence) || PARAPHRASED_NOT_FOUND.matcher(normalized).find();
    }

    public NotFoundAnswerStreamFilter newStreamFilter() {
        return new NotFoundAnswerStreamFilter(notFoundAnswer);
    }

    public String notFoundAnswer() {
        return notFoundAnswer;
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text.toLowerCase(TURKISH)).replaceAll(" ").strip();
    }
}
