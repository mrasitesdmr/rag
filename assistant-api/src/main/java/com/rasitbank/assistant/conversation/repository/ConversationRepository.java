package com.rasitbank.assistant.conversation.repository;

import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Konuşmaları veritabanında saklar ve okur; Konuşmalar listesi için en az bir turu olan konuşmaları en yeniden eskiye
 * sayfalı getirir.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query(value = "select c from Conversation c where exists (select t.id from Turn t where t.conversation = c) "
            + "order by c.createdAt desc, c.id desc",
            countQuery = "select count(c) from Conversation c "
                    + "where exists (select t.id from Turn t where t.conversation = c)")
    Page<Conversation> findWithTurns(Pageable pageable);

    @Query(value = "select c from Conversation c where exists (select t.id from Turn t where t.conversation = c) "
            + "or c.id in :conversationIds order by c.createdAt desc, c.id desc",
            countQuery = "select count(c) from Conversation c "
                    + "where exists (select t.id from Turn t where t.conversation = c) or c.id in :conversationIds")
    Page<Conversation> findWithTurnsOrIdIn(@Param("conversationIds") Collection<Long> conversationIds,
            Pageable pageable);
}
