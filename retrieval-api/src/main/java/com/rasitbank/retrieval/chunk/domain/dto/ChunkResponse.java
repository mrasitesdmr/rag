package com.rasitbank.retrieval.chunk.domain.dto;

import com.rasitbank.retrieval.chunk.domain.entity.Chunk;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceVersion;
import java.util.Arrays;

/**
 * Parça penceresi için tek bir parçayı tam metni, embedding'inin ilk değerleri ve ait olduğu kaynak sürümüyle taşır.
 */
public record ChunkResponse(
        long chunkId,
        int chunkNo,
        int pageNumber,
        String heading,
        String content,
        int tokenCount,
        String embeddingModel,
        int embeddingDimensions,
        float[] embeddingPreview,
        long sourceId,
        String sourceName,
        String version) {

    public static final int EMBEDDING_PREVIEW_SIZE = 8;

    public ChunkResponse {
        embeddingPreview = embeddingPreview.clone();
    }

    public static ChunkResponse of(Chunk chunk, Source source, SourceVersion sourceVersion) {
        float[] embedding = chunk.getEmbedding();
        return new ChunkResponse(chunk.getId(), chunk.getChunkNo(), chunk.getPageNumber(), chunk.getHeading(),
                chunk.getContent(), chunk.getTokenCount(), chunk.getEmbeddingModel(), embedding.length,
                Arrays.copyOf(embedding, Math.min(EMBEDDING_PREVIEW_SIZE, embedding.length)), source.getId(),
                source.getName(), sourceVersion.getVersion());
    }

    @Override
    public float[] embeddingPreview() {
        return embeddingPreview.clone();
    }
}
