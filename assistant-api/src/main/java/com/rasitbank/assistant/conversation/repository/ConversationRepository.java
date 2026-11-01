package com.rasitbank.assistant.conversation.repository;

import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Konuşmaları veritabanında saklar ve okur.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
