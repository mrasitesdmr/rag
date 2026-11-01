package com.rasitbank.assistant.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * İş kuralına giren zamanın okunduğu UTC saatini sağlar; testte sabit saatle değiştirilir.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
