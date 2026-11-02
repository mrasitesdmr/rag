package com.rasitbank.assistant.evaluation.service;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunResponse;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRun;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunQuestion;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunQuestionId;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunStatus;
import com.rasitbank.assistant.evaluation.repository.EvalRunQuestionRepository;
import com.rasitbank.assistant.evaluation.repository.EvalRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Değerlendirme çalıştırmasının kaydını adım adım, her adımı kendi kısa transaction'ında yazar: oluşturma, soru sonucu,
 * tamamlama, başarısızlık ve yeniden başlatmada yarım kalanları kapatma.
 */
@Service
public class EvalRunLifecycleService {

    public static final String EVAL_RUN_INTERRUPTED = "EVAL_RUN_INTERRUPTED";

    private final EvalRunRepository evalRunRepository;
    private final EvalRunQuestionRepository evalRunQuestionRepository;
    private final Clock clock;

    public EvalRunLifecycleService(EvalRunRepository evalRunRepository,
            EvalRunQuestionRepository evalRunQuestionRepository, Clock clock) {
        this.evalRunRepository = evalRunRepository;
        this.evalRunQuestionRepository = evalRunQuestionRepository;
        this.clock = clock;
    }

    @Transactional
    public EvalRunResponse createRun(String goldenSetVersion, RetrievalStrategy retrievalStrategy, int candidates,
            int topK, String sourceVersions, List<EvalRunQuestionDraft> questions) {
        int measuredCount = (int) questions.stream().filter(EvalRunQuestionDraft::isMeasured).count();
        EvalRun run = evalRunRepository.saveAndFlush(new EvalRun(goldenSetVersion, retrievalStrategy, candidates,
                topK, sourceVersions, questions.size(), measuredCount, Instant.now(clock)));
        long evalRunId = Objects.requireNonNull(run.getId());
        evalRunQuestionRepository.saveAll(questions.stream()
                .map(question -> new EvalRunQuestion(evalRunId, question.position(), question.category(),
                        question.text(), question.pageNumber(), question.answerText(),
                        question.answerChunkIds().stream().mapToLong(Long::longValue).toArray()))
                .toList());
        return EvalRunResponse.from(run);
    }

    @Transactional
    public void recordQuestionResult(long evalRunId, int position, QuestionRank rank, boolean rerankFallback) {
        EvalRunQuestion question = evalRunQuestionRepository.findById(new EvalRunQuestionId(evalRunId, position))
                .orElseThrow(() -> new IllegalStateException(
                        "Eval run question not found: " + evalRunId + "/" + position));
        question.recordResult(rank.vectorRank(), rank.promptPosition(), rerankFallback);
        getRun(evalRunId).recordProgress(rerankFallback);
    }

    @Transactional
    public EvalRunResponse completeRun(long evalRunId, RetrievalMetricsResult metrics) {
        EvalRun run = getRun(evalRunId);
        run.complete(metrics, Instant.now(clock));
        return EvalRunResponse.from(run);
    }

    @Transactional
    public EvalRunResponse failRun(long evalRunId, String errorCode) {
        EvalRun run = getRun(evalRunId);
        if (run.isRunning()) {
            run.fail(errorCode, Instant.now(clock));
        }
        return EvalRunResponse.from(run);
    }

    @Transactional
    public int failInterruptedRuns() {
        List<EvalRun> runningRuns = evalRunRepository.findAllByStatus(EvalRunStatus.RUNNING);
        Instant now = Instant.now(clock);
        runningRuns.forEach(run -> run.fail(EVAL_RUN_INTERRUPTED, now));
        return runningRuns.size();
    }

    private EvalRun getRun(long evalRunId) {
        return evalRunRepository.findById(evalRunId)
                .orElseThrow(() -> new IllegalStateException("Eval run not found: " + evalRunId));
    }
}
