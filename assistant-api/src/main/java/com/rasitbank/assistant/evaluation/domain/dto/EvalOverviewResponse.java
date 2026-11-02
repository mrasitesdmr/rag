package com.rasitbank.assistant.evaluation.domain.dto;

import org.jspecify.annotations.Nullable;

/**
 * Değerlendirme ekranının tek okumasını taşır: golden set özeti, süren ölçüm, son biten ölçüm ve son başarılı ölçüm.
 */
public record EvalOverviewResponse(@Nullable GoldenSetSummaryResponse goldenSet, @Nullable EvalRunResponse runningRun,
        @Nullable EvalRunResponse lastFinishedRun, @Nullable EvalRunDetailResponse lastDoneRun) {
}
