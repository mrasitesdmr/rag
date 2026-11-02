package com.rasitbank.assistant.liveconfig.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.retrieval.service.RerankerStatus;
import org.jspecify.annotations.Nullable;

/**
 * Asistanın etkin yapılandırmasıdır: dil modeli, embedding modeli ve boyutu, üretim modu, yapılandırılan retrieval
 * stratejisi, aday ve top-k sayıları, sorgu yeniden yazma, reranker'ın son ölçülen durumu ve bilgi bankası durumu.
 */
public record LiveConfigResponse(
        String chatModel,
        @Nullable String embeddingModel,
        @Nullable Integer embeddingDimensions,
        GenerationMode generationMode,
        RetrievalStrategy retrievalStrategy,
        int candidates,
        int topK,
        boolean queryRewriteEnabled,
        RerankerStatus rerankerStatus,
        KnowledgeBaseResponse knowledgeBase) {
}
