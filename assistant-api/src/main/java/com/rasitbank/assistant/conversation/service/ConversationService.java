package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.conversation.domain.dto.ConversationResponse;
import com.rasitbank.assistant.conversation.domain.dto.CreateConversationRequest;
import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import com.rasitbank.assistant.conversation.repository.ConversationRepository;
import com.rasitbank.assistant.exception.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Konuşma açar ve var olan konuşmayı getirir.
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final Clock clock;

    public ConversationService(ConversationRepository conversationRepository, Clock clock) {
        this.conversationRepository = conversationRepository;
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
}
