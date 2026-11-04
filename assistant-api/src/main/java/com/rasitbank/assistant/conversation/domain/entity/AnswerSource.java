package com.rasitbank.assistant.conversation.domain.entity;

/**
 * Turun cevabının hangi yoldan geldiğini taşır: dil modeli, boş bilgi bankası, reranker puan eşiği ya da selamlaşma
 * karşılığı.
 */
public enum AnswerSource {
    MODEL,
    EMPTY_KNOWLEDGE_BASE,
    BELOW_RERANK_THRESHOLD,
    SMALL_TALK
}
