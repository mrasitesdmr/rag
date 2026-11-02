package com.rasitbank.assistant.suggestion.repository;

import com.rasitbank.assistant.suggestion.domain.entity.SuggestionCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Öneri kategorilerini saklar; liste değiştirilirken eşzamanlı yüklemeleri sıraya sokan tablo kilidini alır.
 */
public interface SuggestionCategoryRepository extends JpaRepository<SuggestionCategory, Long> {

    List<SuggestionCategory> findAllByOrderByPositionAsc();

    @Modifying
    @Query(value = "LOCK TABLE suggestion_categories IN EXCLUSIVE MODE", nativeQuery = true)
    void lockForReplace();
}
