package com.rasitbank.assistant.handoff.domain.dto;

import com.rasitbank.assistant.handoff.domain.entity.Handoff;
import com.rasitbank.assistant.handoff.domain.entity.HandoffReason;
import java.time.Instant;

/**
 * Konuşmanın aktarma kaydını istemciye taşır; aynı konuşmanın her isteğinde aynı kayıt döner.
 */
public record HandoffResponse(long conversationId, long turnId, HandoffReason reason, String queue, Instant createdAt) {

    public static HandoffResponse from(Handoff handoff) {
        return new HandoffResponse(handoff.getConversationId(), handoff.getTurnId(), handoff.getReason(),
                handoff.getQueue(), handoff.getCreatedAt());
    }
}
