package com.rasitbank.assistant.conversation.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.TurnChunk;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Bir turda prompt'a giren parçayı Konuşmalar detayındaki parça tablosuna taşır: prompt'taki yeri, kimliği, sürümü,
 * sayfası, vector sırası, mesafesi, rerank puanı ve golden set'teki cevabın geçtiği parça olup olmadığı.
 */
public record TurnChunkResponse(int n, long chunkId, long sourceId, String version, int chunkNo, int pageNumber,
        String heading, int vectorRank, double distance, @Nullable Double rerankScore, boolean containsAnswer) {

    public static TurnChunkResponse from(TurnChunk chunk, boolean containsAnswer) {
        return new TurnChunkResponse(Objects.requireNonNull(chunk.getPromptPosition()), chunk.getChunkId(),
                chunk.getSourceId(), chunk.getVersion(), chunk.getChunkNo(), chunk.getPageNumber(),
                chunk.getHeading(), chunk.getVectorRank(), chunk.getDistance(), chunk.getRerankScore(), containsAnswer);
    }
}
