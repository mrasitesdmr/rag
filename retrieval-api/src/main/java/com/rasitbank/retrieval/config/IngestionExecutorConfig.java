package com.rasitbank.retrieval.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * İçe alma işlerini arka planda tek tek yürüten tek thread'li executor'ı kurar.
 */
@Configuration(proxyBeanMethods = false)
public class IngestionExecutorConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService ingestionExecutor() {
        return Executors.newSingleThreadExecutor(Thread.ofPlatform().name("ingestion-", 1).factory());
    }
}
