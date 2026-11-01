package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.domain.entity.TurnChunkValues;
import com.rasitbank.assistant.conversation.domain.entity.TurnValues;
import com.rasitbank.assistant.conversation.repository.ConversationRepository;
import com.rasitbank.assistant.conversation.repository.TurnRepository;
import com.rasitbank.assistant.retrieval.service.RetrievedChunk;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cevap akışı bitince turu aday parçalarıyla birlikte tek bir kısa transaction'da kaydeder; içinde dış çağrı
 * yapılmaz.
 */
@Service
public class TurnService {

    private static final Logger log = LoggerFactory.getLogger(TurnService.class);

    private static final String STREAM_CANCELLED = "STREAM_CANCELLED";

    private final ConversationRepository conversationRepository;
    private final TurnRepository turnRepository;
    private final AssistantProperties.Retrieval retrievalSettings;
    private final Clock clock;

    public TurnService(ConversationRepository conversationRepository, TurnRepository turnRepository,
            AssistantProperties properties, Clock clock) {
        this.conversationRepository = conversationRepository;
        this.turnRepository = turnRepository;
        this.retrievalSettings = properties.retrieval();
        this.clock = clock;
    }

    @Transactional
    public long recordTurn(TurnDraft draft, TurnOutcome outcome) {
        Conversation conversation = conversationRepository.getReferenceById(draft.conversationId());
        int turnNo = turnRepository.findMaxTurnNo(draft.conversationId()) + 1;
        RetrievedContext context = draft.context();
        TurnValues values = new TurnValues(
                draft.question(),
                draft.searchQuery(),
                GenerationMode.RAG,
                context != null ? context.strategy() : retrievalSettings.configuredStrategy(),
                context != null ? context.candidates() : retrievalSettings.candidates(),
                context != null ? context.topK() : retrievalSettings.topK(),
                draft.chatModel(),
                outcome.answer(),
                outcome.notFoundAnswer(),
                outcome.citations(),
                outcome.promptTokens(),
                outcome.completionTokens(),
                context != null ? toIntMs(context.retrievalLatencyMs()) : null,
                context != null ? toIntMs(context.rerankLatencyMs()) : null,
                toIntMs(outcome.generationLatencyMs()),
                toIntMs(outcome.timeToFirstTokenMs()),
                Math.toIntExact(outcome.latencyMs()),
                outcome.errorCode(),
                draft.receivedAt(),
                Instant.now(clock));
        Turn turn = turnRepository.save(Turn.record(conversation, turnNo, values, chunkValues(context)));
        long turnId = Objects.requireNonNull(turn.getId());
        logRecordedTurn(draft, outcome, turnId);
        return turnId;
    }

    private static List<TurnChunkValues> chunkValues(@Nullable RetrievedContext context) {
        if (context == null) {
            return List.of();
        }
        Map<Integer, Integer> promptPositions = new HashMap<>();
        List<RetrievedChunk> contextChunks = context.contextChunks();
        for (int i = 0; i < contextChunks.size(); i++) {
            promptPositions.put(contextChunks.get(i).vectorRank(), i + 1);
        }
        return context.candidateChunks().stream()
                .map(chunk -> new TurnChunkValues(
                        chunk.vectorRank(),
                        chunk.hit().chunkId(),
                        chunk.hit().sourceId(),
                        chunk.hit().version(),
                        chunk.hit().chunkNo(),
                        chunk.hit().pageNumber(),
                        chunk.hit().heading(),
                        chunk.hit().distance(),
                        chunk.rerankScore(),
                        promptPositions.get(chunk.vectorRank())))
                .toList();
    }

    private static void logRecordedTurn(TurnDraft draft, TurnOutcome outcome, long turnId) {
        int answerLength = outcome.answer() == null ? 0 : outcome.answer().length();
        if (outcome.errorCode() == null) {
            RetrievedContext context = Objects.requireNonNull(draft.context());
            log.info("Turn completed conversationId={} turnId={} strategy={} questionLength={} answerLength={} "
                            + "promptTokens={} completionTokens={} citedCount={} notFoundAnswer={} latencyMs={}",
                    draft.conversationId(), turnId, context.strategy(), draft.question().length(), answerLength,
                    outcome.promptTokens(), outcome.completionTokens(), outcome.citations().size(),
                    outcome.notFoundAnswer(), outcome.latencyMs());
        } else if (STREAM_CANCELLED.equals(outcome.errorCode())) {
            log.info("Turn cancelled conversationId={} turnId={} answerLength={}", draft.conversationId(), turnId,
                    answerLength);
        } else {
            log.warn("Turn failed conversationId={} turnId={} errorCode={} answerLength={}", draft.conversationId(),
                    turnId, outcome.errorCode(), answerLength);
        }
    }

    private static @Nullable Integer toIntMs(@Nullable Long milliseconds) {
        return milliseconds == null ? null : Math.toIntExact(milliseconds);
    }
}
