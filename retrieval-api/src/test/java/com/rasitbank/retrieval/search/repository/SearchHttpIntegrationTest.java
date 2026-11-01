package com.rasitbank.retrieval.search.repository;

import static com.rasitbank.retrieval.fixture.SearchFixtures.e0;
import static com.rasitbank.retrieval.fixture.SearchFixtures.e1;
import static com.rasitbank.retrieval.fixture.SearchFixtures.insertChunk;
import static com.rasitbank.retrieval.fixture.SearchFixtures.insertSource;
import static com.rasitbank.retrieval.fixture.SearchFixtures.insertVersion;
import static com.rasitbank.retrieval.fixture.SearchFixtures.mixed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.rasitbank.retrieval.fixture.SearchFixtures;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gerçek sunucu ve veritabanı üzerinden arama ucunun yanıtını, hiçbir şey yazmadığını ve hata gövdelerini doğrular.
 */
class SearchHttpIntegrationTest extends AbstractSearchIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @BeforeEach
    void stubModelLabel() {
        given(embeddingClient.modelLabel()).willReturn(SearchFixtures.MODEL);
    }

    private HttpResponse<String> postSearch(String body) {
        try {
            return HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/workspaces/42/search"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private long count(String table) {
        Long count = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0 : count;
    }

    @Test
    void givenSeededChunks_whenPostSearch_thenReturnsHitsAndWritesNothing() {
        // Arrange
        insertSource(jdbc, 10001, "Rehber");
        long versionId = insertVersion(jdbc, 10001, "2026.10", true);
        insertChunk(jdbc, 1, 10001, versionId, 0, e1());
        insertChunk(jdbc, 2, 10001, versionId, 1, mixed());
        insertChunk(jdbc, 3, 10001, versionId, 2, e0());
        given(embeddingClient.embed(List.of("kart"))).willReturn(List.<float[]>of(e0()));
        List<Long> before = List.of(count("sources"), count("source_versions"), count("chunks"),
                count("ingestion_jobs"));

        // Act
        HttpResponse<String> response = postSearch("{\"query\":\" kart \",\"topK\":2}");

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode hits = JSON.readTree(response.body()).get("hits");
        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).get("chunkId").asLong()).isEqualTo(3);
        assertThat(hits.get(0).get("sourceName").asString()).isEqualTo("Rehber");
        assertThat(hits.get(0).get("version").asString()).isEqualTo("2026.10");
        assertThat(hits.get(0).get("distance").asDouble()).isCloseTo(0.0, org.assertj.core.api.Assertions.within(1e-6));
        assertThat(hits.get(1).get("chunkId").asLong()).isEqualTo(2);
        assertThat(hits.get(1).get("distance").asDouble()).isCloseTo(0.2, org.assertj.core.api.Assertions.within(1e-6));
        assertThat(List.of(count("sources"), count("source_versions"), count("chunks"), count("ingestion_jobs")))
                .isEqualTo(before);
    }

    @Test
    void givenEmptyKnowledgeBase_whenPostSearch_thenReturnsNoHitsAndEmbeddingNotCalled() {
        // Act
        HttpResponse<String> response = postSearch("{\"query\":\"kart\",\"topK\":5}");

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(response.body()).get("hits")).isEmpty();
        then(embeddingClient).should(never()).embed(anyList());
    }

    @Test
    void givenEmbeddingUnavailable_whenPostSearch_thenReturns503ProblemDetail() {
        // Arrange
        insertSource(jdbc, 10001, "Rehber");
        insertVersion(jdbc, 10001, "1", true);
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE", "down"));

        // Act
        HttpResponse<String> response = postSearch("{\"query\":\"kart\",\"topK\":5}");

        // Assert
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("EMBEDDING_UNAVAILABLE");
    }

    @Test
    void givenTopKZero_whenPostSearch_thenReturns400AndEmbeddingNotCalled() {
        // Act
        HttpResponse<String> response = postSearch("{\"query\":\"kart\",\"topK\":0}");

        // Assert
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JSON.readTree(response.body()).get("violations").get(0).get("field").asString()).isEqualTo("topK");
        then(embeddingClient).should(never()).embed(anyList());
    }
}
