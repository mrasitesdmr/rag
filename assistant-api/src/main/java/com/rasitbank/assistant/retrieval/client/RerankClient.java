package com.rasitbank.assistant.retrieval.client;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Aday parçaları reranker'a (llama-server /v1/rerank) puanlatır; puanları gönderilen belge sırasıyla döner,
 * her hatayı ve eksik ya da bozuk yanıtı RERANKER_UNAVAILABLE olarak bildirir. Sağlık ucunu (/health) da yoklar.
 */
@Component
public class RerankClient {

    private static final String RERANK_PATH = "/v1/rerank";
    private static final String HEALTH_PATH = "/health";

    private final RestClient restClient;
    private final String model;

    public RerankClient(@Qualifier("rerankRestClient") RestClient restClient, AssistantProperties properties) {
        this.restClient = restClient;
        this.model = properties.reranker().model();
    }

    public List<Double> rerankChunks(String query, List<String> documents) {
        RerankResponse response;
        try {
            response = restClient.post()
                    .uri(RERANK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new RerankRequest(model, query, documents))
                    .retrieve()
                    .body(RerankResponse.class);
        } catch (RestClientException exception) {
            throw unavailable("Reranker request failed: " + exception.getClass().getSimpleName());
        }
        return alignScores(response, documents.size());
    }

    public boolean isHealthy() {
        try {
            restClient.get()
                    .uri(HEALTH_PATH)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            return false;
        }
    }

    private static List<Double> alignScores(RerankResponse response, int documentCount) {
        if (response == null || response.results() == null) {
            throw unavailable("Reranker returned no results");
        }
        List<RerankResult> results = response.results();
        if (results.size() != documentCount) {
            throw unavailable("Reranker returned " + results.size() + " results for " + documentCount + " documents");
        }
        Double[] scores = new Double[documentCount];
        for (RerankResult result : results) {
            Integer index = result == null ? null : result.index();
            Double score = result == null ? null : result.relevanceScore();
            if (index == null || score == null || index < 0 || index >= documentCount || scores[index] != null) {
                throw unavailable("Reranker returned a missing, duplicate or out of range result");
            }
            scores[index] = score;
        }
        return List.of(scores);
    }

    private static ServiceUnavailableException unavailable(String message) {
        return new ServiceUnavailableException("RERANKER_UNAVAILABLE", message);
    }
}
