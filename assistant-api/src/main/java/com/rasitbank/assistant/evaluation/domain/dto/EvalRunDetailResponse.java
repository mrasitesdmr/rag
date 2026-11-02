package com.rasitbank.assistant.evaluation.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunStatus;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Bir değerlendirme çalıştırmasını "Kaçanlar ve sırası değişenler" satırlarıyla birlikte taşır.
 */
public record EvalRunDetailResponse(long evalRunId, EvalRunStatus status, String goldenSetVersion,
        RetrievalStrategy retrievalStrategy, int candidates, int topK, String sourceVersions, int questionCount,
        int measuredCount, int unmatchedCount, int processedCount, @Nullable Integer hitCount,
        @Nullable Integer missedCount, @Nullable Double recallAtK, @Nullable Double mrr, int rerankFallbackCount,
        @Nullable String errorCode, Instant startedAt, @Nullable Instant finishedAt, @Nullable Long durationMs,
        List<EvalQuestionResultResponse> missedAndMovedQuestions) {

    public EvalRunDetailResponse {
        missedAndMovedQuestions = List.copyOf(missedAndMovedQuestions);
    }

    public static EvalRunDetailResponse of(EvalRunResponse run, List<EvalQuestionResultResponse> missedAndMoved) {
        return new EvalRunDetailResponse(run.evalRunId(), run.status(), run.goldenSetVersion(),
                run.retrievalStrategy(), run.candidates(), run.topK(), run.sourceVersions(), run.questionCount(),
                run.measuredCount(), run.unmatchedCount(), run.processedCount(), run.hitCount(), run.missedCount(),
                run.recallAtK(), run.mrr(), run.rerankFallbackCount(), run.errorCode(), run.startedAt(),
                run.finishedAt(), run.durationMs(), missedAndMoved);
    }
}
