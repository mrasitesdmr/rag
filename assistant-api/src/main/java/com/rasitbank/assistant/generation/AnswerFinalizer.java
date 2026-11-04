package com.rasitbank.assistant.generation;

import com.rasitbank.assistant.config.AssistantProperties;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * RAG turunda dil modelinin ham cevabını tura kaydedilecek son haline getirir: "bulamadım" cevabını tam o cümleye
 * indirip alıntısız bırakır; cevap hedefli bir giriş cümlesiyle başlıyorsa o cümleye bağlanan alıntıları siler; diğer
 * cevaplardan aralık dışı alıntı işaretlerini siler.
 */
@Component
public class AnswerFinalizer {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]");
    private static final Pattern CITATION_WITH_LEADING_SPACE = Pattern.compile("\\s*\\[\\d{1,9}]");
    private static final Pattern LEADING_CITATIONS = Pattern.compile("^(\\s*\\[\\d{1,9}])+");

    private final NotFoundAnswerDetector notFoundAnswerDetector;
    private final CitationParser citationParser;
    private final List<String> normalizedOpeners;

    public AnswerFinalizer(NotFoundAnswerDetector notFoundAnswerDetector, CitationParser citationParser,
            AssistantProperties properties) {
        this.notFoundAnswerDetector = notFoundAnswerDetector;
        this.citationParser = citationParser;
        this.normalizedOpeners = properties.generation().targetedOpeners().stream()
                .map(AnswerFinalizer::normalize)
                .toList();
    }

    public FinalAnswer finalizeRagAnswer(String modelText, int contextSize) {
        if (notFoundAnswerDetector.isNotFoundAnswer(modelText)) {
            return new FinalAnswer(notFoundAnswerDetector.notFoundAnswer(), true, List.of(), 0);
        }
        OpenerCleanup cleanup = removeOpenerCitations(modelText);
        String answer = citationParser.removeOutOfRangeCitations(cleanup.text(), contextSize).strip();
        return new FinalAnswer(answer, false, citationParser.parseCitations(answer, contextSize),
                cleanup.removedCount());
    }

    private record OpenerCleanup(String text, int removedCount) {
    }

    private OpenerCleanup removeOpenerCitations(String modelText) {
        String text = modelText.stripLeading();
        Matcher end = SENTENCE_END.matcher(text);
        if (!end.find()) {
            return new OpenerCleanup(modelText, 0);
        }
        String firstSentence = text.substring(0, end.end());
        String firstSentenceWithoutCitations = CITATION_WITH_LEADING_SPACE.matcher(firstSentence).replaceAll("");
        if (!normalizedOpeners.contains(normalize(firstSentenceWithoutCitations))) {
            return new OpenerCleanup(modelText, 0);
        }
        String rest = text.substring(end.end());
        Matcher trailing = LEADING_CITATIONS.matcher(rest);
        String trailingCitations = trailing.find() ? trailing.group() : "";
        String remaining = rest.substring(trailingCitations.length()).stripLeading();
        int removedCount = countCitations(firstSentence) + countCitations(trailingCitations);
        String cleaned = remaining.isEmpty()
                ? firstSentenceWithoutCitations
                : firstSentenceWithoutCitations + " " + remaining;
        return new OpenerCleanup(cleaned, removedCount);
    }

    private static int countCitations(String text) {
        return (int) CITATION_WITH_LEADING_SPACE.matcher(text).results().count();
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text.toLowerCase(TURKISH)).replaceAll(" ").strip();
    }
}
