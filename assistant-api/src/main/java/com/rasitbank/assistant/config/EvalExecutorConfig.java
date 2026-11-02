package com.rasitbank.assistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Değerlendirme çalıştırmalarını arka planda tek tek yürüten tek thread'li executor'ı tanımlar.
 */
@Configuration(proxyBeanMethods = false)
public class EvalExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor evalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("eval-");
        return executor;
    }
}
