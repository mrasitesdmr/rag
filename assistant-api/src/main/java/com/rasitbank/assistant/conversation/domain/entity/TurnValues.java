package com.rasitbank.assistant.conversation.domain.entity;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Akış bitince kaydedilecek turun bütün değerlerini (soru, arama sorgusu, mod, strateji, cevap, token sayıları,
 * süreler, hata kodu) tek yerde toplar; Sadece LLM turunda arama sorgusu ve aday/top-k boştur.
 */
public record TurnValues(
        String question,
        @Nullable String searchQuery,
        QueryRewriteStatus queryRewriteStatus,
        @Nullable Integer queryRewriteLatencyMs,
        int historyTurns,
        GenerationMode generationMode,
        RetrievalStrategy retrievalStrategy,
        @Nullable Integer candidates,
        @Nullable Integer topK,
        String chatModel,
        @Nullable String answer,
        boolean notFoundAnswer,
        List<Integer> cited,
        @Nullable Integer promptTokens,
        @Nullable Integer completionTokens,
        @Nullable Integer retrievalLatencyMs,
        @Nullable Integer rerankLatencyMs,
        @Nullable Integer generationLatencyMs,
        @Nullable Integer timeToFirstTokenMs,
        int latencyMs,
        @Nullable String errorCode,
        Instant createdAt,
        Instant completedAt) {

    public TurnValues {
        cited = List.copyOf(cited);
    }
}
