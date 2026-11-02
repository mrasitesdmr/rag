package com.rasitbank.assistant.conversation.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.domain.entity.TurnStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Kayıtlı bir turu Konuşmalar detayına taşır: soru, arama sorgusu, cevap, hesaplanan durum, üretim ayarları, token
 * sayıları, süre, geri bildirim ve prompt'a giren parçalar.
 */
public record TurnResponse(long turnId, int turnNo, TurnStatus status, String question, @Nullable String searchQuery,
        @Nullable String answer, GenerationMode generationMode, RetrievalStrategy retrievalStrategy,
        @Nullable Integer candidates, @Nullable Integer topK, List<Integer> cited, @Nullable Integer promptTokens,
        @Nullable Integer completionTokens, int latencyMs, @Nullable String errorCode, @Nullable Boolean helpful,
        Instant createdAt, Instant completedAt, List<TurnChunkResponse> contextChunks) {

    public TurnResponse {
        cited = List.copyOf(cited);
        contextChunks = List.copyOf(contextChunks);
    }

    public static TurnResponse from(Turn turn, TurnStatus status) {
        List<TurnChunkResponse> contextChunks = turn.getChunks().stream()
                .filter(chunk -> chunk.getPromptPosition() != null)
                .map(TurnChunkResponse::from)
                .sorted(Comparator.comparingInt(TurnChunkResponse::n))
                .toList();
        return new TurnResponse(Objects.requireNonNull(turn.getId()), turn.getTurnNo(), status, turn.getQuestion(),
                turn.getSearchQuery(), turn.getAnswer(), turn.getGenerationMode(), turn.getRetrievalStrategy(),
                turn.getCandidates(), turn.getTopK(), turn.getCited(), turn.getPromptTokens(),
                turn.getCompletionTokens(), turn.getLatencyMs(), turn.getErrorCode(), turn.getHelpful(),
                turn.getCreatedAt(), turn.getCompletedAt(), contextChunks);
    }
}
