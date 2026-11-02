package com.rasitbank.assistant.evaluation.repository;

import com.rasitbank.assistant.evaluation.domain.entity.EvalRunQuestion;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunQuestionId;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Çalıştırmaya kopyalanan soruları saklar; ölçülecek soruları ve "Kaçanlar ve sırası değişenler" satırlarını golden set
 * sırasıyla okur.
 */
public interface EvalRunQuestionRepository extends JpaRepository<EvalRunQuestion, EvalRunQuestionId> {

    @Query("select q from EvalRunQuestion q where q.id.evalRunId = :evalRunId and q.measured = true"
            + " order by q.id.position")
    List<EvalRunQuestion> findMeasuredByEvalRunIdOrderByPosition(@Param("evalRunId") long evalRunId);

    @Query("select q from EvalRunQuestion q where q.id.evalRunId = :evalRunId and q.measured = true"
            + " and (q.promptPosition is null or q.vectorRank is null or q.promptPosition <> q.vectorRank)"
            + " order by q.id.position")
    List<EvalRunQuestion> findMissedAndMoved(@Param("evalRunId") long evalRunId, Pageable pageable);
}
