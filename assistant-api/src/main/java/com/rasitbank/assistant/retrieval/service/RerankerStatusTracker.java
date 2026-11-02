package com.rasitbank.assistant.retrieval.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reranker'ın son ölçülen durumunu bellekte tutar; ölçüm periyodik sağlık kontrolünden ya da soru sırasındaki rerank
 * çağrısından gelir, son ölçüm kazanır. Reranker yapılandırmada kullanılmıyorsa ölçüm yok sayılır.
 */
@Component
public class RerankerStatusTracker {

    private static final Logger log = LoggerFactory.getLogger(RerankerStatusTracker.class);

    private final boolean rerankerUsed;
    private final AtomicReference<RerankerStatus> status = new AtomicReference<>(RerankerStatus.UNKNOWN);

    public RerankerStatusTracker(AssistantProperties properties) {
        this.rerankerUsed = properties.generation().configuredMode() == GenerationMode.RAG
                && properties.retrieval().configuredStrategy() == RetrievalStrategy.RERANK;
    }

    public boolean isRerankerUsed() {
        return rerankerUsed;
    }

    public RerankerStatus currentStatus() {
        return rerankerUsed ? status.get() : RerankerStatus.NOT_USED;
    }

    public void recordAvailable() {
        record(RerankerStatus.UP);
    }

    public void recordUnavailable() {
        record(RerankerStatus.DOWN);
    }

    private void record(RerankerStatus measured) {
        if (!rerankerUsed) {
            return;
        }
        RerankerStatus previous = status.getAndSet(measured);
        if (previous != measured) {
            log.info("Reranker status changed from={} to={}", previous, measured);
        }
    }
}
