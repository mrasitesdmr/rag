package com.rasitbank.assistant.conversation.repository;

import com.rasitbank.assistant.conversation.domain.entity.Turn;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Konuşma turlarını aday parçalarıyla birlikte saklar; konuşmanın son tur numarasını, cevabı tamamlanmış son turlarını
 * ve Konuşmalar ekranı için turları okur, tura geri bildirimi tek atomik güncellemeyle yazar.
 */
public interface TurnRepository extends JpaRepository<Turn, Long> {

    @Query("select coalesce(max(t.turnNo), 0) from Turn t where t.conversation.id = :conversationId")
    int findMaxTurnNo(@Param("conversationId") long conversationId);

    @Query("select t from Turn t where t.conversation.id = :conversationId and t.errorCode is null "
            + "and t.answer is not null order by t.turnNo desc")
    List<Turn> findAnsweredTurns(@Param("conversationId") long conversationId, Pageable pageable);

    @Query("select t from Turn t where t.id = :turnId and t.conversation.id = :conversationId")
    Optional<Turn> findInConversation(@Param("conversationId") long conversationId, @Param("turnId") long turnId);

    @Query("select t from Turn t where t.conversation.id in :conversationIds and (t.turnNo = 1 or t.turnNo = "
            + "(select max(t2.turnNo) from Turn t2 where t2.conversation = t.conversation))")
    List<Turn> findFirstAndLastTurns(@Param("conversationIds") Collection<Long> conversationIds);

    @Query("select distinct t from Turn t left join fetch t.chunks where t.conversation.id = :conversationId "
            + "order by t.turnNo")
    List<Turn> findAllWithChunks(@Param("conversationId") long conversationId);

    @Modifying
    @Query("update Turn t set t.helpful = :helpful, t.feedbackAt = :feedbackAt where t.id = :turnId")
    int updateFeedback(@Param("turnId") long turnId, @Param("helpful") boolean helpful,
            @Param("feedbackAt") Instant feedbackAt);
}
