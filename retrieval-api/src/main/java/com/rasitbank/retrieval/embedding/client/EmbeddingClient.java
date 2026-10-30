package com.rasitbank.retrieval.embedding.client;

import com.rasitbank.retrieval.config.EmbeddingProperties;
import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Metinleri Ollama'daki embedding modeline toplu gönderip vektörlerini döner; eşzamanlı istekleri sınırlar,
 * geçici hatalarda yeniden dener ve vektör sayısını ve boyutunu doğrular.
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private static final String EMBED_PATH = "/api/embed";
    private static final String EMBEDDING_UNAVAILABLE = "EMBEDDING_UNAVAILABLE";

    private final RestClient restClient;
    private final EmbeddingProperties properties;
    private final Semaphore requestPermits;

    public EmbeddingClient(RestClient embeddingRestClient, EmbeddingProperties properties) {
        this.restClient = embeddingRestClient;
        this.properties = properties;
        this.requestPermits = new Semaphore(properties.maxConcurrentRequests(), true);
    }

    public List<float[]> embed(List<String> inputs) {
        if (inputs.isEmpty()) {
            return List.of();
        }
        OllamaEmbedRequest request = new OllamaEmbedRequest(properties.model(), inputs);
        for (int attempt = 1; ; attempt++) {
            try {
                return validate(send(request), inputs.size());
            } catch (ResourceAccessException | HttpServerErrorException exception) {
                log.warn("Embedding request failed attempt={}/{} status={}", attempt, properties.maxAttempts(),
                        failureStatus(exception));
                if (attempt >= properties.maxAttempts()) {
                    throw unavailable("Embedding model did not answer after " + attempt + " attempts");
                }
                waitBeforeRetry(attempt);
            } catch (RestClientException exception) {
                log.warn("Embedding request failed attempt={}/{} status={}", attempt, properties.maxAttempts(),
                        failureStatus(exception));
                throw unavailable("Embedding model rejected the request");
            }
        }
    }

    public String modelLabel() {
        return properties.modelLabel();
    }

    private OllamaEmbedResponse send(OllamaEmbedRequest request) {
        acquirePermit();
        try {
            OllamaEmbedResponse response = restClient.post()
                    .uri(EMBED_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OllamaEmbedResponse.class);
            if (response == null) {
                throw unavailable("Embedding model returned an empty body");
            }
            return response;
        } finally {
            requestPermits.release();
        }
    }

    private List<float[]> validate(OllamaEmbedResponse response, int expectedCount) {
        List<float[]> embeddings = response.embeddings();
        if (embeddings == null || embeddings.size() != expectedCount) {
            throw unavailable("Embedding model returned " + (embeddings == null ? 0 : embeddings.size())
                    + " vectors for " + expectedCount + " inputs");
        }
        for (float[] embedding : embeddings) {
            if (embedding == null || embedding.length != properties.dimensions()) {
                throw new ServiceUnavailableException("EMBEDDING_DIMENSION_MISMATCH",
                        "Embedding model returned a vector of " + (embedding == null ? 0 : embedding.length)
                                + " dimensions, expected " + properties.dimensions());
            }
        }
        return List.copyOf(embeddings);
    }

    private void acquirePermit() {
        Duration timeout = properties.connectTimeout().plus(properties.readTimeout());
        try {
            if (!requestPermits.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw unavailable("Timed out waiting for an embedding request slot");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("Interrupted while waiting for an embedding request slot");
        }
    }

    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(properties.retryBackoff().multipliedBy(attempt));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("Interrupted while waiting to retry the embedding request");
        }
    }

    private static String failureStatus(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return String.valueOf(responseException.getStatusCode().value());
        }
        Throwable cause = exception.getCause();
        return (cause != null ? cause : exception).getClass().getSimpleName();
    }

    private static ServiceUnavailableException unavailable(String message) {
        return new ServiceUnavailableException(EMBEDDING_UNAVAILABLE, message);
    }
}
