package com.rasitbank.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Gerçek sunucu ve gerçek PostgreSQL ile assistant-api iskeletinin ayağa kalktığını doğrular.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssistantApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("pgvector/pgvector:0.8.1-pg18");

    @Autowired
    private Flyway flyway;

    @LocalServerPort
    private int port;

    @Test
    void givenEmptyDatabase_whenContextStarts_thenFlywayRunsWithNoMigrations() {
        assertThat(flyway.info().applied()).isEmpty();
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void givenRunningApplication_whenGetActuatorHealth_thenReturnsUp() {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"").doesNotContain("components");
    }

    @Test
    void givenRunningApplication_whenGetUnknownAssistantPath_thenReturns404NotFoundProblem() {
        HttpResponse<String> response = get("/api/assistant/yok");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(response.body()).contains("\"errorCode\":\"NOT_FOUND\"");
    }

    private HttpResponse<String> get(String path) {
        try {
            return HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
