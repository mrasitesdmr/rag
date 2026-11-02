package com.rasitbank.assistant.conversation.domain.entity;

/**
 * Cevabın nasıl üretildiğini belirtir: rehber parçalarıyla (RAG) ya da retrieval yapmadan doğrudan dil modeliyle
 * (Sadece LLM).
 */
public enum GenerationMode {
    RAG,
    LLM_ONLY
}
