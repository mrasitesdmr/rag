package com.rasitbank.retrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * İçe alma hattının parçalama, embedding grubu ve tekrar eden sayfa üst/alt bilgisini ayıklama ayarlarını taşır.
 */
@ConfigurationProperties("ingestion")
public record IngestionProperties(
        int targetChunkChars,
        int maxChunkChars,
        int overlapChars,
        int charsPerToken,
        int embeddingBatchSize,
        int repeatedLineMinPages,
        double repeatedLineMinShare,
        int repeatedLineMaxPerEdge) {
}
