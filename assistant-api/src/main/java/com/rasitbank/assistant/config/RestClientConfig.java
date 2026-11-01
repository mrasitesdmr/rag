package com.rasitbank.assistant.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * retrieval-api'ye ve reranker'a giden, kendi adresi ve bağlantı/okuma zaman aşımı olan RestClient'ları kurar.
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    public RestClient retrievalRestClient(RestClient.Builder builder, AssistantProperties properties) {
        AssistantProperties.RetrievalApi retrievalApi = properties.retrievalApi();
        return builder.clone()
                .baseUrl(retrievalApi.baseUrl())
                .requestFactory(requestFactory(retrievalApi.connectTimeout(), retrievalApi.readTimeout()))
                .build();
    }

    @Bean
    public RestClient rerankRestClient(RestClient.Builder builder, AssistantProperties properties) {
        AssistantProperties.Reranker reranker = properties.reranker();
        return builder.clone()
                .baseUrl(reranker.baseUrl())
                .requestFactory(requestFactory(reranker.connectTimeout(), reranker.readTimeout()))
                .build();
    }

    private static JdkClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return requestFactory;
    }
}
