package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.dto.SendMessageRequest;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.generation.PromptBuilder;
import com.rasitbank.assistant.retrieval.service.ContextRetriever;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Müşterinin sorusunu RAG hattından geçirir: akış açılmadan önce konuşmayı, süren turu ve retrieval'ı denetler,
 * sonra cevabı SSE akışı olarak başlatır. Model çağrısı sırasında transaction açık tutmaz.
 */
@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    private final ConversationService conversationService;
    private final ActiveTurnRegistry activeTurnRegistry;
    private final ContextRetriever contextRetriever;
    private final PromptBuilder promptBuilder;
    private final AnswerStreamer answerStreamer;
    private final TurnService turnService;
    private final AssistantProperties properties;
    private final String chatModelName;
    private final Clock clock;

    public AnswerService(ConversationService conversationService, ActiveTurnRegistry activeTurnRegistry,
            ContextRetriever contextRetriever, PromptBuilder promptBuilder, AnswerStreamer answerStreamer,
            TurnService turnService, AssistantProperties properties,
            @Value("${spring.ai.ollama.chat.model}") String chatModelName, Clock clock) {
        this.conversationService = conversationService;
        this.activeTurnRegistry = activeTurnRegistry;
        this.contextRetriever = contextRetriever;
        this.promptBuilder = promptBuilder;
        this.answerStreamer = answerStreamer;
        this.turnService = turnService;
        this.properties = properties;
        this.chatModelName = chatModelName;
        this.clock = clock;
    }

    public SseEmitter sendMessage(long conversationId, SendMessageRequest request) {
        long startNanos = System.nanoTime();
        Instant receivedAt = Instant.now(clock);
        conversationService.getConversation(conversationId);
        String question = Objects.requireNonNull(request.text()).strip();
        if (!activeTurnRegistry.tryStart(conversationId)) {
            throw new ConflictException("TURN_IN_PROGRESS",
                    "An answer is still being produced in conversation: " + conversationId);
        }
        boolean streamStarted = false;
        try {
            RetrievedContext context = retrieveContext(conversationId, question, receivedAt, startNanos);
            TurnDraft draft = new TurnDraft(conversationId, question, question, chatModelName, receivedAt, startNanos,
                    context);
            SseEmitter emitter = new SseEmitter(properties.generation().streamTimeout().toMillis());
            if (context.contextChunks().isEmpty()) {
                answerStreamer.streamFixedAnswer(draft, properties.generation().notFoundAnswer(), emitter);
            } else {
                answerStreamer.streamModelAnswer(draft, promptBuilder.buildPrompt(question, context.contextChunks()),
                        emitter);
            }
            streamStarted = true;
            return emitter;
        } finally {
            if (!streamStarted) {
                activeTurnRegistry.finish(conversationId);
            }
        }
    }

    private RetrievedContext retrieveContext(long conversationId, String question, Instant receivedAt,
            long startNanos) {
        try {
            return contextRetriever.retrieveContext(question);
        } catch (ServiceUnavailableException exception) {
            TurnDraft draft = new TurnDraft(conversationId, question, question, chatModelName, receivedAt, startNanos,
                    null);
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
}
