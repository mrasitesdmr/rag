package com.rasitbank.assistant.conversation.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.Channel;
import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import java.time.Instant;
import java.util.Objects;

/**
 * Açılan konuşmanın kimliğini, kanalını ve açılış zamanını istemciye taşır.
 */
public record ConversationResponse(long conversationId, Channel channel, Instant createdAt) {

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(Objects.requireNonNull(conversation.getId()), conversation.getChannel(),
                conversation.getCreatedAt());
    }
}
