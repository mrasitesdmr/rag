package com.rasitbank.assistant.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunResponse;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunStatus;
import com.rasitbank.assistant.evaluation.domain.entity.GoldenQuestion;
import com.rasitbank.assistant.evaluation.repository.EvalRunQuestionRepository;
import com.rasitbank.assistant.evaluation.repository.EvalRunRepository;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.client.ChunkMatchesResponse;
import com.rasitbank.assistant.retrieval.client.ChunkTextMatch;
import com.rasitbank.assistant.retrieval.client.MatchedChunk;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.support.TestProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Ölçüm başlatmadaki kontrollerin sırasını, hangi hatada neyin çağrılmadığını ve ölçülemeyen soruların sayımını doğrular.
 */
class EvalRunServiceTest {

    private final GoldenSetService goldenSetService = mock(GoldenSetService.class);
    private final EvalRunRepository evalRunRepository = mock(EvalRunRepository.class);
    private final EvalRunQuestionRepository evalRunQuestionRepository = mock(EvalRunQuestionRepository.class);
    private final EvalRunLifecycleService lifecycle = mock(EvalRunLifecycleService.class);
    private final EvalRunRunner runner = mock(EvalRunRunner.class);
    private final RetrievalClient retrievalClient = mock(RetrievalClient.class);
    private final TaskExecutor executor = mock(TaskExecutor.class);

    private EvalRunService service(AssistantProperties properties) {
        return new EvalRunService(goldenSetService, evalRunRepository, evalRunQuestionRepository, lifecycle, runner,
                retrievalClient, executor, properties);
    }

