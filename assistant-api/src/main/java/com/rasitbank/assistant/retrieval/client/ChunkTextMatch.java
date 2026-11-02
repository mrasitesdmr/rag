package com.rasitbank.assistant.retrieval.client;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Metin eşleştirme cevabında tek bir metni ve onu içeren güncel parçaları taşır.
 */
public record ChunkTextMatch(String text, @Nullable List<MatchedChunk> chunks) {

    public List<MatchedChunk> chunksOrEmpty() {
        return chunks == null ? List.of() : chunks;
    }
}
