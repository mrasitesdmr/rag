package com.rasitbank.assistant.retrieval.service;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Retrieval adımının sonucunu taşır: uygulanan strateji, bütün adaylar (vector sırasıyla), prompt'a girecek
 * parçalar (prompt sırasıyla) ve adımların süreleri.
 */
public record RetrievedContext(
        RetrievalStrategy strategy,
        int candidates,
        int topK,
        List<RetrievedChunk> candidateChunks,
        List<RetrievedChunk> contextChunks,
        long retrievalLatencyMs,
        @Nullable Long rerankLatencyMs) {

    public RetrievedContext {
        candidateChunks = List.copyOf(candidateChunks);
        contextChunks = List.copyOf(contextChunks);
    }

    public RetrievedContext withoutContextChunks() {
        return new RetrievedContext(strategy, candidates, topK, candidateChunks, List.of(), retrievalLatencyMs,
                rerankLatencyMs);
    }
}
