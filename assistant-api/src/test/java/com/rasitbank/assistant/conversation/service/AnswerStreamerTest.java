package com.rasitbank.assistant.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.conversation.domain.event.ContextChunk;
import com.rasitbank.assistant.conversation.domain.event.DoneEvent;
import com.rasitbank.assistant.conversation.domain.event.SourcesEvent;
import com.rasitbank.assistant.conversation.domain.event.StreamErrorEvent;
import com.rasitbank.assistant.conversation.domain.event.TokenEvent;
import com.rasitbank.assistant.generation.CitationParser;
import com.rasitbank.assistant.generation.NotFoundAnswerDetector;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import com.rasitbank.assistant.support.ChatResponses;
import com.rasitbank.assistant.support.ContextFixtures;
import com.rasitbank.assistant.support.RecordingSseEmitter;
import com.rasitbank.assistant.support.TestProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

/**
 * Cevap akışının olay sırasını, hata ve iptal yollarını, tur kaydının tam bir kez ve olaylardan önce yazıldığını
 * doğrular. Sahte model, sahte tur servisi ve olayları kaydeden sahte emitter kullanılır.
 */
class AnswerStreamerTest {

    private static final Prompt PROMPT = new Prompt("prompt");
    private static final String NOT_FOUND = TestProperties.NOT_FOUND_ANSWER;

    private final ChatModel chatModel = mock(ChatModel.class);
    private final TurnService turnService = mock(TurnService.class);
    private final ActiveTurnRegistry registry = mock(ActiveTurnRegistry.class);

    private AnswerStreamer streamer(AssistantProperties properties) {
        return new AnswerStreamer(chatModel, new CitationParser(), new NotFoundAnswerDetector(properties),
                turnService, registry, properties);
    }

    private static TurnDraft draft(RetrievedContext context) {
        return new TurnDraft(7L, GenerationMode.RAG, "Kartımı kaybettim, ne yapmalıyım?", "Kartımı kaybettim, ne yapmalıyım?",
                QueryRewriteStatus.NO_HISTORY, null, 0, "qwen3.5:9b", Instant.parse("2026-11-01T10:00:00Z"), System.nanoTime(), context);
    }

    private TurnOutcome recordedOutcome() {
        ArgumentCaptor<TurnOutcome> outcome = ArgumentCaptor.captor();
        verify(turnService).recordTurn(any(TurnDraft.class), outcome.capture());
        return outcome.getValue();
    }

    private static void awaitCompleted(RecordingSseEmitter emitter) throws InterruptedException {
        assertThat(emitter.awaitCompletion(5_000)).as("emitter completed within 5 s").isTrue();
    }

