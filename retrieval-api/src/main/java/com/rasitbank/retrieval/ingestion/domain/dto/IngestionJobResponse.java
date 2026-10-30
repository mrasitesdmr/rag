package com.rasitbank.retrieval.ingestion.domain.dto;

import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * İçe alma işinin aşamasını, ilerlemesini, hata kodunu ve zaman damgalarını istemciye taşır.
 */
public record IngestionJobResponse(
        long jobId,
        long workspaceId,
        long sourceId,
        String version,
        String fileName,
        IngestionStage stage,
        @Nullable IngestionStage failedStage,
        double progress,
        int embeddedChunks,
        @Nullable Integer totalChunks,
        @Nullable String errorCode,
        Instant createdAt,
        @Nullable Instant startedAt,
        @Nullable Instant finishedAt,
        String statusUrl) {

    public static IngestionJobResponse from(IngestionJob job) {
        return new IngestionJobResponse(job.getId(), job.getWorkspaceId(), job.getSourceId(), job.getVersion(),
                job.getFileName(), job.getStage(), job.getFailedStage(), job.getProgress(), job.getEmbeddedChunks(),
                job.getTotalChunks(), job.getErrorCode(), job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt(),
                statusUrl(job.getWorkspaceId(), job.getId()));
    }

    public static String statusUrl(long workspaceId, long jobId) {
        return "/api/workspaces/" + workspaceId + "/ingestion-jobs/" + jobId;
    }
}
