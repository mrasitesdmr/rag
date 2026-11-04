package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.AnswerSource;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.conversation.domain.event.ContextChunk;
import com.rasitbank.assistant.conversation.domain.event.DoneEvent;
import com.rasitbank.assistant.conversation.domain.event.SourcesEvent;
import com.rasitbank.assistant.conversation.domain.event.StreamErrorEvent;
import com.rasitbank.assistant.conversation.domain.event.TokenEvent;
import com.rasitbank.assistant.generation.AnswerFinalizer;
import com.rasitbank.assistant.generation.FinalAnswer;
import com.rasitbank.assistant.generation.NotFoundAnswerDetector;
import com.rasitbank.assistant.generation.NotFoundAnswerStreamFilter;
import com.rasitbank.assistant.retrieval.service.RetrievedChunk;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Dil modelinin cevabını ya da hazır bir cevabı SSE olayları olarak akıtır (token, sources, done; hata olursa error),
 * istemci koparsa model çağrısını iptal eder ve her turu akış bitince tam bir kez kaydeder. RAG'de "bulamadım"
 * cümlesinin başını cümle tamamlanana kadar tutar, sonrasını göndermez ve cevabı tura denetlenmiş haliyle yazar.
 * Sadece LLM turunda alıntı ve "bulamadım" tespiti yapılmaz, sources olayı parçasız gider.
 */
@Component
public class AnswerStreamer {

    private static final Logger log = LoggerFactory.getLogger(AnswerStreamer.class);

    static final String TOKEN_EVENT = "token";
    static final String SOURCES_EVENT = "sources";
    static final String DONE_EVENT = "done";
    static final String ERROR_EVENT = "error";

    static final String CHAT_MODEL_UNAVAILABLE = "CHAT_MODEL_UNAVAILABLE";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    static final String STREAM_CANCELLED = "STREAM_CANCELLED";

    private final ChatModel chatModel;
    private final AnswerFinalizer answerFinalizer;
    private final NotFoundAnswerDetector notFoundAnswerDetector;
    private final TurnService turnService;
    private final ActiveTurnRegistry activeTurnRegistry;
    private final AssistantProperties properties;

    public AnswerStreamer(ChatModel chatModel, AnswerFinalizer answerFinalizer,
            NotFoundAnswerDetector notFoundAnswerDetector, TurnService turnService,
            ActiveTurnRegistry activeTurnRegistry, AssistantProperties properties) {
        this.chatModel = chatModel;
        this.answerFinalizer = answerFinalizer;
        this.notFoundAnswerDetector = notFoundAnswerDetector;
        this.turnService = turnService;
        this.activeTurnRegistry = activeTurnRegistry;
        this.properties = properties;
    }

    public void streamModelAnswer(TurnDraft draft, Prompt prompt, SseEmitter emitter, Set<Long> answerChunkIds) {
        StreamingTurn turn = new StreamingTurn(draft, emitter, answerChunkIds, AnswerSource.MODEL);
        turn.registerEmitterCallbacks();
        turn.start(prompt);
    }

    public void streamFixedAnswer(TurnDraft draft, String answer, AnswerSource answerSource, SseEmitter emitter) {
        StreamingTurn turn = new StreamingTurn(draft, emitter, Set.of(), answerSource);
        turn.registerEmitterCallbacks();
        turn.completeWithFixedAnswer(answer);
    }

    private static boolean isRag(TurnDraft draft) {
        return draft.generationMode() == GenerationMode.RAG;
    }

    private static long elapsedMs(long startNanos, long endNanos) {
        return Duration.ofNanos(endNanos - startNanos).toMillis();
    }

    private static String textOf(ChatResponse response) {
        Generation generation = response.getResult();
        if (generation == null) {
            return "";
        }
        return Objects.requireNonNullElse(generation.getOutput().getText(), "");
    }

