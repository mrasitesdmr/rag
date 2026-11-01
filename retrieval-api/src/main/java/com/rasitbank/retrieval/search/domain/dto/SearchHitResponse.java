package com.rasitbank.retrieval.search.domain.dto;

/**
 * Aramada dönen bir parçayı, ait olduğu kaynak ve sürümle ve sorguya cosine mesafesiyle istemciye taşır.
 */
public record SearchHitResponse(
        long chunkId,
        int chunkNo,
        int pageNumber,
        String heading,
        String content,
        long sourceId,
        String sourceName,
        String version,
        double distance) {
}
