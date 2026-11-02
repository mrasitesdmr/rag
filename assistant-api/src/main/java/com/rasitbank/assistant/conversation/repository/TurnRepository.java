package com.rasitbank.assistant.conversation.repository;

import com.rasitbank.assistant.conversation.domain.entity.Turn;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Konuşma turlarını aday parçalarıyla birlikte saklar; konuşmanın son tur numarasını ve cevabı tamamlanmış son
 * turlarını okur, tura geri bildirimi tek atomik güncellemeyle yazar.
 */
public interface TurnRepository extends JpaRepository<Turn, Long> {

    @Query("select coalesce(max(t.turnNo), 0) from Turn t where t.conversation.id = :conversationId")
    int findMaxTurnNo(@Param("conversationId") long conversationId);

    @Query("select t from Turn t where t.conversation.id = :conversationId and t.errorCode is null "
            + "and t.answer is not null order by t.turnNo desc")
    List<Turn> findAnsweredTurns(@Param("conversationId") long conversationId, Pageable pageable);

    @Query("select t from Turn t where t.id = :turnId and t.conversation.id = :conversationId")
    Optional<Turn> findInConversation(@Param("conversationId") long conversationId, @Param("turnId") long turnId);

    @Modifying
    @Query("update Turn t set t.helpful = :helpful, t.feedbackAt = :feedbackAt where t.id = :turnId")
    int updateFeedback(@Param("turnId") long turnId, @Param("helpful") boolean helpful,
            @Param("feedbackAt") Instant feedbackAt);
}
