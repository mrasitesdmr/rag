package com.rasitbank.assistant.conversation.domain.entity;

/**
 * Bir turda prompt'a giren parçaların nasıl seçildiğini belirtir: vector sırasının ilkleri, reranker sırası
 * ya da reranker başarısız olunca vector sırasına dönüş.
 */
public enum RetrievalStrategy {
    TOP_K,
    RERANK,
    RERANK_FALLBACK
}
