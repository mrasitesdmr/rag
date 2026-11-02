package com.rasitbank.assistant.conversation.repository;

import com.rasitbank.assistant.conversation.domain.entity.Turn;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Konuşma turlarını aday parçalarıyla birlikte saklar; konuşmanın son tur numarasını ve cevabı tamamlanmış son
 * turlarını okur.
 */
public interface TurnRepository extends JpaRepository<Turn, Long> {

    @Query("select coalesce(max(t.turnNo), 0) from Turn t where t.conversation.id = :conversationId")
    int findMaxTurnNo(@Param("conversationId") long conversationId);

    @Query("select t from Turn t where t.conversation.id = :conversationId and t.errorCode is null "
            + "and t.answer is not null order by t.turnNo desc")
    List<Turn> findAnsweredTurns(@Param("conversationId") long conversationId, Pageable pageable);
}
