package com.rasitbank.assistant.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * recall@k ve MRR'nin elle türetilmiş değerlerle hesaplandığını doğrular; kaçan soru MRR'ye 0 katar.
 */
class RetrievalMetricsTest {

    private static final double EPSILON = 1e-6;

    @Test
    void givenPromptPositionsOneTwoOneFourAndNone_whenEvaluate_thenRecallIsPointEightAndMrrIsPointFiftyFive() {
        // Arrange: MRR = (1/1 + 1/2 + 1/1 + 1/4 + 0) / 5 = 2.75 / 5 = 0.55
        List<Integer> positions = Arrays.asList(1, 2, 1, 4, null);

        // Act
        RetrievalMetricsResult result = RetrievalMetrics.evaluate(positions);

        // Assert
        assertThat(result.hitCount()).isEqualTo(4);
        assertThat(result.missedCount()).isEqualTo(1);
        assertThat(result.recallAtK()).isCloseTo(0.8, within(EPSILON));
        assertThat(result.mrr()).isCloseTo(0.55, within(EPSILON));
    }

    @Test
    void givenNoPromptPositions_whenEvaluate_thenRecallAndMrrAreZero() {
        // Act
        RetrievalMetricsResult result = RetrievalMetrics.evaluate(Arrays.asList(null, null, null));

        // Assert
        assertThat(result.hitCount()).isZero();
        assertThat(result.missedCount()).isEqualTo(3);
        assertThat(result.recallAtK()).isZero();
        assertThat(result.mrr()).isZero();
    }

    @Test
    void givenSinglePromptPositionThree_whenEvaluate_thenMrrIsOneThird() {
        // Act
        RetrievalMetricsResult result = RetrievalMetrics.evaluate(List.of(3));

        // Assert
        assertThat(result.recallAtK()).isCloseTo(1.0, within(EPSILON));
        assertThat(result.mrr()).isCloseTo(0.333333, within(EPSILON));
    }

    @Test
    void givenHundredQuestionsNinetyEightHit_whenEvaluate_thenRecallIsPointNinetyEight() {
        // Arrange: 85 soru 1. sırada, 13 soru 2. sırada, 2 kaçan → recall 98/100, MRR (85 + 6.5) / 100 = 0.915
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < 85; i++) positions.add(1);
        for (int i = 0; i < 13; i++) positions.add(2);
        positions.add(null);
        positions.add(null);

        // Act
        RetrievalMetricsResult result = RetrievalMetrics.evaluate(positions);

        // Assert
        assertThat(result.recallAtK()).isCloseTo(0.98, within(EPSILON));
        assertThat(result.mrr()).isCloseTo(0.915, within(EPSILON));
        assertThat(result.missedCount()).isEqualTo(2);
    }

    @Test
    void givenEmptyList_whenEvaluate_thenThrowsIllegalArgument() {
        assertThatThrownBy(() -> RetrievalMetrics.evaluate(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenPositionZero_whenEvaluate_thenThrowsIllegalArgument() {
        assertThatThrownBy(() -> RetrievalMetrics.evaluate(List.of(0))).isInstanceOf(IllegalArgumentException.class);
    }
}
