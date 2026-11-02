package com.rasitbank.assistant.evaluation.repository;

import com.rasitbank.assistant.evaluation.domain.entity.GoldenSet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Kayıtlı golden set'i saklar; set değiştirilirken eşzamanlı yüklemeleri sıraya sokan tablo kilidini alır.
 */
public interface GoldenSetRepository extends JpaRepository<GoldenSet, Long> {

    Optional<GoldenSet> findFirstByOrderByIdDesc();

    @Modifying
    @Query(value = "LOCK TABLE golden_sets IN EXCLUSIVE MODE", nativeQuery = true)
    void lockForReplace();
}
