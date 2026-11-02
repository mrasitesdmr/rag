package com.rasitbank.assistant.conversation.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.Channel;
import com.rasitbank.assistant.handoff.domain.dto.HandoffResponse;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Konuşmalar detay ekranının gövdesidir: konuşmanın kayıtlı turları, cevabı hâlâ üretilen turu ve aktarma kaydı.
 */
public record ConversationDetailResponse(long conversationId, Channel channel, Instant createdAt,
        List<TurnResponse> turns, @Nullable InProgressTurnResponse inProgressTurn, @Nullable HandoffResponse handoff) {

    public ConversationDetailResponse {
        turns = List.copyOf(turns);
    }
}
