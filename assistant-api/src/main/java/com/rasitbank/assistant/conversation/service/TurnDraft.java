package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Cevap üretilirken turun baştan bilinen kısmını taşır: konuşma, soru, arama sorgusu, dil modeli, isteğin geliş
 * zamanı ve retrieval sonucu (retrieval başarısızsa boş).
 */
public record TurnDraft(
        long conversationId,
        String question,
        String searchQuery,
        String chatModel,
        Instant receivedAt,
        long startNanos,
        @Nullable RetrievedContext context) {
}
