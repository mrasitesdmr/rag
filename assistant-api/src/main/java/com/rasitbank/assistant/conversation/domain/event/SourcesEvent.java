package com.rasitbank.assistant.conversation.domain.event;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Cevap akışının "sources" olayıdır: üretim modu, uygulanan strateji, arama sorgusu ve yeniden yazmanın sonucu,
 * modele giden parçalar ve cevaptaki alıntılar. Sadece LLM turunda arama alanları boş, listeler boştur.
 */
public record SourcesEvent(
        GenerationMode generationMode,
        RetrievalStrategy retrievalStrategy,
        @Nullable Integer candidates,
        @Nullable Integer topK,
        @Nullable String searchQuery,
        QueryRewriteStatus queryRewriteStatus,
        List<ContextChunk> contextChunks,
        List<Integer> citations) {

    public SourcesEvent {
        contextChunks = List.copyOf(contextChunks);
        citations = List.copyOf(citations);
    }
}
