package com.rasitbank.assistant.conversation.domain.event;

import com.rasitbank.assistant.conversation.domain.entity.AnswerSource;
import org.jspecify.annotations.Nullable;

/**
 * Cevap akışının "done" olayıdır: kaydedilen turun kimliği, "bulamadım" işareti, kaydedilen son cevap metni,
 * cevabın geldiği yol, token sayıları ve adım süreleri (Sadece LLM turunda retrieval ve rerank süresi boştur).
 */
public record DoneEvent(
        long turnId,
        boolean notFoundAnswer,
        String answer,
        AnswerSource answerSource,
        String chatModel,
        @Nullable Integer promptTokens,
        @Nullable Integer completionTokens,
        @Nullable Long queryRewriteLatencyMs,
        @Nullable Long retrievalLatencyMs,
        @Nullable Long rerankLatencyMs,
        @Nullable Long generationLatencyMs,
        @Nullable Long timeToFirstTokenMs,
        long latencyMs,
        @Nullable String hostLabel) {
}