    private static void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
    }

    private final class StreamingTurn {

        private final TurnDraft draft;
        private final @Nullable RetrievedContext context;
        private final SseEmitter emitter;
        private final AtomicBoolean finished = new AtomicBoolean();
        private final StringBuilder rawAnswer = new StringBuilder();
        private final Set<Long> answerChunkIds;
        private final AnswerSource answerSource;
        private final @Nullable NotFoundAnswerStreamFilter notFoundFilter;

        private volatile @Nullable Disposable subscription;
        private @Nullable Long generationStartNanos;
        private @Nullable Long firstTokenNanos;
        private @Nullable Integer promptTokens;
        private @Nullable Integer completionTokens;

        private StreamingTurn(TurnDraft draft, SseEmitter emitter, Set<Long> answerChunkIds,
                AnswerSource answerSource) {
            this.draft = draft;
            this.context = isRag(draft) ? draft.context() : null;
            this.emitter = emitter;
            this.answerChunkIds = Set.copyOf(answerChunkIds);
            this.answerSource = answerSource;
            this.notFoundFilter = isRag(draft) && answerSource == AnswerSource.MODEL
                    ? notFoundAnswerDetector.newStreamFilter()
                    : null;
        }

        private void registerEmitterCallbacks() {
            emitter.onCompletion(this::cancel);
            emitter.onTimeout(this::cancel);
            emitter.onError(error -> cancel());
        }

        private void start(Prompt prompt) {
            if (isRag(draft)) {
                Objects.requireNonNull(context, "A RAG model answer needs a retrieved context");
            }
            synchronized (this) {
                generationStartNanos = System.nanoTime();
            }
            AssistantProperties.Generation generation = properties.generation();
            Flux<ChatResponse> responses;
            try {
                responses = chatModel.stream(prompt);
            } catch (RuntimeException exception) {
                fail(CHAT_MODEL_UNAVAILABLE, exception);
                return;
            }
            subscription = responses
                    .timeout(Mono.delay(generation.firstTokenTimeout()),
                            response -> Mono.delay(generation.tokenIdleTimeout()))
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(this::onResponse, error -> fail(CHAT_MODEL_UNAVAILABLE, error), this::onModelCompleted);
            if (finished.get()) {
                disposeSubscription();
            }
        }

        private void onResponse(ChatResponse response) {
            if (finished.get()) {
                return;
            }
            captureUsage(response);
            String text = textOf(response);
            if (text.isEmpty()) {
                return;
            }
            String visibleText;
            synchronized (this) {
                if (firstTokenNanos == null) {
                    firstTokenNanos = System.nanoTime();
                }
                rawAnswer.append(text);
                visibleText = notFoundFilter == null ? text : notFoundFilter.accept(text);
            }
            sendToken(visibleText);
        }

        private boolean sendToken(String text) {
            if (text.isEmpty()) {
                return true;
            }
            try {
                send(emitter, TOKEN_EVENT, new TokenEvent(text));
                return true;
            } catch (IOException exception) {
                cancel();
                return false;
            } catch (RuntimeException exception) {
                fail(INTERNAL_ERROR, exception);
                return false;
            }
        }

        private synchronized void captureUsage(ChatResponse response) {
            Usage usage = response.getMetadata().getUsage();
            if (usage == null) {
                return;
            }
            Integer prompt = usage.getPromptTokens();
            Integer completion = usage.getCompletionTokens();
            if ((prompt != null && prompt > 0) || (completion != null && completion > 0)) {
                promptTokens = prompt;
                completionTokens = completion;
            }
        }

        private void onModelCompleted() {
            if (finished.get()) {
                return;
            }
            String heldText;
            synchronized (this) {
                heldText = notFoundFilter == null ? "" : notFoundFilter.finish();
            }
            if (!sendToken(heldText) || !finished.compareAndSet(false, true)) {
                return;
            }
            long endNanos = System.nanoTime();
            TurnOutcome outcome;
            synchronized (this) {
                FinalAnswer finalAnswer = finalAnswer(rawAnswer.toString());
                outcome = new TurnOutcome(
                        finalAnswer.answer(),
                        finalAnswer.notFoundAnswer(),
                        finalAnswer.citations(),
                        promptTokens,
                        completionTokens,
                        generationMs(endNanos),
                        generationMs(firstTokenNanos),
                        elapsedMs(draft.startNanos(), endNanos),
                        null,
                        AnswerSource.MODEL);
            }
            finishSuccessfully(outcome);
        }

        private FinalAnswer finalAnswer(String modelText) {
            if (context == null) {
                return new FinalAnswer(modelText, false, List.of(), 0);
            }
            FinalAnswer finalAnswer = answerFinalizer.finalizeRagAnswer(modelText, context.contextChunks().size());
            int removedLength = modelText.strip().length() - finalAnswer.answer().length();
            if (finalAnswer.notFoundAnswer() && removedLength > 0) {
                log.info("Not found answer normalized conversationId={} removedLength={}", draft.conversationId(),
                        removedLength);
            }
            if (finalAnswer.openerCitationsRemoved() > 0) {
                log.info("Opener citations removed conversationId={} removedCount={}", draft.conversationId(),
                        finalAnswer.openerCitationsRemoved());
            }
            return finalAnswer;
        }

        private void completeWithFixedAnswer(String answer) {
            if (!sendToken(answer)) {
                return;
            }
            synchronized (this) {
                rawAnswer.append(answer);
            }
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            long latencyMs = elapsedMs(draft.startNanos(), System.nanoTime());
            finishSuccessfully(new TurnOutcome(answer, isFixedNotFoundAnswer(), List.of(), null, null, null, null,
                    latencyMs, null, answerSource));
        }

        private boolean isFixedNotFoundAnswer() {
            return answerSource == AnswerSource.EMPTY_KNOWLEDGE_BASE
                    || answerSource == AnswerSource.BELOW_RERANK_THRESHOLD;
        }

        private void finishSuccessfully(TurnOutcome outcome) {
            long turnId;
            try {
                turnId = turnService.recordTurn(draft, outcome);
            } catch (RuntimeException exception) {
                log.error("Turn record failed conversationId={} reason={}", draft.conversationId(),
                        exception.getClass().getSimpleName());
                recordQuietly(TurnOutcome.failed(outcome.answer(), INTERNAL_ERROR, outcome.generationLatencyMs(),
                        outcome.timeToFirstTokenMs(), outcome.latencyMs()));
                activeTurnRegistry.finish(draft.conversationId());
                sendErrorAndComplete(INTERNAL_ERROR);
                return;
            }
            activeTurnRegistry.finish(draft.conversationId());
            try {
                send(emitter, SOURCES_EVENT, sourcesEvent(outcome.citations()));
                send(emitter, DONE_EVENT, doneEvent(turnId, outcome));
                emitter.complete();
            } catch (IOException | RuntimeException exception) {
                log.info("Stream closed before done event conversationId={} turnId={}", draft.conversationId(), turnId);
            }
        }

        private void fail(String errorCode, Throwable error) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            disposeSubscription();
            log.warn("Answer stream failed conversationId={} errorCode={} reason={}", draft.conversationId(),
                    errorCode, error.getClass().getSimpleName());
            recordQuietly(interruptedOutcome(errorCode));
            activeTurnRegistry.finish(draft.conversationId());
            sendErrorAndComplete(errorCode);
        }

        private void cancel() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            disposeSubscription();
            recordQuietly(interruptedOutcome(STREAM_CANCELLED));
            activeTurnRegistry.finish(draft.conversationId());
            try {
                emitter.complete();
            } catch (RuntimeException exception) {
                log.debug("Emitter already closed conversationId={}", draft.conversationId());
            }
        }

        private synchronized TurnOutcome interruptedOutcome(String errorCode) {
            long endNanos = System.nanoTime();
            String answer = rawAnswer.isEmpty() ? null : rawAnswer.toString();
            if (answerSource == AnswerSource.MODEL) {
                return TurnOutcome.failed(answer, errorCode, generationMs(endNanos), generationMs(firstTokenNanos),
                        elapsedMs(draft.startNanos(), endNanos));
            }
            return new TurnOutcome(answer, isFixedNotFoundAnswer(), List.of(), null, null, null, null,
                    elapsedMs(draft.startNanos(), endNanos), errorCode, answerSource);
        }

        private synchronized @Nullable Long generationMs(@Nullable Long endNanos) {
            if (generationStartNanos == null || endNanos == null) {
                return null;
            }
            return elapsedMs(generationStartNanos, endNanos);
        }

        private void recordQuietly(TurnOutcome outcome) {
            try {
                turnService.recordTurn(draft, outcome);
            } catch (RuntimeException exception) {
                log.error("Turn record failed conversationId={} errorCode={} reason={}", draft.conversationId(),
                        outcome.errorCode(), exception.getClass().getSimpleName());
            }
        }

        private void sendErrorAndComplete(String errorCode) {
            try {
                send(emitter, ERROR_EVENT, new StreamErrorEvent(errorCode));
                emitter.complete();
            } catch (IOException | RuntimeException exception) {
                log.debug("Stream closed before error event conversationId={}", draft.conversationId());
            }
        }

        private void disposeSubscription() {
            Disposable current = subscription;
            if (current != null) {
                current.dispose();
            }
        }

        private SourcesEvent sourcesEvent(List<Integer> citations) {
            if (context == null) {
                return new SourcesEvent(draft.generationMode(), RetrievalStrategy.NONE, null, null, null,
                        QueryRewriteStatus.DISABLED, List.of(), List.of());
            }
            List<RetrievedChunk> contextChunks = context.contextChunks();
            List<ContextChunk> chunks = new ArrayList<>(contextChunks.size());
            for (int i = 0; i < contextChunks.size(); i++) {
                RetrievedChunk chunk = contextChunks.get(i);
                chunks.add(new ContextChunk(
                        i + 1,
                        chunk.hit().chunkId(),
                        chunk.hit().chunkNo(),
                        chunk.hit().pageNumber(),
                        chunk.hit().heading(),
                        chunk.hit().sourceId(),
                        chunk.hit().sourceName(),
                        chunk.hit().version(),
                        chunk.vectorRank(),
                        chunk.hit().distance(),
                        chunk.rerankScore(),
                        answerChunkIds.contains(chunk.hit().chunkId())));
            }
            return new SourcesEvent(GenerationMode.RAG, context.strategy(), context.candidates(), context.topK(),
                    draft.searchQuery(), draft.queryRewriteStatus(), chunks, citations);
        }

        private DoneEvent doneEvent(long turnId, TurnOutcome outcome) {
            return new DoneEvent(
                    turnId,
                    outcome.notFoundAnswer(),
                    Objects.requireNonNullElse(outcome.answer(), ""),
                    outcome.answerSource(),
                    draft.chatModel(),
                    outcome.promptTokens(),
                    outcome.completionTokens(),
                    draft.queryRewriteLatencyMs(),
                    context == null ? null : context.retrievalLatencyMs(),
                    context == null ? null : context.rerankLatencyMs(),
                    outcome.generationLatencyMs(),
                    outcome.timeToFirstTokenMs(),
                    outcome.latencyMs(),
                    properties.hostLabel());
        }
    }
}
