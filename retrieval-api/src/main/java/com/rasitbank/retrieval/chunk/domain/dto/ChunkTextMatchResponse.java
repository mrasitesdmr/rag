package com.rasitbank.retrieval.chunk.domain.dto;

import java.util.List;

/**
 * Tek bir metni ve onu içeren güncel parçaları parça kimliği sırasıyla taşır.
 */
public record ChunkTextMatchResponse(String text, List<MatchedChunkResponse> chunks) {

    public ChunkTextMatchResponse {
        chunks = List.copyOf(chunks);
    }
}
