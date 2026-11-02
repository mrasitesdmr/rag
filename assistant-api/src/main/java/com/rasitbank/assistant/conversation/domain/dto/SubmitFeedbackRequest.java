package com.rasitbank.assistant.conversation.domain.dto;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Müşterinin bir turun cevabına verdiği "Faydalı mı?" cevabını (Evet / Hayır) taşır.
 */
public record SubmitFeedbackRequest(@NotNull(message = "must not be null") @Nullable Boolean helpful) {
}
