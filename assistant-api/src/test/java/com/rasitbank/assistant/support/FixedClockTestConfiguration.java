package com.rasitbank.assistant.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * İş kuralına giren zamanı 2026-11-02T10:31:05Z'ye sabitler; kayıt zamanı kesin değerle doğrulanır.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockTestConfiguration {

    public static final Instant NOW = Instant.parse("2026-11-02T10:31:05Z");

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
