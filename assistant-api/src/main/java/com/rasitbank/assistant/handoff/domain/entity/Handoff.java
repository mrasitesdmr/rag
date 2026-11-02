package com.rasitbank.assistant.handoff.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Konuşmanın temsilci kuyruğuna aktarıldığı kayıttır: aktarmayı başlatan tur, sebep, kuyruk ve zaman; konuşma başına
 * en fazla bir tanedir.
 */
@Entity
@Table(name = "handoffs")
public class Handoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(name = "conversation_id", nullable = false)
    private long conversationId;

    @Column(name = "turn_id", nullable = false)
    private long turnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private HandoffReason reason;

    @Column(name = "queue", nullable = false)
    private String queue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Handoff() {
    }

    public @Nullable Long getId() {
        return id;
    }

    public long getConversationId() {
        return conversationId;
    }

    public long getTurnId() {
        return turnId;
    }

    public HandoffReason getReason() {
        return reason;
    }

    public String getQueue() {
        return queue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
