package com.rasitbank.retrieval.source.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Bir kaynağın başarıyla içe alınmış bir sürümüdür; dosyanın özelliklerini ve son senkron zamanını saklar.
 */
@Entity
@Table(name = "source_versions")
public class SourceVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false)
    private long sourceId;

    @Column(name = "ingestion_job_id", nullable = false)
    private long ingestionJobId;

    @Column(nullable = false)
    private String version;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "chapter_count", nullable = false)
    private int chapterCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SourceVersion() {
    }

    private SourceVersion(long sourceId, long ingestionJobId, String version, String fileName, long fileSizeBytes,
            int pageCount, int chapterCount, int chunkCount, Instant syncedAt) {
        this.sourceId = sourceId;
        this.ingestionJobId = ingestionJobId;
        this.version = version;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.pageCount = pageCount;
        this.chapterCount = chapterCount;
        this.chunkCount = chunkCount;
        this.syncedAt = syncedAt;
        this.createdAt = syncedAt;
    }

    public static SourceVersion of(long sourceId, long ingestionJobId, String version, String fileName,
            long fileSizeBytes, int pageCount, int chapterCount, int chunkCount, Instant syncedAt) {
        return new SourceVersion(sourceId, ingestionJobId, version, fileName, fileSizeBytes, pageCount, chapterCount,
                chunkCount, syncedAt);
    }

    public Long getId() {
        return id;
    }

    public long getSourceId() {
        return sourceId;
    }

    public long getIngestionJobId() {
        return ingestionJobId;
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

    public int getPageCount() {
        return pageCount;
    }

    public int getChapterCount() {
        return chapterCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
