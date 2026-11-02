package com.rasitbank.assistant.evaluation.service;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Soruların doğru parçasının prompt'taki yerlerinden recall@k ve MRR'yi hesaplar; prompt'a girmeyen soru isabet
 * sayılmaz ve MRR'ye 0 katar.
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    public static RetrievalMetricsResult evaluate(List<@Nullable Integer> promptPositions) {
        if (promptPositions.isEmpty()) {
            throw new IllegalArgumentException("At least one measured question is required");
        }
        int hitCount = 0;
        double reciprocalRankSum = 0;
        for (Integer promptPosition : promptPositions) {
            if (promptPosition == null) {
                continue;
            }
            if (promptPosition < 1) {
                throw new IllegalArgumentException("Prompt position must be at least 1: " + promptPosition);
            }
            hitCount++;
            reciprocalRankSum += 1.0 / promptPosition;
        }
        int questionCount = promptPositions.size();
        return new RetrievalMetricsResult(hitCount, questionCount - hitCount, (double) hitCount / questionCount,
                reciprocalRankSum / questionCount);
    }
}
