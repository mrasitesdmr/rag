package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.retrieval.client.RerankClient;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.support.ChatResponses;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sadece LLM modunda (assistant.generation.mode=llm) soru gönderme ucunu gerçek sunucu ve PostgreSQL ile sınar:
 * retrieval ve reranker çağrılmaz, SSE ve tur kaydı stratejisiz ve parçasızdır.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "assistant.generation.mode=llm")
class SendMessageLlmOnlyIntegrationTest extends PostgresIntegrationTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private RetrievalClient retrievalClient;

    @MockitoBean
    private RerankClient rerankClient;

    private HttpResponse<String> post(String path, String body, String accept) {
        try {
            return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json").header("Accept", accept)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private long createConversation() {
        HttpResponse<String> response = post("/api/assistant/conversations", "{\"channel\":\"PREVIEW\"}",
                "application/json");
        return json.readTree(response.body()).get("conversationId").asLong();
    }

    private List<JsonNode[]> parseEvents(String body) {
        List<JsonNode[]> events = new ArrayList<>();
        for (String block : body.split("\n\n")) {
            String name = null;
            String data = null;
            for (String line : block.split("\n")) {
                if (line.startsWith("event:")) {
                    name = line.substring("event:".length()).strip();
                } else if (line.startsWith("data:")) {
                    data = line.substring("data:".length()).strip();
                }
            }
            if (name != null && data != null) {
                events.add(new JsonNode[] {json.valueToTree(name), json.readTree(data)});
            }
        }
        return events;
    }

    @Test
    void givenLlmOnlyMode_whenSendMessage_thenSourcesWithoutChunksAndCitations() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Kartını kapat "),
                ChatResponses.textWithUsage("[1].", 63, 9)));
        long conversationId = createConversation();

        // Act
        HttpResponse<String> response = post("/api/assistant/conversations/" + conversationId + "/messages",
                "{\"text\":\"Kartımı kaybettim, ne yapmalıyım?\"}", "text/event-stream, application/problem+json");

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        List<JsonNode[]> events = parseEvents(response.body());
        assertThat(events).extracting(event -> event[0].asString()).containsExactly("token", "token", "sources", "done");
        assertThat(events.get(2)[1].toString()).isEqualTo("{\"generationMode\":\"LLM_ONLY\",\"retrievalStrategy\":\"NONE\","
                + "\"candidates\":null,\"topK\":null,\"searchQuery\":null,\"queryRewriteStatus\":\"DISABLED\","
                + "\"contextChunks\":[],\"citations\":[]}");
        JsonNode done = events.get(3)[1];
        assertThat(done.get("retrievalLatencyMs").isNull()).isTrue();
        assertThat(done.get("rerankLatencyMs").isNull()).isTrue();
        assertThat(done.get("notFoundAnswer").asBoolean()).isFalse();
        verifyNoInteractions(retrievalClient, rerankClient);
    }

    @Test
    void givenLlmOnlyMode_whenSendMessage_thenTurnRowHasNoneStrategyAndNoRetrievalColumns() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.textWithUsage("Kapat [1].", 63, 9)));
        long conversationId = createConversation();

        // Act
        HttpResponse<String> response = post("/api/assistant/conversations/" + conversationId + "/messages",
                "{\"text\":\"Kartımı kaybettim\"}", "text/event-stream");

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        var row = jdbc.queryForMap("select * from conversation_turns where conversation_id = ?", conversationId);
        assertThat(row.get("generation_mode")).isEqualTo("LLM_ONLY");
        assertThat(row.get("retrieval_strategy")).isEqualTo("NONE");
        assertThat(row.get("search_query")).isNull();
        assertThat(row.get("candidates")).isNull();
        assertThat(row.get("top_k")).isNull();
        assertThat(row.get("retrieval_latency_ms")).isNull();
        assertThat(row.get("rerank_latency_ms")).isNull();
        assertThat(row.get("query_rewrite_status")).isEqualTo("DISABLED");
        assertThat(row.get("not_found_answer")).isEqualTo(false);
        assertThat(jdbc.queryForObject("select count(*) from turn_chunks where turn_id = ?", Integer.class,
                row.get("id"))).isZero();
        verifyNoInteractions(retrievalClient, rerankClient);
    }
}