    private static List<GoldenQuestion> questions(int count) {
        List<GoldenQuestion> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new GoldenQuestion(1, i, "Kartlar", "Soru " + i + "?", "soru " + i, 10 + i,
                    "Cevap cümlesi numara " + i + " burada geçer."));
        }
        return list;
    }

    private static ChunkTextMatch found(long chunkId) {
        return new ChunkTextMatch("t", List.of(new MatchedChunk(chunkId, 10001, "Rehber", "2026.10", 19)));
    }

    private static ChunkTextMatch missing() {
        return new ChunkTextMatch("t", List.of());
    }

    private static EvalRunResponse createdRun() {
        return new EvalRunResponse(7, EvalRunStatus.RUNNING, "rehber-golden-v1", RetrievalStrategy.RERANK, 20, 5,
                "2026.10", 4, 3, 1, 0, null, null, null, null, 0, null, Instant.parse("2026-11-02T10:00:00Z"), null, null);
    }

    private void givenGoldenSet(int count) {
        given(goldenSetService.findGoldenSetSnapshot())
                .willReturn(Optional.of(new GoldenSetSnapshot("rehber-golden-v1", questions(count))));
    }

    @Test
    void givenLlmOnlyMode_whenStartEvalRun_thenThrowsEvalNotAvailableInLlmOnly() {
        // Act + Assert
        assertThatThrownBy(() -> service(TestProperties.withMode("llm", "rerank")).startEvalRun())
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("EVAL_NOT_AVAILABLE_IN_LLM_ONLY"));
        verifyNoInteractions(retrievalClient, lifecycle, executor);
    }

    @Test
    void givenNoGoldenSet_whenStartEvalRun_thenThrowsGoldenSetNotLoaded() {
        // Arrange
        given(goldenSetService.findGoldenSetSnapshot()).willReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service(TestProperties.defaults()).startEvalRun())
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("GOLDEN_SET_NOT_LOADED"));
        verifyNoInteractions(retrievalClient, lifecycle);
    }

    @Test
    void givenRunningRun_whenStartEvalRun_thenThrowsEvalRunInProgress() {
        // Arrange
        givenGoldenSet(3);
        given(evalRunRepository.existsByStatus(EvalRunStatus.RUNNING)).willReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service(TestProperties.defaults()).startEvalRun())
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("EVAL_RUN_IN_PROGRESS"));
        verifyNoInteractions(retrievalClient, lifecycle, executor);
    }

    @Test
    void givenNoAnswerTextFound_whenStartEvalRun_thenThrowsGoldenSetNotInKnowledgeBase() {
        // Arrange
        givenGoldenSet(2);
        given(retrievalClient.findChunkMatches(anyList()))
                .willReturn(new ChunkMatchesResponse(List.of(missing(), missing())));

        // Act + Assert
        assertThatThrownBy(() -> service(TestProperties.defaults()).startEvalRun())
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE"));
        verify(lifecycle, never()).createRun(anyString(), any(), anyInt(), anyInt(), anyString(), anyList());
        verifyNoInteractions(executor);
    }

    @Test
    void givenChunkMatchFails_whenStartEvalRun_thenThrowsRetrievalUnavailable() {
        // Arrange
        givenGoldenSet(2);
        given(retrievalClient.findChunkMatches(anyList()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        // Act + Assert
        assertThatThrownBy(() -> service(TestProperties.defaults()).startEvalRun())
                .isInstanceOfSatisfying(ServiceUnavailableException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("RETRIEVAL_UNAVAILABLE"));
        verifyNoInteractions(lifecycle, executor);
    }

    @Test
    void givenThreeOfFourAnswerTextsFound_whenStartEvalRun_thenMeasuresThreeAndOneUnmatched() {
        // Arrange
        givenGoldenSet(4);
        given(retrievalClient.findChunkMatches(anyList())).willReturn(
                new ChunkMatchesResponse(List.of(found(151), missing(), found(160), found(161))));
        given(lifecycle.createRun(anyString(), any(), anyInt(), anyInt(), anyString(), anyList()))
                .willReturn(createdRun());

        // Act
        EvalRunResponse run = service(TestProperties.defaults()).startEvalRun();

        // Assert
        assertThat(run.evalRunId()).isEqualTo(7);
        ArgumentCaptor<List<EvalRunQuestionDraft>> drafts = ArgumentCaptor.captor();
        verify(lifecycle).createRun(eq("rehber-golden-v1"), eq(RetrievalStrategy.RERANK), eq(20), eq(5), eq("2026.10"),
                drafts.capture());
        assertThat(drafts.getValue()).hasSize(4);
        assertThat(drafts.getValue().stream().map(EvalRunQuestionDraft::isMeasured)).containsExactly(true, false, true, true);
        assertThat(drafts.getValue().get(0).answerChunkIds()).containsExactly(151L);
        assertThat(drafts.getValue().get(1).answerChunkIds()).isEmpty();
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    void givenMatchesFromTwoVersions_whenStartEvalRun_thenSourceVersionsSortedDistinctJoined() {
        // Arrange
        givenGoldenSet(3);
        given(retrievalClient.findChunkMatches(anyList())).willReturn(new ChunkMatchesResponse(List.of(
                new ChunkTextMatch("a", List.of(new MatchedChunk(1, 1, "R", "2026.10", 1))),
                new ChunkTextMatch("b", List.of(new MatchedChunk(2, 2, "S", "2026.09", 1))),
                new ChunkTextMatch("c", List.of(new MatchedChunk(3, 1, "R", "2026.10", 1))))));
        given(lifecycle.createRun(anyString(), any(), anyInt(), anyInt(), anyString(), anyList()))
                .willReturn(createdRun());

        // Act
        service(TestProperties.defaults()).startEvalRun();

        // Assert
        verify(lifecycle).createRun(anyString(), any(), anyInt(), anyInt(), eq("2026.09, 2026.10"), anyList());
    }

    @Test
    void givenSingleRunningIndexViolation_whenStartEvalRun_thenThrowsEvalRunInProgress() {
        // Arrange
        givenGoldenSet(1);
        given(retrievalClient.findChunkMatches(anyList())).willReturn(new ChunkMatchesResponse(List.of(found(151))));
        given(lifecycle.createRun(anyString(), any(), anyInt(), anyInt(), anyString(), anyList()))
                .willThrow(new DataIntegrityViolationException("x",
                        new RuntimeException("duplicate key value violates unique constraint \"ux_eval_runs_single_running\"")));

        // Act + Assert
        assertThatThrownBy(() -> service(TestProperties.defaults()).startEvalRun())
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("EVAL_RUN_IN_PROGRESS"));
        verifyNoInteractions(executor);
    }

    @Test
    void givenOtherConstraintViolation_whenStartEvalRun_thenOriginalExceptionPropagates() {
        // Arrange
        givenGoldenSet(1);
        given(retrievalClient.findChunkMatches(anyList())).willReturn(new ChunkMatchesResponse(List.of(found(151))));
        given(lifecycle.createRun(anyString(), any(), anyInt(), anyInt(), anyString(), anyList()))
                .willThrow(new DataIntegrityViolationException("x", new RuntimeException("null value in column")));

        // Act + Assert
        assertThatThrownBy(() -> service(TestProperties.defaults()).startEvalRun())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenTopKStrategy_whenStartEvalRun_thenRunCreatedWithTopK() {
        // Arrange
        givenGoldenSet(1);
        given(retrievalClient.findChunkMatches(anyList())).willReturn(new ChunkMatchesResponse(List.of(found(151))));
        given(lifecycle.createRun(anyString(), any(), anyInt(), anyInt(), anyString(), anyList()))
                .willReturn(createdRun());

        // Act
        service(TestProperties.withStrategy("top5")).startEvalRun();

        // Assert
        verify(lifecycle).createRun(anyString(), eq(RetrievalStrategy.TOP_K), eq(20), eq(5), anyString(), anyList());
    }
}
