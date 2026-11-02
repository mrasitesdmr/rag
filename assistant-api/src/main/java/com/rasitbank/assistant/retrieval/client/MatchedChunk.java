package com.rasitbank.assistant.retrieval.client;

/**
 * Aranan metni içeren parçanın kimliğini, kaynağını, sürüm etiketini ve sayfasını taşır.
 */
public record MatchedChunk(long chunkId, long sourceId, String sourceName, String version, int pageNumber) {
}
