package com.rasitbank.assistant.retrieval.client;

/**
 * retrieval-api aramasında dönen bir parçayı, kaynağı, sürümü ve sorguya cosine mesafesiyle taşır.
 */
public record SearchHit(
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
