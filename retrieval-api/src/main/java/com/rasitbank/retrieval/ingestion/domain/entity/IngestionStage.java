package com.rasitbank.retrieval.ingestion.domain.entity;

import java.util.Arrays;
import java.util.List;

/**
 * İçe alma işinin aşamasıdır: QUEUED → PARSING → CHUNKING → EMBEDDING → WRITING → DONE, hata olursa FAILED.
 */
public enum IngestionStage {
    QUEUED,
    PARSING,
    CHUNKING,
    EMBEDDING,
    WRITING,
    DONE,
    FAILED;

    public boolean isActive() {
        return this != DONE && this != FAILED;
    }

    public static List<IngestionStage> activeStages() {
        return Arrays.stream(values()).filter(IngestionStage::isActive).toList();
    }
}
