package com.rasitbank.assistant.evaluation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Servis açılırken önceki çalışmada yarım kalan değerlendirme çalıştırmalarını başarısız (yeniden başladı) olarak kapatır.
 */
@Component
public class EvalRunStartupListener {

    private static final Logger log = LoggerFactory.getLogger(EvalRunStartupListener.class);

    private final EvalRunLifecycleService evalRunLifecycleService;

    public EvalRunStartupListener(EvalRunLifecycleService evalRunLifecycleService) {
        this.evalRunLifecycleService = evalRunLifecycleService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedRuns() {
        int count = evalRunLifecycleService.failInterruptedRuns();
        log.info("Interrupted eval runs failed count={}", count);
    }
}
