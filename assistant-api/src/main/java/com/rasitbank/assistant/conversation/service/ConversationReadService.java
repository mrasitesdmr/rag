package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.conversation.domain.dto.ConversationDetailResponse;
import com.rasitbank.assistant.conversation.domain.dto.ConversationSummaryResponse;
import com.rasitbank.assistant.conversation.domain.dto.InProgressTurnResponse;
import com.rasitbank.assistant.conversation.domain.dto.TurnResponse;
import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.domain.entity.TurnStatus;
import com.rasitbank.assistant.conversation.repository.ConversationRepository;
import com.rasitbank.assistant.conversation.repository.TurnRepository;
import com.rasitbank.assistant.evaluation.service.GoldenAnswerLocator;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.handoff.domain.dto.HandoffResponse;
import com.rasitbank.assistant.handoff.repository.HandoffRepository;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Konuşmalar ekranının okuma tarafıdır: kayıtlı turlardan ve bellekteki süren turlardan konuşma listesini ve konuşma
 * detayını, tur durumları ve golden set'teki cevap parçası işaretleriyle birlikte kurar. Veritabanını kısa bir okuma
 * transaction'ında okur, retrieval-api'ye transaction kapandıktan sonra gider.
 */
@Service
public class ConversationReadService {

    private static final Logger log = LoggerFactory.getLogger(ConversationReadService.class);

    private final ConversationRepository conversationRepository;
    private final TurnRepository turnRepository;
    private final HandoffRepository handoffRepository;
    private final ActiveTurnRegistry activeTurnRegistry;
    private final TurnStatusResolver turnStatusResolver;
    private final GoldenAnswerLocator goldenAnswerLocator;
    private final TransactionTemplate readOnlyTransaction;

    public ConversationReadService(ConversationRepository conversationRepository, TurnRepository turnRepository,
            HandoffRepository handoffRepository, ActiveTurnRegistry activeTurnRegistry,
            TurnStatusResolver turnStatusResolver, GoldenAnswerLocator goldenAnswerLocator,
            PlatformTransactionManager transactionManager) {
        this.conversationRepository = conversationRepository;
        this.turnRepository = turnRepository;
        this.handoffRepository = handoffRepository;
        this.activeTurnRegistry = activeTurnRegistry;
        this.turnStatusResolver = turnStatusResolver;
        this.goldenAnswerLocator = goldenAnswerLocator;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    public Page<ConversationSummaryResponse> listConversations(int page, int size) {
        Map<Long, ActiveTurn> activeTurns = activeTurnRegistry.activeTurns();
        ConversationPage conversationPage = Objects.requireNonNull(
                readOnlyTransaction.execute(status -> readConversationPage(activeTurns, PageRequest.of(page, size))));
        Map<String, Set<Long>> answerChunks = goldenAnswerLocator.locateAnswerChunks(
                conversationPage.notFoundLastTurns().stream().map(Turn::getQuestion).toList());
        Page<ConversationSummaryResponse> summaries = conversationPage.conversations().map(conversation -> {
            long conversationId = Objects.requireNonNull(conversation.getId());
            return toSummary(conversation, conversationPage.turnsByConversation().getOrDefault(conversationId, List.of()),
                    activeTurns.get(conversationId), conversationPage.handedOffIds().contains(conversationId),
                    answerChunks);
        });
        log.debug("Conversations listed page={} size={} returned={}", page, size, summaries.getNumberOfElements());
        return summaries;
    }

    public ConversationDetailResponse getConversationDetail(long conversationId) {
        @Nullable ActiveTurn activeTurn = activeTurnRegistry.findActiveTurn(conversationId).orElse(null);
        ConversationDetail detail = Objects.requireNonNull(
                readOnlyTransaction.execute(status -> readConversationDetail(conversationId)));
        List<Turn> turns = detail.turns();
        @Nullable Turn lastTurn = turns.isEmpty() ? null : turns.getLast();
        @Nullable InProgressTurnResponse inProgressTurn = activeTurn != null && isCurrent(activeTurn, lastTurn)
                ? new InProgressTurnResponse(turns.size() + 1, activeTurn.question(), activeTurn.startedAt())
                : null;
        Map<String, Set<Long>> answerChunks = goldenAnswerLocator.locateAnswerChunks(
                turns.stream().map(Turn::getQuestion).toList());
        List<TurnResponse> turnResponses = turns.stream()
                .map(turn -> {
                    Set<Long> answerChunkIds = answerChunkIdsOf(turn, answerChunks);
                    return TurnResponse.from(turn, turnStatusResolver.resolve(turn, answerChunkIds), answerChunkIds);
                })
                .toList();
        Conversation conversation = detail.conversation();
        return new ConversationDetailResponse(conversationId, conversation.getChannel(), conversation.getCreatedAt(),
                turnResponses, inProgressTurn, detail.handoff());
    }

    private ConversationPage readConversationPage(Map<Long, ActiveTurn> activeTurns, Pageable pageable) {
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
        List<Turn> notFoundLastTurns = turnsByConversation.entrySet().stream()
                .map(entry -> lastTurnOf(entry.getValue()))
                .filter(Objects::nonNull)
                .filter(turn -> !isCurrentTurnInProgress(activeTurns.get(turn.getConversation().getId()), turn))
                .filter(ConversationReadService::isRagNotFoundAnswer)
                .toList();
        if (!notFoundLastTurns.isEmpty()) {
            // The fetch join fills the chunk collections of the already loaded turns before the transaction closes.
            turnRepository.findAllWithChunksByIdIn(notFoundLastTurns.stream()
                    .map(turn -> Objects.requireNonNull(turn.getId()))
                    .toList());
        }
        return new ConversationPage(conversations, turnsByConversation, handedOffIds, notFoundLastTurns);
    }

    private ConversationDetail readConversationDetail(long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("CONVERSATION_NOT_FOUND",
                        "Conversation not found: " + conversationId));
        List<Turn> turns = turnRepository.findAllWithChunks(conversationId);
        @Nullable HandoffResponse handoff = handoffRepository.findByConversationId(conversationId)
                .map(HandoffResponse::from)
                .orElse(null);
        return new ConversationDetail(conversation, turns, handoff);
    }

