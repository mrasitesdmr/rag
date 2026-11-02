package com.rasitbank.assistant.conversation.domain.event;

import org.jspecify.annotations.Nullable;

/**
 * Cevap akışının "done" olayıdır: kaydedilen turun kimliği, "bulamadım" işareti, token sayıları ve adım süreleri.
 */
public record DoneEvent(
        long turnId,
        boolean notFoundAnswer,
        String chatModel,
        @Nullable Integer promptTokens,
        @Nullable Integer completionTokens,
        @Nullable Long queryRewriteLatencyMs,
        long retrievalLatencyMs,
        @Nullable Long rerankLatencyMs,
        @Nullable Long generationLatencyMs,
        @Nullable Long timeToFirstTokenMs,
        long latencyMs,
        @Nullable String hostLabel) {
}
