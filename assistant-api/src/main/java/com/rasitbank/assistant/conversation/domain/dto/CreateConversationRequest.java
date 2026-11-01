package com.rasitbank.assistant.conversation.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.Channel;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Yeni konuşma açma isteğini taşır: konuşmanın açıldığı kanal.
 */
public record CreateConversationRequest(@NotNull(message = "must not be null") @Nullable Channel channel) {
}
