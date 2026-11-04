package com.rasitbank.assistant.retrieval.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Rerank stratejisinde prompt'a girecek en iyi parçanın reranker puanı yapılandırılan eşiğin altında mı, ona bakar;
 * eşik boşsa, strateji top-5 ya da yedek yolsa eşik uygulanmaz.
 */
@Component
public class RerankScoreGate {

    private final @Nullable Double minRerankScore;

    public RerankScoreGate(AssistantProperties properties) {
        this.minRerankScore = properties.retrieval().minRerankScore();
    }

    public boolean isBelowThreshold(RetrievedContext context) {
        List<RetrievedChunk> contextChunks = context.contextChunks();
        if (context.strategy() != RetrievalStrategy.RERANK || minRerankScore == null || contextChunks.isEmpty()) {
            return false;
        }
        Double topRerankScore = contextChunks.getFirst().rerankScore();
        return topRerankScore != null && topRerankScore < minRerankScore;
    }

    public @Nullable Double minRerankScore() {
        return minRerankScore;
    }
}
