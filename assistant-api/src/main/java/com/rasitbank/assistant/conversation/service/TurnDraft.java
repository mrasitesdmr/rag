package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.generation.MessageLabel;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Cevap üretilirken turun baştan bilinen kısmını taşır: konuşma, üretim modu, soru, arama sorgusu ve yeniden yazmanın
 * sonucu, geçmişe giren tur sayısı, dil modeli, isteğin geliş zamanı, retrieval sonucu (retrieval başarısızsa, Sadece
 * LLM turunda ya da kalıp listesiyle tanınan selamlaşma turunda boş) ve varsa mesaj etiketi ile etiket çağrısının
 * süresi.
 */
public record TurnDraft(
        long conversationId,
        GenerationMode generationMode,
        String question,
        @Nullable String searchQuery,
        QueryRewriteStatus queryRewriteStatus,
        @Nullable Long queryRewriteLatencyMs,
        int historyTurns,
        String chatModel,
        Instant receivedAt,
        long startNanos,
        @Nullable RetrievedContext context,
        @Nullable MessageLabel messageLabel,
        @Nullable Long messageLabelLatencyMs) {
}
