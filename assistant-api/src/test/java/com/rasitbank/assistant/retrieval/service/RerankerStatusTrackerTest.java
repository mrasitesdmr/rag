package com.rasitbank.assistant.retrieval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.support.TestProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Reranker durum takipçisinin son ölçümü esas aldığını ve reranker kullanılmayan modlarda ölçümü yok saydığını doğrular.
 */
class RerankerStatusTrackerTest {

    @Test
    void givenRagRerank_whenNothingMeasured_thenUnknown() {
        // Arrange
        RerankerStatusTracker tracker = new RerankerStatusTracker(TestProperties.defaults());

        // Act
        RerankerStatus status = tracker.currentStatus();

        // Assert
        assertThat(status).isEqualTo(RerankerStatus.UNKNOWN);
        assertThat(tracker.isRerankerUsed()).isTrue();
    }

    @Test
    void givenMeasurements_whenCurrentStatus_thenLatestWins() {
        // Arrange
        RerankerStatusTracker tracker = new RerankerStatusTracker(TestProperties.defaults());

        // Act + Assert
        tracker.recordUnavailable();
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.DOWN);
        tracker.recordAvailable();
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.UP);
        tracker.recordUnavailable();
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.DOWN);
    }

    @ParameterizedTest
    @CsvSource({"rag,top5", "llm,rerank", "llm,top5"})
    void givenTopKOrLlmOnly_whenCurrentStatus_thenNotUsedEvenAfterMeasurements(String mode, String strategy) {
        // Arrange
        RerankerStatusTracker tracker = new RerankerStatusTracker(TestProperties.withMode(mode, strategy));

        // Act
        tracker.recordUnavailable();
        tracker.recordAvailable();

        // Assert
        assertThat(tracker.isRerankerUsed()).isFalse();
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.NOT_USED);
    }
}
