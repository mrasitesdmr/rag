package com.rasitbank.assistant.retrieval.service;

import com.rasitbank.assistant.retrieval.client.RerankClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reranker kullanılıyorsa sağlık ucunu belirli aralıkla yoklar ve sonucu durum takipçisine yazar; böylece soru
 * gelmese de reranker'ın düştüğü ya da geri geldiği görülür.
 */
@Component
@ConditionalOnProperty(name = "assistant.reranker.health-check-enabled", havingValue = "true")
public class RerankerHealthChecker {

    private final RerankClient rerankClient;
    private final RerankerStatusTracker tracker;

    public RerankerHealthChecker(RerankClient rerankClient, RerankerStatusTracker tracker) {
        this.rerankClient = rerankClient;
        this.tracker = tracker;
    }

    @Scheduled(fixedDelayString = "${assistant.reranker.health-check-interval}")
    public void checkRerankerHealth() {
        if (!tracker.isRerankerUsed()) {
            return;
        }
        if (rerankClient.isHealthy()) {
            tracker.recordAvailable();
        } else {
            tracker.recordUnavailable();
        }
    }
}
