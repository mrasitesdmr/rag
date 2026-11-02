package com.rasitbank.assistant.handoff.repository;

import com.rasitbank.assistant.handoff.domain.entity.Handoff;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Aktarma kayıtlarını saklar; konuşma zaten aktarılmışsa yeni kayıt eklemeyen tek sorguyla yazar ve konuşmanın
 * kaydını okur.
 */
public interface HandoffRepository extends JpaRepository<Handoff, Long> {

    Optional<Handoff> findByConversationId(long conversationId);

    @Modifying
    @Query(nativeQuery = true, value = "insert into handoffs (conversation_id, turn_id, reason, queue, created_at) "
            + "values (:conversationId, :turnId, :reason, :queue, :createdAt) "
            + "on conflict (conversation_id) do nothing")
    int insertIfAbsent(@Param("conversationId") long conversationId, @Param("turnId") long turnId,
            @Param("reason") String reason, @Param("queue") String queue, @Param("createdAt") Instant createdAt);
}
