package com.rasitbank.assistant.evaluation.domain.dto;

import com.rasitbank.assistant.evaluation.domain.entity.GoldenSet;
import java.time.Instant;

/**
 * Kayıtlı golden set'in özetini taşır: sürüm, soru sayısı ve son yüklenme zamanı.
 */
public record GoldenSetSummaryResponse(String version, int questionCount, Instant loadedAt) {

    public static GoldenSetSummaryResponse from(GoldenSet goldenSet, int questionCount) {
        return new GoldenSetSummaryResponse(goldenSet.getVersion(), questionCount, goldenSet.getLoadedAt());
    }
}
