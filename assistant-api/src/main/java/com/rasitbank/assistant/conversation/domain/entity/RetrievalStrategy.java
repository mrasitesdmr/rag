package com.rasitbank.assistant.conversation.domain.entity;

/**
 * Bir turda prompt'a giren parçaların nasıl seçildiğini belirtir: vector sırasının ilkleri, reranker sırası,
 * reranker başarısız olunca vector sırasına dönüş ya da Sadece LLM turunda retrieval yapılmaması.
 */
public enum RetrievalStrategy {
    TOP_K,
    RERANK,
    RERANK_FALLBACK,
    NONE
}
