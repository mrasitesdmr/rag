package com.rasitbank.assistant.retrieval.service;

/**
 * Reranker'ın son ölçülen erişilebilirliğidir: ulaşıldı, ulaşılamadı, henüz ölçülmedi ya da yapılandırmada
 * kullanılmıyor.
 */
public enum RerankerStatus {
    UP,
    DOWN,
    UNKNOWN,
    NOT_USED
}
