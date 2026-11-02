package com.rasitbank.assistant.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.domain.dto.EvalOverviewResponse;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunDetailResponse;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunResponse;
import com.rasitbank.assistant.evaluation.domain.dto.GoldenQuestionRequest;
import com.rasitbank.assistant.evaluation.domain.dto.ReplaceGoldenSetRequest;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunStatus;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.client.ChunkMatchesResponse;
import com.rasitbank.assistant.retrieval.client.ChunkTextMatch;
import com.rasitbank.assistant.retrieval.client.MatchedChunk;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.retrieval.service.ContextRetriever;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import com.rasitbank.assistant.support.ContextFixtures;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Golden set yükleme ve değerlendirme çalıştırmasını gerçek PostgreSQL'e karşı doğrular: sürüm çakışması, kopyalanan
 * sorular, tek çalışan ölçüm kuralı, hata kayıtları, süre ve ekranın okuması. Retrieval ve dil modeli sahtedir.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EvaluationIntegrationTest.AdjustableClockConfiguration.class)
class EvaluationIntegrationTest extends PostgresIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-11-02T10:00:00Z");

    /** Testin ilerletebildiği saat; iki ayrı an gerektiren süre ve yükleme zamanı testleri içindir. */
    static final class AdjustableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(T0);

        void set(Instant instant) {
            now.set(instant);
        }

        void advance(Duration duration) {
            now.updateAndGet(current -> current.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AdjustableClockConfiguration {
        @Bean
        @Primary
        AdjustableClock adjustableClock() {
            return new AdjustableClock();
        }
    }

    @Autowired
    private AdjustableClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private GoldenSetService goldenSetService;

    @Autowired
    private EvalRunService evalRunService;

    @Autowired
    private EvalRunRunner evalRunRunner;

    @Autowired
    private EvalRunLifecycleService lifecycleService;

    @Autowired
    private EvalRunStartupListener startupListener;

    @MockitoBean
    private RetrievalClient retrievalClient;

    @MockitoBean
    private ContextRetriever contextRetriever;

    @MockitoBean
    private ChatModel chatModel;

    @BeforeEach
    void cleanAndResetClock() {
        jdbc.execute("truncate eval_runs, golden_sets restart identity cascade");
        reset(retrievalClient, contextRetriever, chatModel);
        clock.set(T0);
    }

    private static String answerOf(int position) {
        return "Cevap cümlesi numara " + position + " rehberde burada geçer.";
    }

    private static ReplaceGoldenSetRequest goldenSet(String version, int count) {
        List<GoldenQuestionRequest> questions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            questions.add(new GoldenQuestionRequest("Kartlar", "Soru " + i + "?", i, answerOf(i)));
        }
        return new ReplaceGoldenSetRequest(version, questions);
    }

    /** Her sorunun cevap cümlesi, kimliği 100 + sıra olan tek parçada bulunur; fixture'daki chunkId = 100 + vector sırası. */
    private void stubEveryAnswerFoundInChunkOfSamePosition(int count) {
        List<ChunkTextMatch> matches = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            matches.add(new ChunkTextMatch(answerOf(i), List.of(new MatchedChunk(100L + i, 10001, "Rehber", "2026.10", 19))));
        }
        given(retrievalClient.findChunkMatches(anyList())).willReturn(new ChunkMatchesResponse(matches));
    }

    private static RetrievedContext asFallback(RetrievedContext context) {
        return new RetrievedContext(RetrievalStrategy.RERANK_FALLBACK, context.candidates(), context.topK(),
                context.candidateChunks(), context.contextChunks(), context.retrievalLatencyMs(), null);
    }

    private EvalRunDetailResponse awaitFinished(long evalRunId) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(50)).until(
                () -> evalRunService.getEvalRun(evalRunId).status() != EvalRunStatus.RUNNING);
        return evalRunService.getEvalRun(evalRunId);
    }

    private long createRunWithoutExecutor(String version, int questionCount) {
        List<EvalRunQuestionDraft> drafts = new ArrayList<>();
        for (int i = 1; i <= questionCount; i++) {
            drafts.add(new EvalRunQuestionDraft(i, "Kartlar", "Soru " + i + "?", i, answerOf(i), List.of(100L + i)));
        }
        return lifecycleService.createRun(version, RetrievalStrategy.RERANK, 20, 5, "2026.10", drafts).evalRunId();
    }

    @Test
    void givenSameVersionDifferentContent_whenReplaceGoldenSet_thenConflictAndStoredSetUnchanged() {
        // Arrange
        goldenSetService.replaceGoldenSet(goldenSet("rehber-golden-v1", 3));
        ReplaceGoldenSetRequest different = new ReplaceGoldenSetRequest("rehber-golden-v1", List.of(
                new GoldenQuestionRequest("Kartlar", "Başka soru?", 5, "Tamamen farklı bir cevap cümlesi burada.")));

        // Act + Assert
        assertThatThrownBy(() -> goldenSetService.replaceGoldenSet(different)).isInstanceOfSatisfying(
                ConflictException.class, e -> assertThat(e.getErrorCode()).isEqualTo("GOLDEN_SET_VERSION_CONFLICT"));
        assertThat(goldenSetService.getGoldenSet().questionCount()).isEqualTo(3);
        assertThat(jdbc.queryForList("select text from golden_questions order by position", String.class))
                .containsExactly("Soru 1?", "Soru 2?", "Soru 3?");
    }

    @Test
    void givenSameVersionSameContent_whenReplaceGoldenSet_thenAcceptedAndLoadedAtRenewed() {
        // Arrange
        goldenSetService.replaceGoldenSet(goldenSet("rehber-golden-v1", 2));
        clock.advance(Duration.ofMinutes(5));
        List<GoldenQuestionRequest> padded = List.of(
                new GoldenQuestionRequest(" Kartlar ", "  Soru 1?", 1, answerOf(1) + "  "),
                new GoldenQuestionRequest("Kartlar", "Soru 2? ", 2, " " + answerOf(2)));

        // Act
        var summary = goldenSetService.replaceGoldenSet(new ReplaceGoldenSetRequest(" rehber-golden-v1 ", padded));

        // Assert
        assertThat(summary.version()).isEqualTo("rehber-golden-v1");
        assertThat(summary.questionCount()).isEqualTo(2);
        assertThat(summary.loadedAt()).isEqualTo(T0.plus(Duration.ofMinutes(5)));
        assertThat(jdbc.queryForObject("select count(*) from golden_sets", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("select normalized_text from golden_questions order by position", String.class))
                .containsExactly("soru 1", "soru 2");
    }

    @Test
    void givenNewVersion_whenReplaceGoldenSet_thenOldSetAndQuestionsReplaced() {
        // Arrange
        goldenSetService.replaceGoldenSet(goldenSet("rehber-golden-v1", 3));

        // Act
        goldenSetService.replaceGoldenSet(goldenSet("rehber-golden-v2", 1));

        // Assert
        assertThat(goldenSetService.getGoldenSet().version()).isEqualTo("rehber-golden-v2");
        assertThat(jdbc.queryForObject("select count(*) from golden_questions", Integer.class)).isEqualTo(1);
    }

    @Test
    void givenRunningRun_whenReplaceGoldenSet_thenRunKeepsCopiedQuestions() {
        // Arrange
        goldenSetService.replaceGoldenSet(goldenSet("rehber-golden-v1", 3));
        long runId = createRunWithoutExecutor("rehber-golden-v1", 3);

        // Act
        goldenSetService.replaceGoldenSet(new ReplaceGoldenSetRequest("rehber-golden-v2", List.of(
                new GoldenQuestionRequest("Hesaplar", "Yeni soru?", 9, "Yeni sette tamamen farklı bir cevap cümlesi."))));

        // Assert
        assertThat(jdbc.queryForList("select text from eval_run_questions where eval_run_id = ? order by position",
                String.class, runId)).containsExactly("Soru 1?", "Soru 2?", "Soru 3?");
        assertThat(evalRunService.getEvalRun(runId).goldenSetVersion()).isEqualTo("rehber-golden-v1");
    }

    @Test
    void givenRerankFallbackOnTwoQuestions_whenRunEvalRun_thenCountsTwoFallbacksAndMetrics() {
        // Arrange: soru 1 cevap parçası prompt'ta 1.; soru 2 (fallback) prompt'ta 2.; soru 3 (fallback) vector 3, prompt'ta yok
        long runId = createRunWithoutExecutor("rehber-golden-v1", 3);
        given(contextRetriever.retrieveCandidatesAndContext("Soru 1?")).willReturn(ContextFixtures.rerankContext(1, 2, 3, 4, 5));
        given(contextRetriever.retrieveCandidatesAndContext("Soru 2?"))
                .willReturn(asFallback(ContextFixtures.rerankContext(1, 2, 3, 4, 5)));
        given(contextRetriever.retrieveCandidatesAndContext("Soru 3?"))
                .willReturn(asFallback(ContextFixtures.rerankContext(1, 2, 4, 5, 6)));

        // Act
        evalRunRunner.runEvalRun(runId);

        // Assert: recall 2/3, MRR (1 + 1/2 + 0) / 3 = 0.5
        EvalRunDetailResponse run = evalRunService.getEvalRun(runId);
        assertThat(run.status()).isEqualTo(EvalRunStatus.DONE);
        assertThat(run.rerankFallbackCount()).isEqualTo(2);
        assertThat(run.processedCount()).isEqualTo(3);
        assertThat(run.hitCount()).isEqualTo(2);
        assertThat(run.missedCount()).isEqualTo(1);
        assertThat(run.recallAtK()).isCloseTo(2.0 / 3.0, within(1e-9));
        assertThat(run.mrr()).isCloseTo(0.5, within(1e-9));
        assertThat(run.missedAndMovedQuestions()).extracting("position", "vectorRank", "promptPosition")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3, 3, null));
        verifyNoInteractions(chatModel);
    }

    @Test
    void givenSearchFailsOnSecondQuestion_whenRunEvalRun_thenRunFailsWithRetrievalUnavailable() {
        // Arrange
        long runId = createRunWithoutExecutor("rehber-golden-v1", 3);
        given(contextRetriever.retrieveCandidatesAndContext("Soru 1?")).willReturn(ContextFixtures.rerankContext(1, 2, 3, 4, 5));
        given(contextRetriever.retrieveCandidatesAndContext("Soru 2?"))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        // Act
        evalRunRunner.runEvalRun(runId);

        // Assert
        EvalRunDetailResponse run = evalRunService.getEvalRun(runId);
        assertThat(run.status()).isEqualTo(EvalRunStatus.FAILED);
        assertThat(run.errorCode()).isEqualTo("RETRIEVAL_UNAVAILABLE");
        assertThat(run.processedCount()).isEqualTo(1);
        assertThat(run.recallAtK()).isNull();
        assertThat(run.mrr()).isNull();
        assertThat(run.hitCount()).isNull();
        assertThat(run.missedAndMovedQuestions()).isEmpty();
    }

    @Test
    void givenUnexpectedFailure_whenRunEvalRun_thenRunFailsWithInternalError() {
        // Arrange
        long runId = createRunWithoutExecutor("rehber-golden-v1", 2);
        given(contextRetriever.retrieveCandidatesAndContext(anyString())).willThrow(new IllegalStateException("boom"));

        // Act
        evalRunRunner.runEvalRun(runId);

        // Assert
        EvalRunDetailResponse run = evalRunService.getEvalRun(runId);
        assertThat(run.status()).isEqualTo(EvalRunStatus.FAILED);
        assertThat(run.errorCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void givenFinishedRun_whenGetEvalRun_thenReturnsMetricsVersionsAndDuration() {
        // Arrange: iki soru, her arama saati 1,5 sn ilerletir → süre 3000 ms
        goldenSetService.replaceGoldenSet(goldenSet("rehber-golden-v1", 2));
        stubEveryAnswerFoundInChunkOfSamePosition(2);
        given(contextRetriever.retrieveCandidatesAndContext(anyString())).willAnswer(invocation -> {
            clock.advance(Duration.ofMillis(1500));
            return ContextFixtures.rerankContext(1, 2, 3, 4, 5);
        });

        // Act
        long runId = evalRunService.startEvalRun().evalRunId();
        EvalRunDetailResponse run = awaitFinished(runId);

        // Assert
        assertThat(run.status()).isEqualTo(EvalRunStatus.DONE);
        assertThat(run.goldenSetVersion()).isEqualTo("rehber-golden-v1");
        assertThat(run.sourceVersions()).isEqualTo("2026.10");
        assertThat(run.retrievalStrategy()).isEqualTo(RetrievalStrategy.RERANK);
        assertThat(run.candidates()).isEqualTo(20);
        assertThat(run.topK()).isEqualTo(5);
        assertThat(run.questionCount()).isEqualTo(2);
        assertThat(run.measuredCount()).isEqualTo(2);
        assertThat(run.unmatchedCount()).isZero();
        assertThat(run.startedAt()).isEqualTo(T0);
        assertThat(run.finishedAt()).isEqualTo(T0.plusMillis(3000));
        assertThat(run.durationMs()).isEqualTo(3000L);
        assertThat(run.hitCount()).isEqualTo(2);
        assertThat(run.recallAtK()).isEqualTo(1.0);
        assertThat(run.mrr()).isCloseTo((1.0 + 0.5) / 2, within(1e-9));
    }

    @Test
    void givenRunningRunAtStartup_whenApplicationReady_thenRunFailsInterrupted() {
        // Arrange
        long runId = createRunWithoutExecutor("rehber-golden-v1", 2);
        clock.advance(Duration.ofSeconds(30));

        // Act
        startupListener.failInterruptedRuns();

        // Assert
        EvalRunDetailResponse run = evalRunService.getEvalRun(runId);
        assertThat(run.status()).isEqualTo(EvalRunStatus.FAILED);
        assertThat(run.errorCode()).isEqualTo("EVAL_RUN_INTERRUPTED");
        assertThat(run.finishedAt()).isEqualTo(T0.plusSeconds(30));
        assertThat(evalRunService.getEvalOverview().runningRun()).isNull();
    }

    @RepeatedTest(3)
    void givenTwoConcurrentStarts_whenStartEvalRun_thenOneAcceptedOneConflict() throws Exception {
        // Arrange: ilk ölçüm, ikinci istek gelene kadar RUNNING kalsın diye arama bloklanır
        goldenSetService.replaceGoldenSet(goldenSet("rehber-golden-v1", 2));
        stubEveryAnswerFoundInChunkOfSamePosition(2);
        CountDownLatch release = new CountDownLatch(1);
        given(contextRetriever.retrieveCandidatesAndContext(anyString())).willAnswer(invocation -> {
            release.await(30, TimeUnit.SECONDS);
            return ContextFixtures.rerankContext(1, 2, 3, 4, 5);
        });
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        return evalRunService.startEvalRun();
                    } catch (ConflictException exception) {
                        return exception;
                    }
                }));
            }

            // Act
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> result : results) {
                outcomes.add(result.get(30, TimeUnit.SECONDS));
            }

            // Assert
            assertThat(outcomes.stream().filter(EvalRunResponse.class::isInstance)).hasSize(1);
            assertThat(outcomes.stream().filter(ConflictException.class::isInstance)).hasSize(1)
                    .allSatisfy(e -> assertThat(((ConflictException) e).getErrorCode()).isEqualTo("EVAL_RUN_IN_PROGRESS"));
            assertThat(jdbc.queryForObject("select count(*) from eval_runs where status = 'RUNNING'", Integer.class))
                    .isEqualTo(1);
        } finally {
            release.countDown();
            pool.shutdownNow();
            await().atMost(Duration.ofSeconds(20)).until(
                    () -> jdbc.queryForObject("select count(*) from eval_runs where status = 'RUNNING'", Integer.class) == 0);
        }
    }

    @Test
    void givenRunningAndDoneRuns_whenGetEvalOverview_thenReturnsRunningRunAndFirstTwentyFourMissedOrMoved() {
        // Arrange: bitmiş ölçümde 30 uygun soru (1-30: vector 5, prompt yok ya da 2), 8 + 2 aynı yerde, 2 ölçülemeyen
        goldenSetService.replaceGoldenSet(goldenSet("rehber-golden-v1", 2));
        jdbc.update("""
                insert into eval_runs (status, golden_set_version, retrieval_strategy, candidates, top_k, source_versions,
                    question_count, measured_count, processed_count, hit_count, recall_at_k, mrr, rerank_fallback_count,
                    started_at, finished_at, duration_ms)
                values ('DONE', 'rehber-golden-v1', 'RERANK', 20, 5, '2026.10', 42, 40, 40, 20, 0.5, 0.25, 0,
                    '2026-11-02T09:00:00Z', '2026-11-02T09:01:00Z', 60000)""");
        long doneId = jdbc.queryForObject("select id from eval_runs where status = 'DONE'", Long.class);
        jdbc.update("""
                insert into eval_run_questions (eval_run_id, position, category, text, page_number, answer_text,
                    answer_chunk_ids, is_measured, is_processed, vector_rank, prompt_position)
                select ?, p, 'Kartlar', 'Soru ' || p || '?', 1, 'cevap',
                    case when p <= 40 then array[100 + p]::bigint[] else array[]::bigint[] end,
                    p <= 40, p <= 40,
                    case when p = 3 then null when p <= 30 then 5 when p <= 40 then 3 else null end,
                    case when p <= 30 and p % 2 = 1 then null when p <= 30 then 2 when p <= 40 then 3 else null end
                from generate_series(1, 42) as p""", doneId);
        jdbc.update("""
                insert into eval_runs (status, golden_set_version, retrieval_strategy, candidates, top_k, source_versions,
                    question_count, measured_count, processed_count, rerank_fallback_count, started_at)
                values ('RUNNING', 'rehber-golden-v1', 'RERANK', 20, 5, '2026.10', 10, 10, 5, 0, '2026-11-02T09:30:00Z')""");

        // Act
        EvalOverviewResponse overview = evalRunService.getEvalOverview();

        // Assert
        assertThat(overview.goldenSet().version()).isEqualTo("rehber-golden-v1");
        assertThat(overview.runningRun().processedCount()).isEqualTo(5);
        assertThat(overview.runningRun().status()).isEqualTo(EvalRunStatus.RUNNING);
        assertThat(overview.lastDoneRun().evalRunId()).isEqualTo(doneId);
        assertThat(overview.lastFinishedRun().evalRunId()).isEqualTo(doneId);
        List<Integer> positions = overview.lastDoneRun().missedAndMovedQuestions().stream()
                .map(question -> question.position()).toList();
        assertThat(positions).containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 24).boxed().toList());
        assertThat(overview.lastDoneRun().missedAndMovedQuestions().get(2).vectorRank()).isNull();
        assertThat(overview.lastDoneRun().missedAndMovedQuestions().get(0).promptPosition()).isNull();
        assertThat(overview.lastDoneRun().missedAndMovedQuestions().get(1).promptPosition()).isEqualTo(2);
        assertThat(overview.lastDoneRun().unmatchedCount()).isEqualTo(2);
    }

    @Test
    void givenThreeFinishedRuns_whenListEvalRuns_thenNewestStartedFirstWithPageMetadata() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            long id = createRunWithoutExecutor("rehber-golden-v1", 1);
            clock.advance(Duration.ofMinutes(1));
            lifecycleService.failRun(id, "INTERNAL_ERROR");
        }

        // Act
        var page = evalRunService.listEvalRuns(0, 2);

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(EvalRunResponse::evalRunId).containsExactly(3L, 2L);
    }

    @Test
    void givenUnknownId_whenGetEvalRun_thenNotFound() {
        assertThatThrownBy(() -> evalRunService.getEvalRun(999)).isInstanceOfSatisfying(
                com.rasitbank.assistant.exception.NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("EVAL_RUN_NOT_FOUND"));
    }

    @Test
    void givenRunningRun_whenFailInterruptedRuns_thenReturnsFailedCountAndSecondCallZero() {
        // Arrange
        createRunWithoutExecutor("rehber-golden-v1", 1);

        // Act + Assert
        assertThat(lifecycleService.failInterruptedRuns()).isEqualTo(1);
        assertThat(lifecycleService.failInterruptedRuns()).isZero();
    }

    @Test
    void givenRunningRun_whenCompleteAndFailCalled_thenReturnedResponsesCarryNewState() {
        // Arrange
        long first = createRunWithoutExecutor("rehber-golden-v1", 2);

        // Act
        EvalRunResponse failed = lifecycleService.failRun(first, "INTERNAL_ERROR");
        long second = createRunWithoutExecutor("rehber-golden-v1", 2);
        EvalRunResponse done = lifecycleService.completeRun(second, RetrievalMetrics.evaluate(List.of(1, 3)));

        // Assert
        assertThat(failed.status()).isEqualTo(EvalRunStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(done.status()).isEqualTo(EvalRunStatus.DONE);
        assertThat(done.hitCount()).isEqualTo(2);
        assertThat(done.mrr()).isCloseTo((1.0 + 1.0 / 3.0) / 2, within(1e-9));
    }

    @RepeatedTest(3)
    void givenTwoConcurrentReplaces_whenBothFinish_thenStoredSetIsExactlyOneOfThem() throws Exception {
        // Arrange
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> {
                start.await();
                return goldenSetService.replaceGoldenSet(goldenSet("set-a", 3));
            });
            Future<?> second = pool.submit(() -> {
                start.await();
                return goldenSetService.replaceGoldenSet(goldenSet("set-b", 2));
            });

            // Act
            start.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);

            // Assert
            var stored = goldenSetService.getGoldenSet();
            assertThat(jdbc.queryForObject("select count(*) from golden_sets", Integer.class)).isEqualTo(1);
            assertThat(stored.questionCount()).isEqualTo(stored.version().equals("set-a") ? 3 : 2);
            assertThat(jdbc.queryForObject("select count(*) from golden_questions", Integer.class))
                    .isEqualTo(stored.questionCount());
        } finally {
            pool.shutdownNow();
        }
    }
}
