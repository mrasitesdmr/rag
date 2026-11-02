package com.rasitbank.assistant.evaluation.repository;

import com.rasitbank.assistant.evaluation.domain.entity.GoldenQuestion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Golden set'in sorularını sırasıyla okur ve normalize soru metniyle eşleşen soruları bulur (tek set olduğu için
 * set kimliği gerekmez).
 */
public interface GoldenQuestionRepository extends JpaRepository<GoldenQuestion, Long> {

    List<GoldenQuestion> findByGoldenSetIdOrderByPosition(long goldenSetId);

    List<GoldenQuestion> findByNormalizedTextIn(Collection<String> normalizedTexts);

    int countByGoldenSetId(long goldenSetId);
}
