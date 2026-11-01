package com.rasitbank.assistant.retrieval.client;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Reranker'ın puanlama yanıtını taşır.
 */
public record RerankResponse(@Nullable List<RerankResult> results) {
}
