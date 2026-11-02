package com.rasitbank.assistant.conversation.domain.dto;

import com.rasitbank.assistant.conversation.domain.entity.Channel;
import com.rasitbank.assistant.conversation.domain.entity.TurnStatus;
import java.time.Instant;

/**
 * Konuşmalar listesinin bir satırıdır: konuşma kimliği, kanal, açılış zamanı, tur sayısı, ilk soru, son turun durumu
 * ve temsilciye aktarılıp aktarılmadığı.
 */
public record ConversationSummaryResponse(long conversationId, Channel channel, Instant createdAt, int turnCount,
        String firstQuestion, TurnStatus lastTurnStatus, boolean handedOff) {
}
