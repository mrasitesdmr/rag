package com.rasitbank.retrieval.chunk.domain.dto;

import java.util.List;

/**
 * İstenen her metin için, istek sırasıyla, metni içeren güncel parçaların listesini taşır.
 */
public record ChunkMatchesResponse(List<ChunkTextMatchResponse> matches) {

    public ChunkMatchesResponse {
        matches = List.copyOf(matches);
    }
}
