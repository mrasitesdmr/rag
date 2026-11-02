package com.rasitbank.assistant.retrieval.client;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * retrieval-api'nin metin eşleştirme cevabıdır: istenen her metin için, istek sırasıyla, metni içeren güncel parçalar.
 */
public record ChunkMatchesResponse(@Nullable List<ChunkTextMatch> matches) {

    public List<ChunkTextMatch> matchesOrEmpty() {
        return matches == null ? List.of() : matches;
    }
}
