package com.rasitbank.assistant.conversation.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Cevabı hâlâ üretilen turları konuşma başına bellekte tutar; aynı konuşmada aynı anda tek tur yürümesini sağlar ve
 * Konuşmalar ekranına süren turun sorusunu ve başlama anını verir.
 */
@Component
public class ActiveTurnRegistry {

    private final ConcurrentMap<Long, ActiveTurn> activeTurns = new ConcurrentHashMap<>();

    public boolean tryStart(long conversationId, String question, Instant startedAt) {
        return activeTurns.putIfAbsent(conversationId, new ActiveTurn(question, startedAt)) == null;
    }

    public void finish(long conversationId) {
        activeTurns.remove(conversationId);
    }

    public Optional<ActiveTurn> findActiveTurn(long conversationId) {
        return Optional.ofNullable(activeTurns.get(conversationId));
    }

    public Map<Long, ActiveTurn> activeTurns() {
        return Map.copyOf(activeTurns);
    }
}
