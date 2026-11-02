package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.dto.SendMessageRequest;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.evaluation.service.GoldenAnswerLocator;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.generation.HistoryTurn;
import com.rasitbank.assistant.generation.PromptBuilder;
import com.rasitbank.assistant.retrieval.service.ContextRetriever;
import com.rasitbank.assistant.retrieval.service.QueryRewriteResult;
import com.rasitbank.assistant.retrieval.service.QueryRewriter;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Müşterinin sorusunu yapılandırılan üretim moduna göre cevaplar: akış açılmadan önce konuşmayı ve süren turu
 * denetler, geçmişi okur; RAG'de takip sorusunu arama sorgusuna çevirip retrieval'ı yapar, Sadece LLM'de bu adımları
 * atlar; soru golden set'teyse cevabın geçtiği parçaları bulur; sonra cevabı SSE akışı olarak başlatır. Model
 * çağrıları sırasında transaction açık tutmaz.
 */
@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    private final ConversationService conversationService;
    private final ActiveTurnRegistry activeTurnRegistry;
    private final QueryRewriter queryRewriter;
    private final ContextRetriever contextRetriever;
    private final PromptBuilder promptBuilder;
    private final AnswerStreamer answerStreamer;
    private final TurnService turnService;
    private final GoldenAnswerLocator goldenAnswerLocator;
    private final AssistantProperties properties;
    private final String chatModelName;
    private final Clock clock;

    public AnswerService(ConversationService conversationService, ActiveTurnRegistry activeTurnRegistry,
            QueryRewriter queryRewriter, ContextRetriever contextRetriever, PromptBuilder promptBuilder,
            AnswerStreamer answerStreamer,
            TurnService turnService, GoldenAnswerLocator goldenAnswerLocator, AssistantProperties properties,
            @Value("${spring.ai.ollama.chat.model}") String chatModelName, Clock clock) {
        this.conversationService = conversationService;
        this.activeTurnRegistry = activeTurnRegistry;
        this.queryRewriter = queryRewriter;
        this.contextRetriever = contextRetriever;
        this.promptBuilder = promptBuilder;
        this.answerStreamer = answerStreamer;
        this.turnService = turnService;
        this.goldenAnswerLocator = goldenAnswerLocator;
        this.properties = properties;
        this.chatModelName = chatModelName;
        this.clock = clock;
    }

    public SseEmitter sendMessage(long conversationId, SendMessageRequest request) {
        long startNanos = System.nanoTime();
        Instant receivedAt = Instant.now(clock);
        conversationService.getConversation(conversationId);
        String question = Objects.requireNonNull(request.text()).strip();
        if (!activeTurnRegistry.tryStart(conversationId, question, receivedAt)) {
            throw new ConflictException("TURN_IN_PROGRESS",
                    "An answer is still being produced in conversation: " + conversationId);
        }
        boolean streamStarted = false;
        try {
            List<HistoryTurn> history = conversationService.getHistory(conversationId);
            SseEmitter emitter = properties.generation().configuredMode() == GenerationMode.LLM_ONLY
                    ? answerWithoutRetrieval(conversationId, question, history, receivedAt, startNanos)
                    : answerWithRetrieval(conversationId, question, history, receivedAt, startNanos);
            streamStarted = true;
            return emitter;
        } finally {
            if (!streamStarted) {
                activeTurnRegistry.finish(conversationId);
            }
        }
    }

    private SseEmitter answerWithoutRetrieval(long conversationId, String question, List<HistoryTurn> history,
            Instant receivedAt, long startNanos) {
        log.info("Generation without retrieval conversationId={} mode={} historyTurns={} questionLength={}",
                conversationId, GenerationMode.LLM_ONLY, history.size(), question.length());
        TurnDraft draft = new TurnDraft(conversationId, GenerationMode.LLM_ONLY, question, null,
                QueryRewriteStatus.DISABLED, null, history.size(), chatModelName, receivedAt, startNanos, null);
        SseEmitter emitter = new SseEmitter(properties.generation().streamTimeout().toMillis());
        answerStreamer.streamModelAnswer(draft, promptBuilder.buildLlmOnlyPrompt(question, history), emitter,
                Set.of());
        return emitter;
    }

    private SseEmitter answerWithRetrieval(long conversationId, String question, List<HistoryTurn> history,
            Instant receivedAt, long startNanos) {
        QueryRewriteResult rewrite = queryRewriter.rewriteQuery(question, history);
        logQueryRewrite(conversationId, question, history, rewrite);
        RetrievedContext context = retrieveContext(conversationId, question, rewrite, history.size(), receivedAt,
                startNanos);
        TurnDraft draft = draft(conversationId, question, rewrite, history.size(), receivedAt, startNanos,
                context);
        SseEmitter emitter = new SseEmitter(properties.generation().streamTimeout().toMillis());
        if (context.contextChunks().isEmpty()) {
            answerStreamer.streamFixedAnswer(draft, properties.generation().notFoundAnswer(), emitter);
        } else {
            Set<Long> answerChunkIds = locateAnswerChunks(question);
            answerStreamer.streamModelAnswer(draft,
                    promptBuilder.buildPrompt(question, history, context.contextChunks()), emitter, answerChunkIds);
        }
        return emitter;
    }

    private Set<Long> locateAnswerChunks(String question) {
        return goldenAnswerLocator.locateAnswerChunks(List.of(question)).values().stream()
                .findFirst()
                .orElse(Set.of());
    }

    private RetrievedContext retrieveContext(long conversationId, String question, QueryRewriteResult rewrite,
            int historyTurns, Instant receivedAt, long startNanos) {
        try {
            return contextRetriever.retrieveContext(rewrite.searchQuery());
        } catch (ServiceUnavailableException exception) {
            TurnDraft draft = draft(conversationId, question, rewrite, historyTurns, receivedAt, startNanos, null);
            long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            try {
                turnService.recordTurn(draft, TurnOutcome.failed(null, exception.getErrorCode(), null, null, latencyMs));
            } catch (RuntimeException recordFailure) {
                log.error("Turn record failed conversationId={} errorCode={} reason={}", conversationId,
                        exception.getErrorCode(), recordFailure.getClass().getSimpleName());
            }
            throw exception;
        }
    }

    private TurnDraft draft(long conversationId, String question, QueryRewriteResult rewrite, int historyTurns,
            Instant receivedAt, long startNanos, @Nullable RetrievedContext context) {
        return new TurnDraft(conversationId, GenerationMode.RAG, question, rewrite.searchQuery(), rewrite.status(), rewrite.latencyMs(),
                historyTurns, chatModelName, receivedAt, startNanos, context);
    }

    private static void logQueryRewrite(long conversationId, String question, List<HistoryTurn> history,
            QueryRewriteResult rewrite) {
        log.info("Query rewrite conversationId={} status={} historyTurns={} questionLength={} queryLength={} "
                        + "durationMs={}", conversationId, rewrite.status(), history.size(), question.length(),
                rewrite.searchQuery().length(), rewrite.latencyMs());
    }
}
