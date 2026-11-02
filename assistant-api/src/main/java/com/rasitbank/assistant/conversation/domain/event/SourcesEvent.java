package com.rasitbank.assistant.conversation.domain.event;

import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import java.util.List;

/**
 * Cevap akışının "sources" olayıdır: uygulanan strateji, arama sorgusu ve yeniden yazmanın sonucu, modele giden
 * parçalar ve cevaptaki alıntılar.
 */
public record SourcesEvent(
        RetrievalStrategy retrievalStrategy,
        int candidates,
        int topK,
        String searchQuery,
        QueryRewriteStatus queryRewriteStatus,
        List<ContextChunk> contextChunks,
        List<Integer> citations) {

    public SourcesEvent {
        contextChunks = List.copyOf(contextChunks);
        citations = List.copyOf(citations);
    }
}
