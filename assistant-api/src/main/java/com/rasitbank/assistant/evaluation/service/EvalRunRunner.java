package com.rasitbank.assistant.evaluation.service;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunResponse;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunQuestion;
import com.rasitbank.assistant.evaluation.repository.EvalRunQuestionRepository;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.service.ContextRetriever;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Bir değerlendirme çalıştırmasını arka planda yürütür: ölçülen her soru için sadece retrieval (ve rerank) yapar, doğru
 * parçanın yerini kaydeder ve sonunda metrikleri yazar; dil modelini çağırmaz.
 */
@Service
public class EvalRunRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalRunRunner.class);

    private static final String RETRIEVAL_UNAVAILABLE = "RETRIEVAL_UNAVAILABLE";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private final EvalRunQuestionRepository evalRunQuestionRepository;
    private final ContextRetriever contextRetriever;
    private final AnswerChunkRanker answerChunkRanker;
    private final EvalRunLifecycleService evalRunLifecycleService;

    public EvalRunRunner(EvalRunQuestionRepository evalRunQuestionRepository, ContextRetriever contextRetriever,
            AnswerChunkRanker answerChunkRanker, EvalRunLifecycleService evalRunLifecycleService) {
        this.evalRunQuestionRepository = evalRunQuestionRepository;
        this.contextRetriever = contextRetriever;
        this.answerChunkRanker = answerChunkRanker;
        this.evalRunLifecycleService = evalRunLifecycleService;
    }

    public void runEvalRun(long evalRunId) {
        try {
            List<EvalRunQuestion> questions = evalRunQuestionRepository.findMeasuredByEvalRunIdOrderByPosition(
                    evalRunId);
            List<Integer> promptPositions = new ArrayList<>(questions.size());
            for (EvalRunQuestion question : questions) {
                RetrievedContext context = contextRetriever.retrieveCandidatesAndContext(question.getText());
                QuestionRank rank = answerChunkRanker.rank(context, question.getAnswerChunkIds());
                boolean rerankFallback = context.strategy() == RetrievalStrategy.RERANK_FALLBACK;
                evalRunLifecycleService.recordQuestionResult(evalRunId, question.getPosition(), rank, rerankFallback);
                promptPositions.add(rank.promptPosition());
            }
            EvalRunResponse run = evalRunLifecycleService.completeRun(evalRunId,
                    RetrievalMetrics.evaluate(promptPositions));
            log.info("Eval run finished evalRunId={} status={} recallAtK={} mrr={} rerankFallbackCount={} durationMs={}",
                    evalRunId, run.status(), run.recallAtK(), run.mrr(), run.rerankFallbackCount(), run.durationMs());
        } catch (ServiceUnavailableException exception) {
            failRun(evalRunId, RETRIEVAL_UNAVAILABLE.equals(exception.getErrorCode()) ? RETRIEVAL_UNAVAILABLE
                    : INTERNAL_ERROR);
        } catch (RuntimeException exception) {
            log.error("Eval run stopped by an unexpected error evalRunId={}", evalRunId, exception);
            failRun(evalRunId, INTERNAL_ERROR);
        }
    }

    private void failRun(long evalRunId, String errorCode) {
        try {
            EvalRunResponse run = evalRunLifecycleService.failRun(evalRunId, errorCode);
            log.warn("Eval run failed evalRunId={} errorCode={} processedCount={}", evalRunId, errorCode,
                    run.processedCount());
        } catch (RuntimeException exception) {
            log.error("Eval run failure could not be recorded evalRunId={} errorCode={}", evalRunId, errorCode,
                    exception);
        }
    }
}
