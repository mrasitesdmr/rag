package com.rasitbank.assistant.evaluation.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRun;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunStatus;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Bir değerlendirme çalıştırmasını "Çalıştırmalar" satırı ve süren ölçümün ilerlemesi olarak taşır.
 */
public record EvalRunResponse(long evalRunId, EvalRunStatus status, String goldenSetVersion,
        RetrievalStrategy retrievalStrategy, int candidates, int topK, String sourceVersions, int questionCount,
        int measuredCount, int unmatchedCount, int processedCount, @Nullable Integer hitCount,
        @Nullable Integer missedCount, @Nullable Double recallAtK, @Nullable Double mrr, int rerankFallbackCount,
        @Nullable String errorCode, Instant startedAt, @Nullable Instant finishedAt, @Nullable Long durationMs) {

    public static EvalRunResponse from(EvalRun run) {
        boolean isDone = run.getStatus() == EvalRunStatus.DONE;
        Integer hitCount = isDone ? run.getHitCount() : null;
        Integer missedCount = hitCount == null ? null : run.getMeasuredCount() - hitCount;
        return new EvalRunResponse(Objects.requireNonNull(run.getId()), run.getStatus(), run.getGoldenSetVersion(),
                run.getRetrievalStrategy(), run.getCandidates(), run.getTopK(), run.getSourceVersions(),
                run.getQuestionCount(), run.getMeasuredCount(), run.getQuestionCount() - run.getMeasuredCount(),
                run.getProcessedCount(), hitCount, missedCount, isDone ? run.getRecallAtK() : null,
                isDone ? run.getMrr() : null, run.getRerankFallbackCount(), run.getErrorCode(), run.getStartedAt(),
                run.getFinishedAt(), run.getDurationMs());
    }
}
