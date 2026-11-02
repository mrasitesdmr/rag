package com.rasitbank.assistant.evaluation.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.domain.dto.EvalOverviewResponse;
import com.rasitbank.assistant.evaluation.domain.dto.EvalQuestionResultResponse;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunDetailResponse;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunResponse;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRun;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunStatus;
import com.rasitbank.assistant.evaluation.domain.entity.GoldenQuestion;
import com.rasitbank.assistant.evaluation.repository.EvalRunQuestionRepository;
import com.rasitbank.assistant.evaluation.repository.EvalRunRepository;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.retrieval.client.ChunkMatchesResponse;
import com.rasitbank.assistant.retrieval.client.ChunkTextMatch;
import com.rasitbank.assistant.retrieval.client.MatchedChunk;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Değerlendirme çalıştırmasını başlatır (kontroller, cevap cümlelerinin parçalarını bulma, kayıt, arka plan işi) ve
 * Değerlendirme ekranı için çalıştırmaları okur.
 */
@Service
public class EvalRunService {

    public static final int MISSED_AND_MOVED_LIMIT = 24;

    private static final Logger log = LoggerFactory.getLogger(EvalRunService.class);

    private static final String SINGLE_RUNNING_INDEX = "ux_eval_runs_single_running";
    private static final String SOURCE_VERSION_SEPARATOR = ", ";

    private final GoldenSetService goldenSetService;
    private final EvalRunRepository evalRunRepository;
    private final EvalRunQuestionRepository evalRunQuestionRepository;
    private final EvalRunLifecycleService evalRunLifecycleService;
    private final EvalRunRunner evalRunRunner;
    private final RetrievalClient retrievalClient;
    private final TaskExecutor evalExecutor;
    private final AssistantProperties properties;

    public EvalRunService(GoldenSetService goldenSetService, EvalRunRepository evalRunRepository,
            EvalRunQuestionRepository evalRunQuestionRepository, EvalRunLifecycleService evalRunLifecycleService,
            EvalRunRunner evalRunRunner, RetrievalClient retrievalClient,
            @Qualifier("evalExecutor") TaskExecutor evalExecutor, AssistantProperties properties) {
        this.goldenSetService = goldenSetService;
        this.evalRunRepository = evalRunRepository;
        this.evalRunQuestionRepository = evalRunQuestionRepository;
        this.evalRunLifecycleService = evalRunLifecycleService;
        this.evalRunRunner = evalRunRunner;
        this.retrievalClient = retrievalClient;
        this.evalExecutor = evalExecutor;
        this.properties = properties;
    }

    public EvalRunResponse startEvalRun() {
        if (properties.generation().configuredMode() == GenerationMode.LLM_ONLY) {
            throw new ConflictException("EVAL_NOT_AVAILABLE_IN_LLM_ONLY",
                    "Retrieval is not used in LLM-only generation mode");
        }
        GoldenSetSnapshot goldenSet = goldenSetService.findGoldenSetSnapshot()
                .orElseThrow(() -> new ConflictException("GOLDEN_SET_NOT_LOADED", "No golden set has been loaded"));
        if (evalRunRepository.existsByStatus(EvalRunStatus.RUNNING)) {
            throw runInProgress();
        }
        List<GoldenQuestion> questions = goldenSet.questions();
        ChunkMatchesResponse chunkMatches = retrievalClient.findChunkMatches(
                questions.stream().map(GoldenQuestion::getAnswerText).toList());
        List<ChunkTextMatch> matches = chunkMatches.matchesOrEmpty();
        List<EvalRunQuestionDraft> drafts = new ArrayList<>(questions.size());
        Set<String> sourceVersions = new TreeSet<>();
        for (int index = 0; index < questions.size(); index++) {
            GoldenQuestion question = questions.get(index);
            List<MatchedChunk> chunks = matches.get(index).chunksOrEmpty();
            chunks.forEach(chunk -> sourceVersions.add(chunk.version()));
            drafts.add(new EvalRunQuestionDraft(question.getPosition(), question.getCategory(), question.getText(),
                    question.getPageNumber(), question.getAnswerText(),
                    chunks.stream().map(MatchedChunk::chunkId).toList()));
        }
        int measuredCount = (int) drafts.stream().filter(EvalRunQuestionDraft::isMeasured).count();
        if (measuredCount == 0) {
            throw new ConflictException("GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE",
                    "No answer text of the golden set is in the knowledge base");
        }
        AssistantProperties.Retrieval retrieval = properties.retrieval();
        RetrievalStrategy strategy = retrieval.configuredStrategy();
        EvalRunResponse run = createRun(goldenSet.version(), strategy, retrieval.candidates(), retrieval.topK(),
                String.join(SOURCE_VERSION_SEPARATOR, sourceVersions), drafts);
        submit(run.evalRunId());
        log.info("Eval run started evalRunId={} goldenSetVersion={} strategy={} questionCount={} measuredCount={}",
                run.evalRunId(), run.goldenSetVersion(), strategy, run.questionCount(), run.measuredCount());
        return run;
    }

