package com.rasitbank.assistant.conversation.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Müşterinin konuşmaya gönderdiği soruyu taşır.
 */
public record SendMessageRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = SendMessageRequest.TEXT_MAX_LENGTH, message = "size must be at most 1000 characters")
        @Nullable String text) {

    public static final int TEXT_MAX_LENGTH = 1000;
}
