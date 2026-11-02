package com.rasitbank.assistant.conversation.domain.dto;

import java.time.Instant;

/**
 * Cevabı hâlâ üretilen turu Konuşmalar detayına taşır; turun henüz kimliği ve kaydı yoktur.
 */
public record InProgressTurnResponse(int turnNo, String question, Instant startedAt) {
}
