package com.rasitbank.assistant.generation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Akan cevapta sabit "bulamadım" cümlesinin başı görüldüğü sürece metni tutar; cümle tamamlanınca cümleyi tek parça
 * halinde verir ve modelin sonrasında yazdıklarını (alıntı numarası dahil) bırakır, metin cümleden ayrılınca tutulanı
 * geri verip olduğu gibi geçirir. Tur başına bir örnek kullanılır; thread-safe değildir.
 */
public final class NotFoundAnswerStreamFilter {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private enum State {
        HOLDING,
        PASS,
        MATCHED
    }

    private final String sentence;
    private final String normalizedSentence;
    private final StringBuilder held = new StringBuilder();
    private State state = State.HOLDING;

    NotFoundAnswerStreamFilter(String sentence) {
        this.sentence = sentence;
        this.normalizedSentence = normalize(sentence);
    }

    public String accept(String token) {
        return switch (state) {
            case PASS -> token;
            case MATCHED -> "";
            case HOLDING -> hold(token);
        };
    }

    public String finish() {
        if (state != State.HOLDING) {
            return "";
        }
        state = State.PASS;
        String remaining = held.toString();
        held.setLength(0);
        return remaining;
    }

    public boolean isNotFoundAnswer() {
        return state == State.MATCHED;
    }

    private String hold(String token) {
        held.append(token);
        String normalized = normalize(held.toString());
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.startsWith(normalizedSentence)) {
            state = State.MATCHED;
            held.setLength(0);
            return sentence;
        }
        if (normalizedSentence.startsWith(normalized)) {
            return "";
        }
        state = State.PASS;
        String flushed = held.toString();
        held.setLength(0);
        return flushed;
    }

    static String normalize(String text) {
        return WHITESPACE.matcher(text.toLowerCase(TURKISH)).replaceAll(" ").stripLeading();
    }
}
