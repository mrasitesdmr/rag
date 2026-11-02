package com.rasitbank.assistant.retrieval.service;

import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import org.jspecify.annotations.Nullable;

/**
 * Sorgu yeniden yazmanın sonucunu taşır: durum, aramada kullanılacak sorgu ve model çağrıldıysa süresi.
 */
public record QueryRewriteResult(QueryRewriteStatus status, String searchQuery, @Nullable Long latencyMs) {
}
