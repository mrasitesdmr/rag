package com.rasitbank.assistant.handoff.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.repository.ConversationRepository;
import com.rasitbank.assistant.conversation.repository.TurnRepository;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.handoff.domain.dto.HandoffResponse;
import com.rasitbank.assistant.handoff.domain.dto.RequestHandoffRequest;
import com.rasitbank.assistant.handoff.domain.entity.HandoffReason;
import com.rasitbank.assistant.handoff.repository.HandoffRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Cevap yok" turundan sonra konuşmayı yapılandırılmış temsilci kuyruğuna bir kez aktarır; tekrar gelen istekler
 * ilk kaydı döner.
 */
@Service
public class HandoffService {

    private static final Logger log = LoggerFactory.getLogger(HandoffService.class);

    private final ConversationRepository conversationRepository;
    private final TurnRepository turnRepository;
    private final HandoffRepository handoffRepository;
    private final String queue;
    private final Clock clock;

    public HandoffService(ConversationRepository conversationRepository, TurnRepository turnRepository,
            HandoffRepository handoffRepository, AssistantProperties properties, Clock clock) {
        this.conversationRepository = conversationRepository;
        this.turnRepository = turnRepository;
        this.handoffRepository = handoffRepository;
        this.queue = properties.handoff().queue();
        this.clock = clock;
    }

    @Transactional
    public HandoffResponse requestHandoff(long conversationId, RequestHandoffRequest request) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new NotFoundException("CONVERSATION_NOT_FOUND", "Conversation not found: " + conversationId);
        }
        long turnId = Objects.requireNonNull(request.turnId());
        HandoffReason reason = Objects.requireNonNull(request.reason());
        Turn turn = turnRepository.findInConversation(conversationId, turnId)
                .orElseThrow(() -> new NotFoundException("TURN_NOT_FOUND",
                        "Turn not found in conversation: " + turnId));
        if (turn.getErrorCode() != null || !turn.isNotFoundAnswer()) {
            throw new ConflictException("HANDOFF_NOT_ALLOWED", "Turn is not a completed not-found turn: " + turnId);
        }
        boolean created = handoffRepository.insertIfAbsent(conversationId, turnId, reason.name(), queue,
                Instant.now(clock)) == 1;
        HandoffResponse handoff = handoffRepository.findByConversationId(conversationId)
                .map(HandoffResponse::from)
                .orElseThrow(() -> new IllegalStateException("Handoff missing after insert: " + conversationId));
        log.info("Conversation handed off conversationId={} turnId={} queue={} reason={} created={}",
                conversationId, handoff.turnId(), handoff.queue(), handoff.reason(), created);
        return handoff;
    }
}
