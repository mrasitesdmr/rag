package com.rasitbank.assistant.evaluation.repository;

import com.rasitbank.assistant.evaluation.domain.entity.EvalRun;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Değerlendirme çalıştırmalarını saklar; süren, son biten ve son başarılı çalıştırmayı ve en yeni önce sıralı listeyi
 * okur.
 */
public interface EvalRunRepository extends JpaRepository<EvalRun, Long> {

    boolean existsByStatus(EvalRunStatus status);

    Optional<EvalRun> findFirstByStatus(EvalRunStatus status);

    List<EvalRun> findAllByStatus(EvalRunStatus status);

    Optional<EvalRun> findFirstByStatusInOrderByStartedAtDescIdDesc(Collection<EvalRunStatus> statuses);

    Optional<EvalRun> findFirstByStatusOrderByStartedAtDescIdDesc(EvalRunStatus status);

    Page<EvalRun> findAllByOrderByStartedAtDescIdDesc(Pageable pageable);
}
