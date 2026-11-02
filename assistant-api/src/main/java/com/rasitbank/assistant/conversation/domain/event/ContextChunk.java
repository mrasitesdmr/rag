package com.rasitbank.assistant.conversation.domain.event;

import org.jspecify.annotations.Nullable;

/**
 * "sources" olayında prompt'a giren bir parçayı taşır: prompt'taki numarası, kaynağı, vector sırası, mesafesi,
 * rerank puanı ve golden set'teki cevabın geçtiği parça olup olmadığı.
 */
public record ContextChunk(
        int n,
        long chunkId,
        int chunkNo,
        int pageNumber,
        String heading,
        long sourceId,
        String sourceName,
        String version,
        int vectorRank,
        double distance,
        @Nullable Double rerankScore,
        boolean containsAnswer) {
}
