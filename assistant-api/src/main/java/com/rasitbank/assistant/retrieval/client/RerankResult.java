package com.rasitbank.assistant.retrieval.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Reranker yanıtındaki bir belgenin puanını taşır: gönderilen belge dizisindeki yeri ve ham puanı.
 */
public record RerankResult(@Nullable Integer index, @JsonProperty("relevance_score") @Nullable Double relevanceScore) {
}
