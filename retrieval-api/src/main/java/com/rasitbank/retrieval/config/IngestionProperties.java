package com.rasitbank.retrieval.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * İçe alma hattının parçalama, embedding grubu, sayfa üst/alt bilgi kalıbı ve rehberin ilk giriş ayarlarını taşır.
 */
@ConfigurationProperties("ingestion")
public record IngestionProperties(
        int targetChunkChars,
        int maxChunkChars,
        int overlapChars,
        int charsPerToken,
        int embeddingBatchSize,
        String pageHeaderPattern,
        String pageFooterPattern,
        Bootstrap bootstrap) {

    public record Bootstrap(boolean enabled, Path file, String sourceName, String version) {
    }
}
