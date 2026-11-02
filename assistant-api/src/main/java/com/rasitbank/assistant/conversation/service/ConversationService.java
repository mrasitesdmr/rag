package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.dto.ConversationResponse;
import com.rasitbank.assistant.conversation.domain.dto.CreateConversationRequest;
import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.repository.ConversationRepository;
import com.rasitbank.assistant.conversation.repository.TurnRepository;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.generation.CitationParser;
import com.rasitbank.assistant.generation.HistoryTurn;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Konuşma açar, var olan konuşmayı getirir ve konuşmanın cevabı tamamlanmış son turlarını geçmiş olarak okur.
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final TurnRepository turnRepository;
    private final CitationParser citationParser;
    private final int historyMaxTurns;
    private final Clock clock;

    public ConversationService(ConversationRepository conversationRepository, TurnRepository turnRepository,
            CitationParser citationParser, AssistantProperties properties, Clock clock) {
        this.conversationRepository = conversationRepository;
        this.turnRepository = turnRepository;
        this.citationParser = citationParser;
        this.historyMaxTurns = properties.history().maxTurns();
        this.clock = clock;
    }

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        Conversation conversation = conversationRepository.save(
                Conversation.open(Objects.requireNonNull(request.channel()), Instant.now(clock)));
        log.info("Conversation created conversationId={} channel={}", conversation.getId(), conversation.getChannel());
        return ConversationResponse.from(conversation);
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("CONVERSATION_NOT_FOUND",
                        "Conversation not found: " + conversationId));
    }

    @Transactional(readOnly = true)
    public List<HistoryTurn> getHistory(long conversationId) {
        List<Turn> latestFirst = turnRepository.findAnsweredTurns(conversationId, PageRequest.of(0, historyMaxTurns));
        return latestFirst.reversed().stream()
                .map(turn -> new HistoryTurn(turn.getQuestion(),
                        citationParser.removeCitations(Objects.requireNonNull(turn.getAnswer()))))
                .toList();
    }
}