    @Test
    void givenModelStreamsThreeChunks_whenStreamModelAnswer_thenTokensThenSourcesThenDoneAfterTurnRecorded()
            throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Kartını "),
                ChatResponses.text("kapat [1]"), ChatResponses.textWithUsage(".", 2154, 61)));
        given(turnService.recordTurn(any(), any())).willAnswer(invocation -> {
            emitter.note("recordTurn");
            return 12L;
        });

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.rerankContext(7, 2, 15, 1, 3)),
                PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.timeline()).containsExactly("token", "token", "token", "recordTurn", "sources", "done",
                "complete");
        List<RecordingSseEmitter.Event> events = emitter.events();
        assertThat(events.get(0).data()).isEqualTo(new TokenEvent("Kartını "));
        assertThat(events.get(1).data()).isEqualTo(new TokenEvent("kapat [1]"));
        assertThat(events.get(2).data()).isEqualTo(new TokenEvent("."));
        SourcesEvent sources = (SourcesEvent) events.get(3).data();
        assertThat(sources.citations()).containsExactly(1);
        assertThat(sources.generationMode()).isEqualTo(GenerationMode.RAG);
        assertThat(sources.retrievalStrategy()).isEqualTo(RetrievalStrategy.RERANK);
        assertThat(sources.candidates()).isEqualTo(20);
        assertThat(sources.topK()).isEqualTo(5);
        assertThat(sources.contextChunks()).extracting(ContextChunk::n).containsExactly(1, 2, 3, 4, 5);
        assertThat(sources.contextChunks()).extracting(ContextChunk::vectorRank).containsExactly(7, 2, 15, 1, 3);
        assertThat(sources.contextChunks()).extracting(ContextChunk::chunkId)
                .containsExactly(107L, 102L, 115L, 101L, 103L);
        ContextChunk first = sources.contextChunks().get(0);
        assertThat(first.heading()).isEqualTo("H107");
        assertThat(first.pageNumber()).isEqualTo(17);
        assertThat(first.chunkNo()).isEqualTo(7);
        assertThat(first.distance()).isEqualTo(0.37);
        assertThat(first.rerankScore()).isEqualTo(9.3);
        assertThat(first.sourceId()).isEqualTo(10001L);
        assertThat(first.version()).isEqualTo("2026.10");
        DoneEvent done = (DoneEvent) events.get(4).data();
        assertThat(done.turnId()).isEqualTo(12L);
        assertThat(done.promptTokens()).isEqualTo(2154);
        assertThat(done.completionTokens()).isEqualTo(61);
        assertThat(done.notFoundAnswer()).isFalse();
        assertThat(done.chatModel()).isEqualTo("qwen3.5:9b");
        assertThat(done.retrievalLatencyMs()).isEqualTo(84L);
        assertThat(done.rerankLatencyMs()).isEqualTo(712L);
        assertThat(done.generationLatencyMs()).isNotNull();
        assertThat(done.timeToFirstTokenMs()).isNotNull();
        assertThat(done.hostLabel()).isNull();
        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.answer()).isEqualTo("Kartını kapat [1].");
        assertThat(outcome.citations()).containsExactly(1);
        assertThat(outcome.promptTokens()).isEqualTo(2154);
        assertThat(outcome.completionTokens()).isEqualTo(61);
        assertThat(outcome.errorCode()).isNull();
        verify(registry).finish(7L);
    }

    @Test
    void givenHostLabelConfigured_whenStreamModelAnswer_thenDoneCarriesHostLabel() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Merhaba")));
        given(turnService.recordTurn(any(), any())).willReturn(1L);

        streamer(TestProperties.withHostLabel("Apple M5 · 16 GB")).streamModelAnswer(
                draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        DoneEvent done = (DoneEvent) emitter.events().get(emitter.events().size() - 1).data();
        assertThat(done.hostLabel()).isEqualTo("Apple M5 · 16 GB");
        assertThat(done.rerankLatencyMs()).isNull();
    }

    @Test
    void givenUsageOnlyInZeroValuedChunks_whenStreamModelAnswer_thenTokenCountsNull() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.textWithUsage("a", 0, 0),
                ChatResponses.text("b")));
        given(turnService.recordTurn(any(), any())).willReturn(1L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.promptTokens()).isNull();
        assertThat(outcome.completionTokens()).isNull();
    }

    @Test
    void givenEmptyTextChunks_whenStreamModelAnswer_thenNoEmptyTokenEvents() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text(""),
                ChatResponses.text("Merhaba"), ChatResponses.textWithUsage("", 10, 2)));
        given(turnService.recordTurn(any(), any())).willReturn(1L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.eventNames()).containsExactly("token", "sources", "done");
        assertThat(emitter.events().get(0).data()).isEqualTo(new TokenEvent("Merhaba"));
    }

    @Test
    void givenModelFailsAfterFirstChunk_whenStreamModelAnswer_thenErrorEventAndTurnRecordedWithPartialAnswer()
            throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.concat(Flux.just(ChatResponses.text("Kartını ")), Flux.error(new IllegalStateException("down"))));
        given(turnService.recordTurn(any(), any())).willReturn(3L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.eventNames()).containsExactly("token", "error");
        assertThat(emitter.events().get(0).data()).isEqualTo(new TokenEvent("Kartını "));
        assertThat(emitter.events().get(1).data()).isEqualTo(new StreamErrorEvent("CHAT_MODEL_UNAVAILABLE"));
        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.answer()).isEqualTo("Kartını ");
        assertThat(outcome.errorCode()).isEqualTo("CHAT_MODEL_UNAVAILABLE");
        assertThat(outcome.notFoundAnswer()).isFalse();
        verify(registry, times(1)).finish(7L);
    }

    @Test
    void givenModelRefusesConnection_whenStreamModelAnswer_thenOnlyErrorEvent() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.error(new IllegalStateException("refused")));
        given(turnService.recordTurn(any(), any())).willReturn(3L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.events()).hasSize(1);
        assertThat(emitter.events().get(0).name()).isEqualTo("error");
        assertThat(emitter.events().get(0).data()).isEqualTo(new StreamErrorEvent("CHAT_MODEL_UNAVAILABLE"));
        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.answer()).isNull();
        assertThat(outcome.promptTokens()).isNull();
        assertThat(outcome.errorCode()).isEqualTo("CHAT_MODEL_UNAVAILABLE");
    }

    @Test
    void givenModelStreamThrowsSynchronously_whenStreamModelAnswer_thenOnlyErrorEvent() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willThrow(new IllegalStateException("refused"));
        given(turnService.recordTurn(any(), any())).willReturn(3L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.eventNames()).containsExactly("error");
        assertThat(recordedOutcome().errorCode()).isEqualTo("CHAT_MODEL_UNAVAILABLE");
    }

    @Test
    void givenClientDisconnectsOnSecondToken_whenStreamModelAnswer_thenModelSubscriptionCancelledAndTurnCancelled()
            throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter(2);
        AtomicBoolean modelCancelled = new AtomicBoolean();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Kartını "),
                ChatResponses.text("kapat [1]"), ChatResponses.text(".")).concatWith(Flux.never())
                .doOnCancel(() -> modelCancelled.set(true)));
        given(turnService.recordTurn(any(), any())).willReturn(4L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(modelCancelled).isTrue();
        assertThat(emitter.eventNames()).containsExactly("token");
        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.errorCode()).isEqualTo("STREAM_CANCELLED");
        assertThat(outcome.answer()).isEqualTo("Kartını ");
        verify(registry, times(1)).finish(7L);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"timeout", "completion", "error"})
    void givenSingleEmitterCallbackFiresMidStream_whenStreamModelAnswer_thenModelCancelledAndTurnCancelledOnce(
            String callback) throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        AtomicBoolean modelCancelled = new AtomicBoolean();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Kartını "))
                .concatWith(Flux.never()).doOnCancel(() -> modelCancelled.set(true)));
        given(turnService.recordTurn(any(), any())).willReturn(4L);
        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        await().atMost(Duration.ofSeconds(5)).until(() -> emitter.eventNames().contains("token"));

        switch (callback) {
            case "timeout" -> emitter.simulateTimeout();
            case "completion" -> emitter.simulateCompletionCallback();
            default -> emitter.simulateError(new RuntimeException("late"));
        }

        assertThat(modelCancelled).isTrue();
        assertThat(recordedOutcome().errorCode()).isEqualTo("STREAM_CANCELLED");
        assertThat(recordedOutcome().answer()).isEqualTo("Kartını ");
        verify(registry, times(1)).finish(7L);
        assertThat(emitter.eventNames()).containsExactly("token");
    }

    @Test
    void givenSlowTokens_whenStreamModelAnswer_thenMeasuredLatenciesReflectRealTime() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("a"),
                ChatResponses.text("b"), ChatResponses.text("c")).delayElements(Duration.ofMillis(100)));
        given(turnService.recordTurn(any(), any())).willReturn(1L);
        TurnDraft slowDraft = new TurnDraft(7L, GenerationMode.RAG, "q", "q", QueryRewriteStatus.NO_HISTORY, null, 0, "qwen3.5:9b", Instant.parse("2026-11-01T10:00:00Z"),
                System.nanoTime() - Duration.ofSeconds(2).toNanos(), ContextFixtures.topKContext());

        streamer(TestProperties.defaults()).streamModelAnswer(slowDraft, PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        DoneEvent done = (DoneEvent) emitter.events().get(emitter.events().size() - 1).data();
        assertThat(done.generationLatencyMs()).isBetween(300L, 1500L);
        assertThat(done.timeToFirstTokenMs()).isBetween(80L, done.generationLatencyMs());
        assertThat(done.latencyMs()).isBetween(2300L, 4000L);
    }

    @Test
    void givenEmitterRuntimeFailureOnFirstToken_whenStreamModelAnswer_thenInternalErrorRecordedAndSent()
            throws Exception {
        RecordingSseEmitter emitter = RecordingSseEmitter.failingWithRuntimeExceptionOnSend(1);
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.just(ChatResponses.text("a")).concatWith(Flux.never()));
        given(turnService.recordTurn(any(), any())).willReturn(1L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.eventNames()).containsExactly("error");
        assertThat(emitter.events().get(0).data()).isEqualTo(new StreamErrorEvent("INTERNAL_ERROR"));
        assertThat(recordedOutcome().errorCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void givenCompletionCallbackAfterNormalFinish_whenStreamModelAnswer_thenTurnRecordedOnlyOnce() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Merhaba [1]")));
        given(turnService.recordTurn(any(), any())).willReturn(1L);
        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        emitter.simulateCompletionCallback();

        verify(turnService, times(1)).recordTurn(any(), any());
        verify(registry, times(1)).finish(7L);
    }

    @Test
    void givenModelSilentLongerThanFirstTokenTimeout_whenStreamModelAnswer_thenChatModelUnavailable()
            throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        AtomicBoolean modelCancelled = new AtomicBoolean();
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.<org.springframework.ai.chat.model.ChatResponse>never().doOnCancel(() -> modelCancelled.set(true)));
        given(turnService.recordTurn(any(), any())).willReturn(5L);
        AssistantProperties properties = TestProperties.withModelTimeouts(Duration.ofMillis(100),
                Duration.ofSeconds(30));

        streamer(properties).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.eventNames()).containsExactly("error");
        assertThat(emitter.events().get(0).data()).isEqualTo(new StreamErrorEvent("CHAT_MODEL_UNAVAILABLE"));
        assertThat(recordedOutcome().errorCode()).isEqualTo("CHAT_MODEL_UNAVAILABLE");
        assertThat(modelCancelled).isTrue();
    }

    @Test
    void givenModelSilentAfterFirstTokenLongerThanIdleTimeout_whenStreamModelAnswer_thenErrorWithPartialAnswer()
            throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.just(ChatResponses.text("Kartını ")).concatWith(Flux.never()));
        given(turnService.recordTurn(any(), any())).willReturn(5L);
        AssistantProperties properties = TestProperties.withModelTimeouts(Duration.ofSeconds(30),
                Duration.ofMillis(100));

        streamer(properties).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.eventNames()).containsExactly("token", "error");
        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.answer()).isEqualTo("Kartını ");
        assertThat(outcome.errorCode()).isEqualTo("CHAT_MODEL_UNAVAILABLE");
    }

    @Test
    void givenModelWritesNotFoundSentence_whenStreamModelAnswer_thenTurnAndDoneMarkedNotFound() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Bu konuda rehberimizde "),
                ChatResponses.text("bilgi bulamadım. [1]")));
        given(turnService.recordTurn(any(), any())).willReturn(6L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        DoneEvent done = (DoneEvent) emitter.events().get(emitter.events().size() - 1).data();
        assertThat(done.notFoundAnswer()).isTrue();
        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.notFoundAnswer()).isTrue();
        assertThat(outcome.citations()).containsExactly(1);
    }

    @Test
    void givenStreaming_whenModelCalled_thenNoTransactionActive() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        AtomicReference<Boolean> transactionActive = new AtomicReference<>();
        given(chatModel.stream(any(Prompt.class))).willAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return Flux.just(ChatResponses.text("Merhaba"));
        });
        given(turnService.recordTurn(any(), any())).willReturn(1L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(transactionActive.get()).isFalse();
    }

    @Test
    void givenTurnRecordFailsAfterStreaming_whenStreamModelAnswer_thenInternalErrorEventAndNoDoneEvent()
            throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Merhaba")));
        given(turnService.recordTurn(any(), any())).willThrow(new IllegalStateException("db down"))
                .willReturn(9L);

        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.topKContext()), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        assertThat(emitter.eventNames()).containsExactly("token", "error");
        assertThat(emitter.events().get(1).data()).isEqualTo(new StreamErrorEvent("INTERNAL_ERROR"));
        ArgumentCaptor<TurnOutcome> outcomes = ArgumentCaptor.captor();
        verify(turnService, times(2)).recordTurn(any(TurnDraft.class), outcomes.capture());
        assertThat(outcomes.getAllValues().get(1).errorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(outcomes.getAllValues().get(1).answer()).isEqualTo("Merhaba");
        verify(registry, times(1)).finish(7L);
    }

    @Test
    void givenFixedAnswer_whenStreamFixedAnswer_thenTokenEmptySourcesAndNotFoundDone() throws Exception {
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(turnService.recordTurn(any(), any())).willReturn(8L);

        streamer(TestProperties.defaults()).streamFixedAnswer(
                draft(ContextFixtures.emptyContext(RetrievalStrategy.RERANK)), NOT_FOUND, emitter);

        assertThat(emitter.timeline()).containsExactly("token", "sources", "done", "complete");
        assertThat(emitter.events().get(0).data()).isEqualTo(new TokenEvent(NOT_FOUND));
        SourcesEvent sources = (SourcesEvent) emitter.events().get(1).data();
        assertThat(sources.contextChunks()).isEmpty();
        assertThat(sources.citations()).isEmpty();
        DoneEvent done = (DoneEvent) emitter.events().get(2).data();
        assertThat(done.notFoundAnswer()).isTrue();
        assertThat(done.turnId()).isEqualTo(8L);
        assertThat(done.promptTokens()).isNull();
        assertThat(done.completionTokens()).isNull();
        assertThat(done.generationLatencyMs()).isNull();
        assertThat(done.timeToFirstTokenMs()).isNull();
        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.answer()).isEqualTo(NOT_FOUND);
        assertThat(outcome.notFoundAnswer()).isTrue();
        verify(registry).finish(7L);
        verify(chatModel, never()).stream(any(Prompt.class));
    }

    @Test
    void givenFixedAnswerAndDisconnectedClient_whenStreamFixedAnswer_thenTurnCancelled() {
        RecordingSseEmitter emitter = new RecordingSseEmitter(1);

        streamer(TestProperties.defaults()).streamFixedAnswer(
                draft(ContextFixtures.emptyContext(RetrievalStrategy.RERANK)), NOT_FOUND, emitter);

        assertThat(emitter.events()).isEmpty();
        assertThat(recordedOutcome().errorCode()).isEqualTo("STREAM_CANCELLED");
        verify(registry, times(1)).finish(eq(7L));
    }

    private static TurnDraft llmOnlyDraft() {
        return new TurnDraft(7L, GenerationMode.LLM_ONLY, "Kartımı kaybettim, ne yapmalıyım?", null,
                QueryRewriteStatus.DISABLED, null, 0, "qwen3.5:9b", Instant.parse("2026-11-01T10:00:00Z"),
                System.nanoTime(), null);
    }

    @Test
    void givenLlmOnlyAnswerWithCitationText_whenStreamModelAnswer_thenSourcesWithoutChunksAndNoCitations()
            throws Exception {
        // Arrange
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Kartını kapat "),
                ChatResponses.textWithUsage("[1].", 63, 9)));
        given(turnService.recordTurn(any(), any())).willReturn(31L);

        // Act
        streamer(TestProperties.withMode("llm", "rerank")).streamModelAnswer(llmOnlyDraft(), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        // Assert
        assertThat(emitter.eventNames()).containsExactly("token", "token", "sources", "done");
        SourcesEvent sources = (SourcesEvent) emitter.events().get(2).data();
        assertThat(sources).isEqualTo(new SourcesEvent(GenerationMode.LLM_ONLY, RetrievalStrategy.NONE, null, null,
                null, QueryRewriteStatus.DISABLED, List.of(), List.of()));
        DoneEvent done = (DoneEvent) emitter.events().get(3).data();
        assertThat(done.retrievalLatencyMs()).isNull();
        assertThat(done.rerankLatencyMs()).isNull();
        assertThat(done.notFoundAnswer()).isFalse();
        assertThat(done.promptTokens()).isEqualTo(63);
        TurnOutcome outcome = recordedOutcome();
        assertThat(outcome.answer()).isEqualTo("Kartını kapat [1].");
        assertThat(outcome.citations()).isEmpty();
    }

    @Test
    void givenLlmOnlyAnswerWithNotFoundSentence_whenCompleted_thenNotFoundFalse() throws Exception {
        // Arrange
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.textWithUsage(NOT_FOUND, 40, 8)));
        given(turnService.recordTurn(any(), any())).willReturn(32L);

        // Act
        streamer(TestProperties.withMode("llm", "rerank")).streamModelAnswer(llmOnlyDraft(), PROMPT, emitter, Set.of());
        awaitCompleted(emitter);

        // Assert
        DoneEvent done = (DoneEvent) emitter.events().get(emitter.events().size() - 1).data();
        assertThat(done.notFoundAnswer()).isFalse();
        assertThat(recordedOutcome().notFoundAnswer()).isFalse();
    }

    @Test
    void givenAnswerChunkIds_whenStreamModelAnswer_thenOnlyThoseChunksMarkedContainsAnswer() throws Exception {
        // Arrange: prompt sırası vector 7,2,15,1,3 → chunkId 107,102,115,101,103; cevap parçaları 102 ve 103
        RecordingSseEmitter emitter = new RecordingSseEmitter();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Merhaba [1]")));
        given(turnService.recordTurn(any(), any())).willReturn(1L);

        // Act
        streamer(TestProperties.defaults()).streamModelAnswer(draft(ContextFixtures.rerankContext(7, 2, 15, 1, 3)),
                PROMPT, emitter, Set.of(102L, 103L, 999L));
        awaitCompleted(emitter);

        // Assert
        SourcesEvent sources = (SourcesEvent) emitter.events().stream()
                .filter(event -> event.data() instanceof SourcesEvent).findFirst().orElseThrow().data();
        assertThat(sources.contextChunks()).extracting(ContextChunk::chunkId, ContextChunk::containsAnswer)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(107L, false),
                        org.assertj.core.groups.Tuple.tuple(102L, true),
                        org.assertj.core.groups.Tuple.tuple(115L, false),
                        org.assertj.core.groups.Tuple.tuple(101L, false),
                        org.assertj.core.groups.Tuple.tuple(103L, true));
    }
}
