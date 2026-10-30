package com.rasitbank.retrieval.ingestion.service;

import com.rasitbank.retrieval.chunk.domain.entity.Chunk;
import com.rasitbank.retrieval.chunk.repository.ChunkRepository;
import com.rasitbank.retrieval.config.EmbeddingProperties;
import com.rasitbank.retrieval.ingestion.chunking.ChunkDraft;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.parsing.ParsedDocument;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobRepository;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceVersion;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.source.repository.SourceVersionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * İçe alma işinin her aşama geçişini kendi kısa transaction'ında kaydeder; parçaları ve sürümü tek transaction'da yazar.
 */
@Service
public class IngestionJobLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(IngestionJobLifecycleService.class);

    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private final IngestionJobRepository ingestionJobRepository;
    private final SourceRepository sourceRepository;
    private final SourceVersionRepository sourceVersionRepository;
    private final ChunkRepository chunkRepository;
    private final EmbeddingProperties embeddingProperties;
    private final Clock clock;

    public IngestionJobLifecycleService(IngestionJobRepository ingestionJobRepository,
            SourceRepository sourceRepository, SourceVersionRepository sourceVersionRepository,
            ChunkRepository chunkRepository, EmbeddingProperties embeddingProperties, Clock clock) {
        this.ingestionJobRepository = ingestionJobRepository;
        this.sourceRepository = sourceRepository;
        this.sourceVersionRepository = sourceVersionRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingProperties = embeddingProperties;
        this.clock = clock;
    }

    @Transactional
    public IngestionJob start(long jobId) {
        IngestionJob job = getJob(jobId);
        job.start(Instant.now(clock));
        log.info("Ingestion job started jobId={} workspaceId={} sourceId={}", jobId, job.getWorkspaceId(),
                job.getSourceId());
        log.info("Ingestion stage changed jobId={} stage={}", jobId, job.getStage());
        return job;
    }

    @Transactional
    public void moveTo(long jobId, IngestionStage stage) {
        getJob(jobId).moveTo(stage);
        log.info("Ingestion stage changed jobId={} stage={}", jobId, stage);
    }

    @Transactional
    public void recordTotalChunks(long jobId, int totalChunks) {
        getJob(jobId).recordTotalChunks(totalChunks);
    }

    @Transactional
    public void recordEmbeddingProgress(long jobId, int embeddedChunks) {
        getJob(jobId).recordEmbeddingProgress(embeddedChunks);
    }

    @Transactional
    public void complete(long jobId, ParsedDocument document, List<ChunkDraft> drafts, List<float[]> embeddings) {
        if (drafts.size() != embeddings.size()) {
            throw new IllegalArgumentException("Got " + embeddings.size() + " embeddings for " + drafts.size()
                    + " chunks");
        }
        IngestionJob job = getJob(jobId);
        Source source = sourceRepository.findById(job.getSourceId())
                .orElseThrow(() -> new IllegalStateException("Source not found: " + job.getSourceId()));
        Instant now = Instant.now(clock);
        SourceVersion sourceVersion = sourceVersionRepository.save(SourceVersion.of(source.getId(), jobId,
                job.getVersion(), job.getFileName(), job.getFileSizeBytes(), document.pageCount(),
                document.chapterCount(), drafts.size(), now));
        long sourceVersionId = sourceVersion.getId();
        List<Chunk> chunks = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            chunks.add(Chunk.of(job.getWorkspaceId(), source.getId(), sourceVersionId, drafts.get(index),
                    embeddings.get(index), embeddingProperties.modelLabel(), now));
        }
        chunkRepository.saveAll(chunks);
        source.activate(sourceVersionId);
        job.complete(now);
        log.info("Ingestion job finished jobId={} sourceId={} sourceVersionId={} chunkCount={} durationMs={}", jobId,
                source.getId(), sourceVersionId, chunks.size(), durationMs(job));
    }

    @Transactional
    public void fail(long jobId, String errorCode) {
        IngestionJob job = getJob(jobId);
        job.fail(errorCode, Instant.now(clock));
        if (INTERNAL_ERROR.equals(errorCode)) {
            log.error("Ingestion job failed jobId={} failedStage={} errorCode={} durationMs={}", jobId,
                    job.getFailedStage(), errorCode, durationMs(job));
        } else {
            log.warn("Ingestion job failed jobId={} failedStage={} errorCode={} durationMs={}", jobId,
                    job.getFailedStage(), errorCode, durationMs(job));
        }
    }

    private IngestionJob getJob(long jobId) {
        return ingestionJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Ingestion job not found: " + jobId));
    }

    private static long durationMs(IngestionJob job) {
        Instant startedAt = Objects.requireNonNullElse(job.getStartedAt(), job.getCreatedAt());
        Instant finishedAt = Objects.requireNonNullElse(job.getFinishedAt(), startedAt);
        return Duration.between(startedAt, finishedAt).toMillis();
    }
}
