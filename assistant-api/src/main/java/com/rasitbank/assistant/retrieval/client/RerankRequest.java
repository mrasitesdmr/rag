package com.rasitbank.assistant.retrieval.client;

import java.util.List;

/**
 * Reranker'ın (llama-server /v1/rerank) puanlama isteğini taşır: model, sorgu ve vector sırasıyla belgeler.
 */
public record RerankRequest(String model, String query, List<String> documents) {
}
