package com.rasitbank.retrieval.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Servis açıldığında önce yarıda kalan içe alma işlerini kapatır, sonra rehberin ilk girişini başlatır.
 */
@Component
public class IngestionStartupListener {

    private static final Logger log = LoggerFactory.getLogger(IngestionStartupListener.class);

    private final IngestionService ingestionService;
    private final GuideBootstrap guideBootstrap;

    public IngestionStartupListener(IngestionService ingestionService, GuideBootstrap guideBootstrap) {
        this.ingestionService = ingestionService;
        this.guideBootstrap = guideBootstrap;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            ingestionService.failInterruptedJobs();
        } catch (RuntimeException exception) {
            log.error("Interrupted ingestion jobs could not be marked failed", exception);
        }
        try {
            guideBootstrap.bootstrapGuide();
        } catch (RuntimeException exception) {
            log.error("Guide bootstrap failed", exception);
        }
    }
}
