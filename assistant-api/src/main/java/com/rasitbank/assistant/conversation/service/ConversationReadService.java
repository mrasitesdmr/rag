package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.conversation.domain.dto.ConversationDetailResponse;
import com.rasitbank.assistant.conversation.domain.dto.ConversationSummaryResponse;
import com.rasitbank.assistant.conversation.domain.dto.InProgressTurnResponse;
import com.rasitbank.assistant.conversation.domain.dto.TurnResponse;
import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.domain.entity.TurnStatus;
import com.rasitbank.assistant.conversation.repository.ConversationRepository;
import com.rasitbank.assistant.conversation.repository.TurnRepository;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.handoff.domain.dto.HandoffResponse;
import com.rasitbank.assistant.handoff.repository.HandoffRepository;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Konuşmalar ekranının okuma tarafıdır: kayıtlı turlardan ve bellekteki süren turlardan konuşma listesini ve konuşma
 * detayını, tur durumlarıyla birlikte kurar.
 */
@Service
@Transactional(readOnly = true)
public class ConversationReadService {

    private static final Logger log = LoggerFactory.getLogger(ConversationReadService.class);

    private final ConversationRepository conversationRepository;
    private final TurnRepository turnRepository;
    private final HandoffRepository handoffRepository;
    private final ActiveTurnRegistry activeTurnRegistry;
    private final TurnStatusResolver turnStatusResolver;

    public ConversationReadService(ConversationRepository conversationRepository, TurnRepository turnRepository,
            HandoffRepository handoffRepository, ActiveTurnRegistry activeTurnRegistry,
            TurnStatusResolver turnStatusResolver) {
        this.conversationRepository = conversationRepository;
        this.turnRepository = turnRepository;
        this.handoffRepository = handoffRepository;
        this.activeTurnRegistry = activeTurnRegistry;
        this.turnStatusResolver = turnStatusResolver;
    }

    public Page<ConversationSummaryResponse> listConversations(int page, int size) {
        Map<Long, ActiveTurn> activeTurns = activeTurnRegistry.activeTurns();
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversations = activeTurns.isEmpty()
                ? conversationRepository.findWithTurns(pageable)
                : conversationRepository.findWithTurnsOrIdIn(activeTurns.keySet(), pageable);
        List<Long> conversationIds = conversations.getContent().stream()
                .map(conversation -> Objects.requireNonNull(conversation.getId()))
                .toList();
        Map<Long, List<Turn>> turnsByConversation = conversationIds.isEmpty() ? Map.of()
                : turnRepository.findFirstAndLastTurns(conversationIds).stream()
                        .collect(Collectors.groupingBy(turn -> Objects.requireNonNull(turn.getConversation().getId())));
        Set<Long> handedOffIds = conversationIds.isEmpty() ? Set.of()
                : handoffRepository.findHandedOffConversationIds(conversationIds);
        Page<ConversationSummaryResponse> summaries = conversations.map(conversation -> {
            long conversationId = Objects.requireNonNull(conversation.getId());
            return toSummary(conversation, turnsByConversation.getOrDefault(conversationId, List.of()),
                    activeTurns.get(conversationId), handedOffIds.contains(conversationId));
        });
        log.debug("Conversations listed page={} size={} returned={}", page, size, summaries.getNumberOfElements());
        return summaries;
    }

    public ConversationDetailResponse getConversationDetail(long conversationId) {
        @Nullable ActiveTurn activeTurn = activeTurnRegistry.findActiveTurn(conversationId).orElse(null);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("CONVERSATION_NOT_FOUND",
                        "Conversation not found: " + conversationId));
        List<Turn> turns = turnRepository.findAllWithChunks(conversationId);
        @Nullable Turn lastTurn = turns.isEmpty() ? null : turns.getLast();
        @Nullable InProgressTurnResponse inProgressTurn = activeTurn != null && isCurrent(activeTurn, lastTurn)
                ? new InProgressTurnResponse(turns.size() + 1, activeTurn.question(), activeTurn.startedAt())
                : null;
        @Nullable HandoffResponse handoff = handoffRepository.findByConversationId(conversationId)
                .map(HandoffResponse::from)
                .orElse(null);
        List<TurnResponse> turnResponses = turns.stream()
                .map(turn -> TurnResponse.from(turn, turnStatusResolver.resolve(turn)))
                .toList();
        return new ConversationDetailResponse(conversationId, conversation.getChannel(), conversation.getCreatedAt(),
                turnResponses, inProgressTurn, handoff);
    }

    private ConversationSummaryResponse toSummary(Conversation conversation, List<Turn> firstAndLastTurns,
            @Nullable ActiveTurn activeTurn, boolean handedOff) {
        @Nullable Turn firstTurn = firstAndLastTurns.stream().min(Comparator.comparingInt(Turn::getTurnNo))
                .orElse(null);
        @Nullable Turn lastTurn = firstAndLastTurns.stream().max(Comparator.comparingInt(Turn::getTurnNo))
                .orElse(null);
        boolean inProgress = activeTurn != null && isCurrent(activeTurn, lastTurn);
        int recordedTurns = lastTurn == null ? 0 : lastTurn.getTurnNo();
        String firstQuestion = firstTurn != null ? firstTurn.getQuestion()
                : Objects.requireNonNull(activeTurn).question();
        TurnStatus lastTurnStatus = inProgress ? TurnStatus.IN_PROGRESS
                : turnStatusResolver.resolve(Objects.requireNonNull(lastTurn));
        return new ConversationSummaryResponse(Objects.requireNonNull(conversation.getId()),
                conversation.getChannel(), conversation.getCreatedAt(), recordedTurns + (inProgress ? 1 : 0),
                firstQuestion, lastTurnStatus, handedOff);
    }

    private static boolean isCurrent(ActiveTurn activeTurn, @Nullable Turn lastTurn) {
        // The turn is recorded before the registry entry is removed; a recorded turn at or after the start is the same turn.
        return lastTurn == null
                || lastTurn.getCreatedAt().isBefore(activeTurn.startedAt().truncatedTo(ChronoUnit.MICROS));
    }
}
