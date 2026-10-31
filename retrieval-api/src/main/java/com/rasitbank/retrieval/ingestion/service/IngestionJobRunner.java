package com.rasitbank.retrieval.ingestion.service;

import com.rasitbank.retrieval.config.EmbeddingProperties;
import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.embedding.client.EmbeddingClient;
import com.rasitbank.retrieval.exception.BaseException;
import com.rasitbank.retrieval.ingestion.chunking.ChunkDraft;
import com.rasitbank.retrieval.ingestion.chunking.TextChunker;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.parsing.ParsedDocument;
import com.rasitbank.retrieval.ingestion.parsing.PdfDocumentParser;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * İçe alma işini arka planda aşama aşama yürütür: PDF'i okur, parçalar, gruplar hâlinde embed eder ve yazdırır.
 */
@Component
public class IngestionJobRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionJobRunner.class);

    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private final ExecutorService ingestionExecutor;
    private final IngestionJobLifecycleService lifecycle;
    private final WorkspaceService workspaceService;
    private final PdfDocumentParser parser;
    private final TextChunker chunker;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties embeddingProperties;
    private final IngestionProperties ingestionProperties;

    public IngestionJobRunner(ExecutorService ingestionExecutor, IngestionJobLifecycleService lifecycle,
            WorkspaceService workspaceService, PdfDocumentParser parser, TextChunker chunker,
            EmbeddingClient embeddingClient, EmbeddingProperties embeddingProperties,
            IngestionProperties ingestionProperties) {
        this.ingestionExecutor = ingestionExecutor;
        this.lifecycle = lifecycle;
        this.workspaceService = workspaceService;
        this.parser = parser;
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.embeddingProperties = embeddingProperties;
        this.ingestionProperties = ingestionProperties;
    }

    public void submit(long jobId, byte[] content) {
        try {
            ingestionExecutor.execute(() -> run(jobId, content));
        } catch (RejectedExecutionException exception) {
            log.warn("Ingestion job not submitted, executor is shut down jobId={}", jobId);
        }
    }

    public void run(long jobId, byte[] content) {
        try {
            IngestionJob job = lifecycle.start(jobId);
            workspaceService.ensureEmbeddingModelMatches(job.getWorkspaceId(), embeddingProperties.modelLabel());
            ParsedDocument document = parser.parse(content);
            log.info("Document parsed jobId={} pageCount={} chapterCount={} sectionCount={} questionCount={}"
                    + " removedMarginLines={}", jobId, document.pageCount(), document.chapterCount(),
                    document.sections().size(), document.questionCount(), document.removedMarginLines());
            lifecycle.moveTo(jobId, IngestionStage.CHUNKING);
            List<ChunkDraft> drafts = chunker.chunk(document.sections());
            log.info("Document chunked jobId={} chunkCount={} avgTokens={}", jobId, drafts.size(),
                    averageTokens(drafts));
            lifecycle.recordTotalChunks(jobId, drafts.size());
            lifecycle.moveTo(jobId, IngestionStage.EMBEDDING);
            List<float[]> embeddings = embedInBatches(jobId, drafts);
            lifecycle.moveTo(jobId, IngestionStage.WRITING);
            lifecycle.complete(jobId, document, drafts, embeddings);
        } catch (BaseException exception) {
            failJob(jobId, exception.getErrorCode());
        } catch (RuntimeException exception) {
            log.error("Ingestion job crashed jobId={}", jobId, exception);
            failJob(jobId, INTERNAL_ERROR);
        }
    }

    private List<float[]> embedInBatches(long jobId, List<ChunkDraft> drafts) {
        int batchSize = ingestionProperties.embeddingBatchSize();
        int batchCount = Math.ceilDiv(drafts.size(), batchSize);
        List<float[]> embeddings = new ArrayList<>(drafts.size());
        for (int batch = 0; batch < batchCount; batch++) {
            long startNanos = System.nanoTime();
            List<String> inputs = drafts.subList(batch * batchSize, Math.min((batch + 1) * batchSize, drafts.size()))
                    .stream()
                    .map(ChunkDraft::embeddingInput)
                    .toList();
            embeddings.addAll(embeddingClient.embed(inputs));
            lifecycle.recordEmbeddingProgress(jobId, embeddings.size());
            log.info("Embedding batch done jobId={} batch={}/{} chunkCount={} durationMs={}", jobId, batch + 1,
                    batchCount, inputs.size(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
        }
        return embeddings;
    }

    private void failJob(long jobId, String errorCode) {
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Ingestion job interrupted by shutdown jobId={}", jobId);
            return;
        }
        try {
            lifecycle.fail(jobId, errorCode);
        } catch (RuntimeException exception) {
            log.error("Ingestion job could not be marked failed jobId={} errorCode={}", jobId, errorCode, exception);
        }
    }

    private static long averageTokens(List<ChunkDraft> drafts) {
        return Math.round(drafts.stream().mapToInt(ChunkDraft::tokenCount).average().orElse(0));
    }
}
