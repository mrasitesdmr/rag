package com.rasitbank.assistant.conversation.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Cevabı hâlâ üretilen konuşmaları bellekte tutar; aynı konuşmada aynı anda tek tur yürümesini sağlar.
 */
@Component
public class ActiveTurnRegistry {

    private final Set<Long> activeConversationIds = ConcurrentHashMap.newKeySet();

    public boolean tryStart(long conversationId) {
        return activeConversationIds.add(conversationId);
    }

    public void finish(long conversationId) {
        activeConversationIds.remove(conversationId);
    }
}
