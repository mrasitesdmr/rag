package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.conversation.domain.entity.AnswerSource;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Cevap üretiminin sonucunu taşır: cevap metni, "bulamadım" işareti, alıntılar, token sayıları, süreler, hata
 * olduysa errorCode ve cevabın geldiği yol.
 */
public record TurnOutcome(
        @Nullable String answer,
        boolean notFoundAnswer,
        List<Integer> citations,
        @Nullable Integer promptTokens,
        @Nullable Integer completionTokens,
        @Nullable Long generationLatencyMs,
        @Nullable Long timeToFirstTokenMs,
        long latencyMs,
        @Nullable String errorCode,
        AnswerSource answerSource) {

    public TurnOutcome {
        citations = List.copyOf(citations);
    }

    public static TurnOutcome failed(@Nullable String answer, String errorCode, @Nullable Long generationLatencyMs,
            @Nullable Long timeToFirstTokenMs, long latencyMs) {
        return new TurnOutcome(answer, false, List.of(), null, null, generationLatencyMs, timeToFirstTokenMs,
                latencyMs, errorCode, AnswerSource.MODEL);
    }
}
