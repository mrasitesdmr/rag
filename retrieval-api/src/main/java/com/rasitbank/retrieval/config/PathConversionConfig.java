package com.rasitbank.retrieval.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

/**
 * Yapılandırmadaki dosya yollarını (ör. rehber PDF'i) classpath kaynağı sanmadan dosya sistemi yoluna çevirir.
 */
@Configuration(proxyBeanMethods = false)
public class PathConversionConfig {

    @Bean
    @ConfigurationPropertiesBinding
    public static Converter<String, Path> stringToPathConverter() {
        return Path::of;
    }
}
