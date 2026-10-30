package com.rasitbank.retrieval.ingestion.service;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Açılışta yarıda kalan işlerin rehberin ilk girişinden önce kapatıldığını doğrular.
 */
class IngestionStartupListenerTest {

    private final IngestionService ingestionService = mock(IngestionService.class);
    private final GuideBootstrap guideBootstrap = mock(GuideBootstrap.class);
    private final IngestionStartupListener listener = new IngestionStartupListener(ingestionService, guideBootstrap);

    @Test
    void givenApplicationReady_whenOnApplicationReady_thenFailsInterruptedJobsBeforeBootstrapping() {
        listener.onApplicationReady();

        InOrder order = inOrder(ingestionService, guideBootstrap);
        order.verify(ingestionService).failInterruptedJobs();
        order.verify(guideBootstrap).bootstrapGuide();
    }

    @Test
    void givenFailInterruptedJobsThrows_whenOnApplicationReady_thenStillBootstrapsAndDoesNotPropagate() {
        willThrow(new IllegalStateException("db down")).given(ingestionService).failInterruptedJobs();

        listener.onApplicationReady();

        verify(guideBootstrap).bootstrapGuide();
    }

    @Test
    void givenBootstrapThrows_whenOnApplicationReady_thenDoesNotPropagate() {
        willThrow(new IllegalStateException("boom")).given(guideBootstrap).bootstrapGuide();

        listener.onApplicationReady();

        verify(ingestionService).failInterruptedJobs();
    }
}
