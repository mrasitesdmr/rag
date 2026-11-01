package com.rasitbank.assistant.conversation.domain.event;

/**
 * Cevap akışı açıldıktan sonra oluşan hatayı bildiren "error" olayıdır.
 */
public record StreamErrorEvent(String errorCode) {
}
