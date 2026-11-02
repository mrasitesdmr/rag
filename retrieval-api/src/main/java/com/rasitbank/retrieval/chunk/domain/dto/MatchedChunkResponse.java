package com.rasitbank.retrieval.chunk.domain.dto;

import com.rasitbank.retrieval.chunk.repository.ChunkTextMatchRow;

/**
 * Aranan metni içeren parçanın kimliğini, kaynağını, sürüm etiketini ve sayfasını taşır.
 */
public record MatchedChunkResponse(long chunkId, long sourceId, String sourceName, String version, int pageNumber) {

    public static MatchedChunkResponse from(ChunkTextMatchRow row) {
        return new MatchedChunkResponse(row.getChunkId(), row.getSourceId(), row.getSourceName(), row.getVersion(),
                row.getPageNumber());
    }
}
