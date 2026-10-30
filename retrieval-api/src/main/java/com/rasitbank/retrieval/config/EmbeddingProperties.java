package com.rasitbank.retrieval.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Embedding modelinin (Ollama) adresini, adını, boyutunu, zaman aşımlarını ve yeniden deneme ayarlarını taşır.
 */
@ConfigurationProperties("embedding")
public record EmbeddingProperties(
        String baseUrl,
        String model,
        int dimensions,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        Duration retryBackoff,
        int maxConcurrentRequests) {

    public String modelLabel() {
        return model + "@" + dimensions;
    }
}