    private ConversationSummaryResponse toSummary(Conversation conversation, List<Turn> firstAndLastTurns,
            @Nullable ActiveTurn activeTurn, boolean handedOff, Map<String, Set<Long>> answerChunks) {
        @Nullable Turn firstTurn = firstAndLastTurns.stream().min(Comparator.comparingInt(Turn::getTurnNo))
                .orElse(null);
        @Nullable Turn lastTurn = lastTurnOf(firstAndLastTurns);
        boolean inProgress = isCurrentTurnInProgress(activeTurn, lastTurn);
        int recordedTurns = lastTurn == null ? 0 : lastTurn.getTurnNo();
        String firstQuestion = firstTurn != null ? firstTurn.getQuestion()
                : Objects.requireNonNull(activeTurn).question();
        TurnStatus lastTurnStatus = inProgress ? TurnStatus.IN_PROGRESS
                : resolveLastTurnStatus(Objects.requireNonNull(lastTurn), answerChunks);
        return new ConversationSummaryResponse(Objects.requireNonNull(conversation.getId()),
                conversation.getChannel(), conversation.getCreatedAt(), recordedTurns + (inProgress ? 1 : 0),
                firstQuestion, lastTurnStatus, handedOff);
    }

    private TurnStatus resolveLastTurnStatus(Turn lastTurn, Map<String, Set<Long>> answerChunks) {
        Set<Long> answerChunkIds = isRagNotFoundAnswer(lastTurn) ? answerChunkIdsOf(lastTurn, answerChunks) : Set.of();
        return turnStatusResolver.resolve(lastTurn, answerChunkIds);
    }

    private static Set<Long> answerChunkIdsOf(Turn turn, Map<String, Set<Long>> answerChunks) {
        return answerChunks.getOrDefault(SuggestionTextNormalizer.normalize(turn.getQuestion()), Set.of());
    }

    private static @Nullable Turn lastTurnOf(List<Turn> turns) {
        return turns.stream().max(Comparator.comparingInt(Turn::getTurnNo)).orElse(null);
    }

    private static boolean isRagNotFoundAnswer(Turn turn) {
        return turn.getErrorCode() == null && turn.getGenerationMode() == GenerationMode.RAG
                && turn.isNotFoundAnswer();
    }

    private static boolean isCurrentTurnInProgress(@Nullable ActiveTurn activeTurn, @Nullable Turn lastTurn) {
        return activeTurn != null && isCurrent(activeTurn, lastTurn);
    }

    private static boolean isCurrent(ActiveTurn activeTurn, @Nullable Turn lastTurn) {
        // The turn is recorded before the registry entry is removed; a recorded turn at or after the start is the same turn.
        return lastTurn == null
                || lastTurn.getCreatedAt().isBefore(activeTurn.startedAt().truncatedTo(ChronoUnit.MICROS));
    }

    private record ConversationPage(Page<Conversation> conversations, Map<Long, List<Turn>> turnsByConversation,
            Set<Long> handedOffIds, List<Turn> notFoundLastTurns) {
    }

    private record ConversationDetail(Conversation conversation, List<Turn> turns, @Nullable HandoffResponse handoff) {
    }
}
