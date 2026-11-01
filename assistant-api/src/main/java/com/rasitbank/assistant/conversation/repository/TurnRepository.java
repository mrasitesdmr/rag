package com.rasitbank.assistant.conversation.repository;

import com.rasitbank.assistant.conversation.domain.entity.Turn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Konuşma turlarını aday parçalarıyla birlikte saklar ve konuşmanın son tur numarasını okur.
 */
public interface TurnRepository extends JpaRepository<Turn, Long> {

    @Query("select coalesce(max(t.turnNo), 0) from Turn t where t.conversation.id = :conversationId")
    int findMaxTurnNo(@Param("conversationId") long conversationId);
}
