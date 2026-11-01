package com.rasitbank.retrieval.chunk.domain.dto;

import com.rasitbank.retrieval.chunk.domain.entity.Chunk;

/**
 * Kaynak detayındaki parça tablosunun bir satırını embedding'siz olarak istemciye taşır.
 */
public record ChunkSummaryResponse(long chunkId, int chunkNo, int pageNumber, String heading, String content,
        int tokenCount) {

    public static ChunkSummaryResponse from(Chunk chunk) {
        return new ChunkSummaryResponse(chunk.getId(), chunk.getChunkNo(), chunk.getPageNumber(), chunk.getHeading(),
                chunk.getContent(), chunk.getTokenCount());
    }
}
