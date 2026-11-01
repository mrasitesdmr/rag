package com.rasitbank.retrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Vektör aramasının HNSW ayarlarını (ef_search ve iterative scan üst sınırı) taşır.
 */
@ConfigurationProperties("search")
public record SearchProperties(int efSearch, int maxScanTuples) {
}
