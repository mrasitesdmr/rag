package com.rasitbank.assistant.conversation.domain.entity;

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
 * Müşterinin bir kanaldan açtığı konuşmayı temsil eder; turlar bu konuşmaya eklenir.
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Conversation() {
    }

    private Conversation(Channel channel, Instant createdAt) {
        this.channel = channel;
        this.createdAt = createdAt;
    }

    public static Conversation open(Channel channel, Instant createdAt) {
        return new Conversation(channel, createdAt);
    }

    public @Nullable Long getId() {
        return id;
    }

    public Channel getChannel() {
        return channel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
