package com.rasitbank.assistant.suggestion.repository;

import com.rasitbank.assistant.suggestion.domain.entity.SuggestedQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Öneri sorularını saklar ve kategori sırası, ardından soru sırasıyla okur.
 */
public interface SuggestedQuestionRepository extends JpaRepository<SuggestedQuestion, Long> {

    @Query("select q from SuggestedQuestion q join q.category c order by c.position, q.position")
    List<SuggestedQuestion> findAllInDisplayOrder();
}
