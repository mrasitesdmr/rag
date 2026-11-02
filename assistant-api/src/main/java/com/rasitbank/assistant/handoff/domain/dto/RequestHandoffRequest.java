package com.rasitbank.assistant.handoff.domain.dto;

import com.rasitbank.assistant.handoff.domain.entity.HandoffReason;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * "Temsilciye bağlan" isteğini taşır: düğmenin altında durduğu "cevap yok" turu ve aktarma sebebi.
 */
public record RequestHandoffRequest(
        @NotNull(message = "must not be null") @Nullable Long turnId,
        @NotNull(message = "must not be null") @Nullable HandoffReason reason) {
}
