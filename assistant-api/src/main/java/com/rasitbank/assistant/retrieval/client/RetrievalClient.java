package com.rasitbank.assistant.retrieval.client;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * retrieval-api'nin vektör arama ucunu çağırır; her hatayı RETRIEVAL_UNAVAILABLE olarak bildirir.
 */
@Component
public class RetrievalClient {

    private static final Logger log = LoggerFactory.getLogger(RetrievalClient.class);

    private static final String SEARCH_PATH = "/api/workspaces/{workspaceId}/search";

    private final RestClient restClient;
    private final long workspaceId;

    public RetrievalClient(@Qualifier("retrievalRestClient") RestClient restClient, AssistantProperties properties) {
        this.restClient = restClient;
        this.workspaceId = properties.workspaceId();
    }

    public List<SearchHit> search(String query, int topK) {
        long startNanos = System.nanoTime();
        SearchResponse response;
        try {
            response = restClient.post()
                    .uri(SEARCH_PATH, workspaceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new SearchChunksRequest(query, topK))
                    .retrieve()
                    .body(SearchResponse.class);
        } catch (RestClientResponseException exception) {
            log.warn("Retrieval unavailable status={} errorCode={} reason={}", exception.getStatusCode().value(),
                    upstreamErrorCode(exception), exception.getClass().getSimpleName());
            throw unavailable("retrieval-api answered " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            log.warn("Retrieval unavailable status={} errorCode={} reason={}", null, null,
                    exception.getClass().getSimpleName());
            throw unavailable("retrieval-api could not be reached");
        }
        if (response == null || response.hits() == null) {
            log.warn("Retrieval unavailable status={} errorCode={} reason={}", 200, null, "EmptyBody");
            throw unavailable("retrieval-api returned no search result");
        }
        List<SearchHit> hits = List.copyOf(response.hits());
        log.info("Retrieval search finished topK={} hits={} durationMs={}", topK, hits.size(),
                Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
        return hits;
    }

    private static @Nullable String upstreamErrorCode(RestClientResponseException exception) {
        try {
            UpstreamProblem problem = exception.getResponseBodyAs(UpstreamProblem.class);
            return problem == null ? null : problem.errorCode();
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    private static ServiceUnavailableException unavailable(String message) {
        return new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", message);
    }

    record UpstreamProblem(@Nullable String errorCode) {
    }
}
