package com.rasitbank.assistant.evaluation.domain.entity;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.service.RetrievalMetricsResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Golden set'in bir retrieval ölçümüdür: kopyalanan sürüm, strateji, rehber sürümleri, soru sayıları, ilerleme,
 * metrikler, reranker düşüşleri, hata kodu ve süre.
 */
@Entity
@Table(name = "eval_runs")
public class EvalRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EvalRunStatus status;

    @Column(name = "golden_set_version", nullable = false)
    private String goldenSetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "retrieval_strategy", nullable = false)
    private RetrievalStrategy retrievalStrategy;

    @Column(name = "candidates", nullable = false)
    private int candidates;

    @Column(name = "top_k", nullable = false)
    private int topK;

    @Column(name = "source_versions", nullable = false)
    private String sourceVersions;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "measured_count", nullable = false)
    private int measuredCount;

    @Column(name = "processed_count", nullable = false)
    private int processedCount;

    @Column(name = "hit_count")
    private @Nullable Integer hitCount;

    @Column(name = "recall_at_k")
    private @Nullable Double recallAtK;

    @Column(name = "mrr")
    private @Nullable Double mrr;

    @Column(name = "rerank_fallback_count", nullable = false)
    private int rerankFallbackCount;

    @Column(name = "error_code")
    private @Nullable String errorCode;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private @Nullable Instant finishedAt;

    @Column(name = "duration_ms")
    private @Nullable Long durationMs;

    protected EvalRun() {
    }

    public EvalRun(String goldenSetVersion, RetrievalStrategy retrievalStrategy, int candidates, int topK,
            String sourceVersions, int questionCount, int measuredCount, Instant startedAt) {
        this.status = EvalRunStatus.RUNNING;
        this.goldenSetVersion = goldenSetVersion;
        this.retrievalStrategy = retrievalStrategy;
        this.candidates = candidates;
        this.topK = topK;
        this.sourceVersions = sourceVersions;
        this.questionCount = questionCount;
        this.measuredCount = measuredCount;
        this.startedAt = startedAt;
    }

    public void recordProgress(boolean rerankFallback) {
        ensureRunning();
        if (processedCount >= measuredCount) {
            throw new IllegalStateException("All measured questions are already processed: " + id);
        }
        processedCount++;
        if (rerankFallback) {
            rerankFallbackCount++;
        }
    }

    public void complete(RetrievalMetricsResult metrics, Instant finishedAt) {
        ensureRunning();
        this.status = EvalRunStatus.DONE;
        this.hitCount = metrics.hitCount();
        this.recallAtK = metrics.recallAtK();
        this.mrr = metrics.mrr();
        finish(finishedAt);
    }

    public void fail(String errorCode, Instant finishedAt) {
        ensureRunning();
        this.status = EvalRunStatus.FAILED;
        this.errorCode = errorCode;
        finish(finishedAt);
    }

    public boolean isRunning() {
        return status == EvalRunStatus.RUNNING;
    }

    private void finish(Instant finishedAt) {
        this.finishedAt = finishedAt;
        this.durationMs = Math.max(0, Duration.between(startedAt, finishedAt).toMillis());
    }

    private void ensureRunning() {
        if (status != EvalRunStatus.RUNNING) {
            throw new IllegalStateException("Eval run is not running: " + id);
        }
    }

    public @Nullable Long getId() {
        return id;
    }

    public EvalRunStatus getStatus() {
        return status;
    }

    public String getGoldenSetVersion() {
        return goldenSetVersion;
    }

    public RetrievalStrategy getRetrievalStrategy() {
        return retrievalStrategy;
    }

    public int getCandidates() {
        return candidates;
    }

    public int getTopK() {
        return topK;
    }

    public String getSourceVersions() {
        return sourceVersions;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getMeasuredCount() {
        return measuredCount;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public @Nullable Integer getHitCount() {
        return hitCount;
    }

    public @Nullable Double getRecallAtK() {
        return recallAtK;
    }

    public @Nullable Double getMrr() {
        return mrr;
    }

    public int getRerankFallbackCount() {
        return rerankFallbackCount;
    }

    public @Nullable String getErrorCode() {
        return errorCode;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public @Nullable Instant getFinishedAt() {
        return finishedAt;
    }

    public @Nullable Long getDurationMs() {
        return durationMs;
    }
}
