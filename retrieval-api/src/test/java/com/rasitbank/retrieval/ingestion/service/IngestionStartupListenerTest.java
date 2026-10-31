package com.rasitbank.retrieval.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

/**
 * Açılışta yalnızca yarıda kalan işlerin kapatıldığını, başka hiçbir iş başlatılmadığını doğrular.
 */
class IngestionStartupListenerTest {

    private final IngestionService ingestionService = mock(IngestionService.class);
    private final IngestionStartupListener listener = new IngestionStartupListener(ingestionService);

    @Test
    void givenApplicationReady_whenOnApplicationReady_thenOnlyFailsInterruptedJobs() {
        // Act
        listener.onApplicationReady();

        // Assert
        then(ingestionService).should().failInterruptedJobs();
        then(ingestionService).shouldHaveNoMoreInteractions();
    }

    @Test
    void givenFailInterruptedJobsThrows_whenOnApplicationReady_thenDoesNotPropagate() {
        // Arrange
        given(ingestionService.failInterruptedJobs()).willThrow(new IllegalStateException("db down"));

        // Act
        Throwable thrown = catchThrowable(listener::onApplicationReady);

        // Assert
        assertThat(thrown).isNull();
    }
}
