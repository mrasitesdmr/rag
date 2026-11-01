package com.rasitbank.retrieval.source.domain.dto;

import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import com.rasitbank.retrieval.source.domain.entity.SourceVersion;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Bilgi kaynakları tablosundaki ve kaynak detayındaki bir kaynağı dosyası, güncel sürümünün sayıları ve son içe alma
 * işiyle istemciye taşır.
 */
public record SourceResponse(
        long sourceId,
        long workspaceId,
        String name,
        SourceKind kind,
        Instant createdAt,
        String fileName,
        long fileSizeBytes,
        String version,
        @Nullable Integer pageCount,
        @Nullable Integer chapterCount,
        @Nullable Integer chunkCount,
        @Nullable Integer averageTokenCount,
        @Nullable Instant syncedAt,
        IngestionJobResponse latestJob) {

    public static SourceResponse of(Source source, @Nullable SourceVersion activeVersion,
            @Nullable Double averageTokenCount, IngestionJob latestJob) {
        IngestionJobResponse latestJobResponse = IngestionJobResponse.from(latestJob);
        if (activeVersion == null) {
            return new SourceResponse(source.getId(), source.getWorkspaceId(), source.getName(), source.getKind(),
                    source.getCreatedAt(), latestJob.getFileName(), latestJob.getFileSizeBytes(),
                    latestJob.getVersion(), null, null, null, null, null, latestJobResponse);
        }
        return new SourceResponse(source.getId(), source.getWorkspaceId(), source.getName(), source.getKind(),
                source.getCreatedAt(), activeVersion.getFileName(), activeVersion.getFileSizeBytes(),
                activeVersion.getVersion(), activeVersion.getPageCount(), activeVersion.getChapterCount(),
                activeVersion.getChunkCount(), roundAverage(averageTokenCount), activeVersion.getSyncedAt(),
                latestJobResponse);
    }

    private static @Nullable Integer roundAverage(@Nullable Double averageTokenCount) {
        return averageTokenCount == null ? null : (int) Math.round(averageTokenCount);
    }
}
