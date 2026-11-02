package com.rasitbank.assistant.suggestion.repository;

import com.rasitbank.assistant.suggestion.domain.entity.SuggestedFollowUp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Takip sorularını saklar ve kategori, soru ve takip sırasıyla okur.
 */
public interface SuggestedFollowUpRepository extends JpaRepository<SuggestedFollowUp, Long> {

    @Query("select f from SuggestedFollowUp f join f.question q join q.category c "
            + "order by c.position, q.position, f.position")
    List<SuggestedFollowUp> findAllInDisplayOrder();
}