    @Transactional(readOnly = true)
    public Page<EvalRunResponse> listEvalRuns(int page, int size) {
        return evalRunRepository.findAllByOrderByStartedAtDescIdDesc(PageRequest.of(page, size))
                .map(EvalRunResponse::from);
    }

    @Transactional(readOnly = true)
    public EvalRunDetailResponse getEvalRun(long evalRunId) {
        EvalRun run = evalRunRepository.findById(evalRunId)
                .orElseThrow(() -> new NotFoundException("EVAL_RUN_NOT_FOUND", "Eval run not found: " + evalRunId));
        return toDetail(run);
    }

    @Transactional(readOnly = true)
    public EvalOverviewResponse getEvalOverview() {
        @Nullable EvalRunResponse runningRun = evalRunRepository.findFirstByStatus(EvalRunStatus.RUNNING)
                .map(EvalRunResponse::from)
                .orElse(null);
        @Nullable EvalRunResponse lastFinishedRun = evalRunRepository
                .findFirstByStatusInOrderByStartedAtDescIdDesc(List.of(EvalRunStatus.DONE, EvalRunStatus.FAILED))
                .map(EvalRunResponse::from)
                .orElse(null);
        @Nullable EvalRunDetailResponse lastDoneRun = evalRunRepository
                .findFirstByStatusOrderByStartedAtDescIdDesc(EvalRunStatus.DONE)
                .map(this::toDetail)
                .orElse(null);
        return new EvalOverviewResponse(goldenSetService.findGoldenSet().orElse(null), runningRun, lastFinishedRun,
                lastDoneRun);
    }

    private EvalRunResponse createRun(String goldenSetVersion, RetrievalStrategy strategy, int candidates, int topK,
            String sourceVersions, List<EvalRunQuestionDraft> drafts) {
        try {
            return evalRunLifecycleService.createRun(goldenSetVersion, strategy, candidates, topK, sourceVersions,
                    drafts);
        } catch (DataIntegrityViolationException exception) {
            if (isSingleRunningViolation(exception)) {
                throw runInProgress();
            }
            throw exception;
        }
    }

    private void submit(long evalRunId) {
        try {
            evalExecutor.execute(() -> evalRunRunner.runEvalRun(evalRunId));
        } catch (TaskRejectedException exception) {
            evalRunLifecycleService.failRun(evalRunId, "INTERNAL_ERROR");
            throw exception;
        }
    }

    private EvalRunDetailResponse toDetail(EvalRun run) {
        EvalRunResponse response = EvalRunResponse.from(run);
        List<EvalQuestionResultResponse> missedAndMoved = run.getStatus() != EvalRunStatus.DONE ? List.of()
                : evalRunQuestionRepository.findMissedAndMoved(response.evalRunId(),
                                PageRequest.of(0, MISSED_AND_MOVED_LIMIT)).stream()
                        .map(EvalQuestionResultResponse::from)
                        .toList();
        return EvalRunDetailResponse.of(response, missedAndMoved);
    }

    private static boolean isSingleRunningViolation(DataIntegrityViolationException exception) {
        String message = Objects.requireNonNullElse(exception.getMostSpecificCause().getMessage(), "");
        return message.contains(SINGLE_RUNNING_INDEX);
    }

    private static ConflictException runInProgress() {
        return new ConflictException("EVAL_RUN_IN_PROGRESS", "Another evaluation run is running");
    }
}
