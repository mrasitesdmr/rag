package com.rasitbank.assistant.retrieval.client;

/**
 * retrieval-api'nin çalışma alanı yanıtından canlı yapılandırmanın ihtiyaç duyduğu alanları taşır: ad, embedding
 * modeli ve boyutu.
 */
public record WorkspaceResponse(long workspaceId, String name, String embeddingModel, int embeddingDimensions) {
}
