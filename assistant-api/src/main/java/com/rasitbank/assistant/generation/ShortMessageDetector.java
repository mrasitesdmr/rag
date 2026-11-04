package com.rasitbank.assistant.generation;

import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import org.springframework.stereotype.Component;

/**
 * Müşteri mesajının, rehberde aranmadan önce dil modeline selamlaşma etiketi sorulacak kadar kısa olup olmadığına
 * ve hiç harf ya da rakam içermediğine karar verir.
 */
@Component
public class ShortMessageDetector {

    private static final int SHORT_MESSAGE_MAX_WORDS = 10;
    private static final int SHORT_QUESTION_MAX_WORDS = 5;

    public boolean hasNoWords(String message) {
        return SuggestionTextNormalizer.normalize(message).isEmpty();
    }

    public boolean isShortMessage(String message) {
        String normalized = SuggestionTextNormalizer.normalize(message);
        if (normalized.isEmpty()) {
            return false;
        }
        int wordCount = normalized.split(" ").length;
        int maxWords = message.indexOf('?') >= 0 ? SHORT_QUESTION_MAX_WORDS : SHORT_MESSAGE_MAX_WORDS;
        return wordCount <= maxWords;
    }
}
