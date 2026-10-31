package com.rasitbank.retrieval.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Servis açıldığında, önceki çalışmada yarıda kalan içe alma işlerini kapatır; başka bir şey yazmaz.
 */
@Component
public class IngestionStartupListener {

    private static final Logger log = LoggerFactory.getLogger(IngestionStartupListener.class);

    private final IngestionService ingestionService;

    public IngestionStartupListener(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            ingestionService.failInterruptedJobs();
        } catch (RuntimeException exception) {
            log.error("Interrupted ingestion jobs could not be marked failed", exception);
        }
    }
}
