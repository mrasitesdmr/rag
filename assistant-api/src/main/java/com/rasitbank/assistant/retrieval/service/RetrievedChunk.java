package com.rasitbank.assistant.retrieval.service;

import com.rasitbank.assistant.retrieval.client.SearchHit;
import org.jspecify.annotations.Nullable;

/**
 * Retrieval adımında gelen bir aday parçayı, retrieval-api sonucundaki sırası ve varsa rerank puanıyla taşır.
 */
public record RetrievedChunk(SearchHit hit, int vectorRank, @Nullable Double rerankScore) {
}
