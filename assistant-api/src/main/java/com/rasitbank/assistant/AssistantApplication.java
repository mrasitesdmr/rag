package com.rasitbank.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Raşit Bankası dijital asistanının konuşma, cevap ve değerlendirme servisini başlatır.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }
}
