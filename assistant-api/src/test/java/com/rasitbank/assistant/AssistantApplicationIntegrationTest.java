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
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.chat.model.ChatModel;
import com.rasitbank.assistant.support.PostgresIntegrationTest;

/**
 * Gerçek sunucu ve gerçek PostgreSQL ile assistant-api iskeletinin ayağa kalktığını doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssistantApplicationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private ChatModel chatModel;

    @LocalServerPort
    private int port;

    @Test
    void givenEmptyDatabase_whenContextStarts_thenFlywayAppliesAllFiveMigrations() {
        assertThat(flyway.info().applied()).hasSize(5);
        assertThat(flyway.info().applied()[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(flyway.info().applied()[1].getVersion().getVersion()).isEqualTo("2");
        assertThat(flyway.info().applied()[2].getVersion().getVersion()).isEqualTo("3");
        assertThat(flyway.info().applied()[3].getVersion().getVersion()).isEqualTo("4");
        assertThat(flyway.info().applied()[4].getVersion().getVersion()).isEqualTo("5");
        assertThat(flyway.info().applied()[4].getState().isApplied()).isTrue();
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void givenDefaultProperties_whenContextStarts_thenOllamaOptionsMatchLab() {
        assertThat(chatModel).isInstanceOf(OllamaChatModel.class);
        var options = ((OllamaChatModel) chatModel).getOptions();

        assertThat(options.getModel()).isEqualTo("qwen3.5:9b");
        assertThat(options.getTemperature()).isEqualTo(0.0);
        assertThat(options.getNumCtx()).isEqualTo(8192);
        assertThat(options.getThinkOption().toJsonValue()).isEqualTo(false);
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
