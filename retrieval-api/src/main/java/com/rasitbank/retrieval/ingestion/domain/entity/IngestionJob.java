package com.rasitbank.retrieval.ingestion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Bir dosyanın bir sürüm etiketiyle bilgi bankasına içe alınma işidir; aşamasını, ilerlemesini ve hata kodunu tutar.
 */
@Entity
@Table(name = "ingestion_jobs")
public class IngestionJob {

    public static final double QUEUED_PROGRESS = 0.0;
    public static final double PARSING_PROGRESS = 0.05;
    public static final double CHUNKING_PROGRESS = 0.1;
    public static final double EMBEDDING_START_PROGRESS = 0.1;
    public static final double EMBEDDING_PROGRESS_SPAN = 0.8;
    public static final double WRITING_PROGRESS = 0.9;
    public static final double DONE_PROGRESS = 1.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private long workspaceId;

    @Column(name = "source_id", nullable = false)
    private long sourceId;

    @Column(nullable = false)
    private String version;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestionStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "failed_stage")
    private @Nullable IngestionStage failedStage;

    @Column(nullable = false)
    private double progress;

    @Column(name = "embedded_chunks", nullable = false)
    private int embeddedChunks;

    @Column(name = "total_chunks")
    private @Nullable Integer totalChunks;

    @Column(name = "error_code")
    private @Nullable String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private @Nullable Instant startedAt;

    @Column(name = "finished_at")
    private @Nullable Instant finishedAt;

    protected IngestionJob() {
    }

    private IngestionJob(long workspaceId, long sourceId, String version, String fileName, long fileSizeBytes,
            Instant createdAt) {
        this.workspaceId = workspaceId;
        this.sourceId = sourceId;
        this.version = version;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.stage = IngestionStage.QUEUED;
        this.progress = QUEUED_PROGRESS;
        this.createdAt = createdAt;
    }

    public static IngestionJob queued(long workspaceId, long sourceId, String version, String fileName,
            long fileSizeBytes, Instant now) {
        return new IngestionJob(workspaceId, sourceId, version, fileName, fileSizeBytes, now);
    }

    public void start(Instant now) {
        requireStage(IngestionStage.QUEUED);
        this.stage = IngestionStage.PARSING;
        this.progress = PARSING_PROGRESS;
        this.startedAt = now;
    }

    public void moveTo(IngestionStage nextStage) {
        requireActive();
        double nextProgress = switch (nextStage) {
            case CHUNKING -> CHUNKING_PROGRESS;
            case EMBEDDING -> EMBEDDING_START_PROGRESS;
            case WRITING -> WRITING_PROGRESS;
            default -> throw new IllegalArgumentException("Cannot move a job to stage " + nextStage);
        };
        requireStage(IngestionStage.values()[nextStage.ordinal() - 1]);
        this.stage = nextStage;
        this.progress = nextProgress;
    }

    public void recordTotalChunks(int totalChunks) {
        requireActive();
        if (totalChunks < 0) {
            throw new IllegalArgumentException("Total chunks must not be negative: " + totalChunks);
        }
        this.totalChunks = totalChunks;
    }

    public void recordEmbeddingProgress(int embeddedChunks) {
        requireStage(IngestionStage.EMBEDDING);
        if (totalChunks == null || totalChunks == 0 || embeddedChunks < 0 || embeddedChunks > totalChunks) {
            throw new IllegalArgumentException("Embedded chunks " + embeddedChunks + " out of range for total "
                    + totalChunks);
        }
        this.embeddedChunks = embeddedChunks;
        this.progress = EMBEDDING_START_PROGRESS + EMBEDDING_PROGRESS_SPAN * embeddedChunks / totalChunks;
    }

    public void complete(Instant now) {
        requireStage(IngestionStage.WRITING);
        this.stage = IngestionStage.DONE;
        this.progress = DONE_PROGRESS;
        this.finishedAt = now;
    }

    public void fail(String errorCode, Instant now) {
        requireActive();
        this.failedStage = stage;
        this.stage = IngestionStage.FAILED;
        this.errorCode = errorCode;
        this.finishedAt = now;
    }

    public boolean isFinished() {
        return !stage.isActive();
    }

    private void requireActive() {
        if (isFinished()) {
            throw new IllegalStateException("Ingestion job " + id + " is already " + stage);
        }
    }

    private void requireStage(IngestionStage expected) {
        requireActive();
        if (stage != expected) {
            throw new IllegalStateException("Ingestion job " + id + " is " + stage + ", expected " + expected);
        }
    }

    public Long getId() {
        return id;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public long getSourceId() {
        return sourceId;
    }

    public String getVersion() {
        return version;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public IngestionStage getStage() {
        return stage;
    }

    public @Nullable IngestionStage getFailedStage() {
        return failedStage;
    }

    public double getProgress() {
        return progress;
    }

    public int getEmbeddedChunks() {
        return embeddedChunks;
    }

    public @Nullable Integer getTotalChunks() {
        return totalChunks;
    }

    public @Nullable String getErrorCode() {
        return errorCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getStartedAt() {
        return startedAt;
    }

    public @Nullable Instant getFinishedAt() {
        return finishedAt;
    }
}
