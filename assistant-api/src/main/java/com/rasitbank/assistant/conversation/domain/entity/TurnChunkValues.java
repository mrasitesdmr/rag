package com.rasitbank.assistant.conversation.domain.entity;

import org.jspecify.annotations.Nullable;

/**
 * Tur kaydedilirken bir aday parçanın yazılacak değerlerini taşır.
 */
public record TurnChunkValues(
        int vectorRank,
        long chunkId,
        long sourceId,
        String version,
        int chunkNo,
        int pageNumber,
        String heading,
        double distance,
        @Nullable Double rerankScore,
        @Nullable Integer promptPosition) {
}
