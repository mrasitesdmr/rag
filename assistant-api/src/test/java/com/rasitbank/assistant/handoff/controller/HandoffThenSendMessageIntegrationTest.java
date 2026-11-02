package com.rasitbank.assistant.handoff.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.rasitbank.assistant.retrieval.client.RerankClient;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.support.ChatResponses;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.SearchHitFixtures;
import com.rasitbank.assistant.support.TurnFixtures;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gerçek akışla oluşan "cevap yok" turunun aktarılabildiğini ve aktarılan konuşmada yeni sorunun yine cevaplandığını
 * (aktarma konuşmayı kilitlemez, temsilci mesajı üretilmez) doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HandoffThenSendMessageIntegrationTest extends PostgresIntegrationTest {

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

    @BeforeEach
    void clean() {
        new TurnFixtures(jdbc).clean();
    }

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

    private long turnIdFromDone(String sse) {
        String doneBlock = Arrays.stream(sse.split("\n\n")).filter(block -> block.contains("event:done") || block.contains("event: done")).findFirst()
                .orElseThrow();
        String data = Arrays.stream(doneBlock.split("\n")).filter(line -> line.startsWith("data:")).findFirst()
                .orElseThrow().substring("data:".length()).strip();
        return json.readTree(data).get("turnId").asLong();
    }

    @Test
    void givenHandedOffConversation_whenSendMessage_thenAnswerStreams() {
        long conversationId = json.readTree(post("/api/assistant/conversations", "{\"channel\":\"PREVIEW\"}",
                "application/json").body()).get("conversationId").asLong();
        given(retrievalClient.search(anyString(), anyInt())).willReturn(List.of());
        HttpResponse<String> notFound = post("/api/assistant/conversations/" + conversationId + "/messages",
                "{\"text\":\"Bitcoin alıp satabilir miyim?\"}", "text/event-stream, application/problem+json");
        long notFoundTurnId = turnIdFromDone(notFound.body());
        HttpResponse<String> handoff = post("/api/assistant/conversations/" + conversationId + "/handoff",
                "{\"reason\":\"NO_ANSWER\",\"turnId\":" + notFoundTurnId + "}", "application/json");
        assertThat(handoff.statusCode()).isEqualTo(202);
        String handoffRow = jdbc.queryForObject("select turn_id || '/' || queue from handoffs", String.class);

        given(retrievalClient.search(anyString(), eq(20))).willReturn(SearchHitFixtures.hits(20));
        Double[] scores = new Double[20];
        Arrays.fill(scores, 1.0);
        given(rerankClient.rerankChunks(anyString(), anyList())).willReturn(List.of(scores));
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Kartını kapat [1].")));
        HttpResponse<String> next = post("/api/assistant/conversations/" + conversationId + "/messages",
                "{\"text\":\"Kartımı kaybettim, ne yapmalıyım?\"}", "text/event-stream, application/problem+json");

        assertThat(next.statusCode()).isEqualTo(200);
        assertThat(next.body()).containsPattern("event: ?token").containsPattern("event: ?sources")
                .containsPattern("event: ?done");
        assertThat(jdbc.queryForObject("select count(*) from conversation_turns where conversation_id = ?",
                Integer.class, conversationId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from handoffs", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select turn_id || '/' || queue from handoffs", String.class))
                .isEqualTo(handoffRow).isEqualTo(notFoundTurnId + "/bireysel");
    }
}
