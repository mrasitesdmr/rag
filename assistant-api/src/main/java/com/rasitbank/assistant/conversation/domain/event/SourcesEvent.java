package com.rasitbank.assistant.conversation.domain.event;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import java.util.List;

/**
 * Cevap akışının "sources" olayıdır: uygulanan strateji, modele giden parçalar ve cevaptaki alıntılar.
 */
public record SourcesEvent(
        RetrievalStrategy retrievalStrategy,
        int candidates,
        int topK,
        List<ContextChunk> contextChunks,
        List<Integer> citations) {

    public SourcesEvent {
        contextChunks = List.copyOf(contextChunks);
        citations = List.copyOf(citations);
    }
}
