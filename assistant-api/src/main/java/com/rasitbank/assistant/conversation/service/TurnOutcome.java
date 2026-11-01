package com.rasitbank.assistant.conversation.service;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Cevap üretiminin sonucunu taşır: cevap metni, "bulamadım" işareti, alıntılar, token sayıları, süreler ve
 * hata olduysa errorCode.
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
        @Nullable String errorCode) {

    public TurnOutcome {
        citations = List.copyOf(citations);
    }

    public static TurnOutcome failed(@Nullable String answer, String errorCode, @Nullable Long generationLatencyMs,
            @Nullable Long timeToFirstTokenMs, long latencyMs) {
        return new TurnOutcome(answer, false, List.of(), null, null, generationLatencyMs, timeToFirstTokenMs,
                latencyMs, errorCode);
    }
}
