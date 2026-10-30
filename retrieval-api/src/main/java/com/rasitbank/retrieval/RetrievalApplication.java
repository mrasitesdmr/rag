package com.rasitbank.retrieval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Raşit Bankası dijital asistanının bilgi bankasını (kaynaklar, parçalar, vektör arama) başlatır.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RetrievalApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetrievalApplication.class, args);
    }
}
