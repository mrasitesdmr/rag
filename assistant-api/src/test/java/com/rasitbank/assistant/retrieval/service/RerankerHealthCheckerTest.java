package com.rasitbank.assistant.retrieval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.rasitbank.assistant.retrieval.client.RerankClient;
import com.rasitbank.assistant.support.TestProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Periyodik sağlık kontrolünün sonucu takipçiye yazdığını ve reranker kullanılmıyorsa istek atmadığını doğrular.
 */
@ExtendWith(MockitoExtension.class)
class RerankerHealthCheckerTest {

    @Mock
    private RerankClient rerankClient;

    @Test
    void givenHealthy_whenCheck_thenUp() {
        // Arrange
        RerankerStatusTracker tracker = new RerankerStatusTracker(TestProperties.defaults());
        given(rerankClient.isHealthy()).willReturn(true);

        // Act
        new RerankerHealthChecker(rerankClient, tracker).checkRerankerHealth();

        // Assert
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.UP);
    }

    @Test
    void givenUnhealthy_whenCheck_thenDown() {
        // Arrange
        RerankerStatusTracker tracker = new RerankerStatusTracker(TestProperties.defaults());
        given(rerankClient.isHealthy()).willReturn(false);

        // Act
        new RerankerHealthChecker(rerankClient, tracker).checkRerankerHealth();

        // Assert
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.DOWN);
    }

    @Test
    void givenTopK_whenCheck_thenNoRequest() {
        // Arrange
        RerankerStatusTracker tracker = new RerankerStatusTracker(TestProperties.withStrategy("top5"));

        // Act
        new RerankerHealthChecker(rerankClient, tracker).checkRerankerHealth();

        // Assert
        then(rerankClient).should(never()).isHealthy();
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.NOT_USED);
    }

    @Test
    void givenLlmOnly_whenCheck_thenNoRequest() {
        // Arrange
        RerankerStatusTracker tracker = new RerankerStatusTracker(TestProperties.withMode("llm", "rerank"));

        // Act
        new RerankerHealthChecker(rerankClient, tracker).checkRerankerHealth();

        // Assert
        then(rerankClient).should(never()).isHealthy();
    }
}
