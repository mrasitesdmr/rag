package com.rasitbank.assistant.evaluation.domain.entity;

/**
 * Değerlendirme çalıştırmasının durumudur: ölçülüyor, metrikleriyle bitti ya da hata koduyla başarısız oldu.
 */
public enum EvalRunStatus {
    RUNNING,
    DONE,
    FAILED
}
