package com.rasitbank.assistant.retrieval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.support.ContextFixtures;
import com.rasitbank.assistant.support.TestProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Reranker puan eşiğinin sadece rerank stratejisinde, ilk prompt parçasının puanı eşikten küçükse devreye girdiğini;
 * eşitlikte, eşik boşken, top-5'te, yedek yolda ve parçasız bağlamda girmediğini doğrular.
 */
class RerankScoreGateTest {

    @ParameterizedTest(name = "[{index}] top {0} with min -2.0 -> below={1}")
    @CsvSource({"-2.5, true", "-2.0, false", "-1.9, false", "-4.0, true", "3.4, false"})
    void givenTopRerankScoreBelowEqualOrAboveThreshold_whenIsBelowThreshold_thenOnlyBelowIsTrue(double topScore,
            boolean expected) {
        // Arrange
        RerankScoreGate gate = new RerankScoreGate(TestProperties.defaults());

        // Act
        boolean below = gate.isBelowThreshold(ContextFixtures.rerankContextWithTopScore(topScore));

        // Assert
        assertThat(below).isEqualTo(expected);
    }

    @Test
    void givenTopKContext_whenIsBelowThreshold_thenFalse() {
        // Arrange
        RerankScoreGate gate = new RerankScoreGate(TestProperties.defaults());

        // Act
        boolean below = gate.isBelowThreshold(ContextFixtures.topKContext());

        // Assert
        assertThat(below).isFalse();
    }

    @Test
    void givenRerankFallbackContext_whenIsBelowThreshold_thenFalseEvenWithLowScores() {
        // Arrange: strateji yedek yol; puanlar eşiğin altında olsa bile eşik uygulanmaz
        RerankScoreGate gate = new RerankScoreGate(TestProperties.defaults());
        RetrievedContext scored = ContextFixtures.rerankContextWithTopScore(-9.0);
        RetrievedContext fallback = new RetrievedContext(RetrievalStrategy.RERANK_FALLBACK, 20, 5,
                scored.candidateChunks(), scored.contextChunks(), 10L, null);

        // Act
        boolean below = gate.isBelowThreshold(fallback);

        // Assert
        assertThat(below).isFalse();
    }

    @Test
    void givenEmptyContext_whenIsBelowThreshold_thenFalse() {
        // Arrange
        RerankScoreGate gate = new RerankScoreGate(TestProperties.defaults());

        // Act
        boolean below = gate.isBelowThreshold(ContextFixtures.emptyContext(RetrievalStrategy.RERANK));

        // Assert
        assertThat(below).isFalse();
    }

    @Test
    void givenNoThresholdConfigured_whenIsBelowThreshold_thenFalseEvenForVeryLowScore() {
        // Arrange
        RerankScoreGate gate = new RerankScoreGate(TestProperties.withMinRerankScore(null));

        // Act
        boolean below = gate.isBelowThreshold(ContextFixtures.rerankContextWithTopScore(-9.0));

        // Assert
        assertThat(below).isFalse();
        assertThat(gate.minRerankScore()).isNull();
    }
}
