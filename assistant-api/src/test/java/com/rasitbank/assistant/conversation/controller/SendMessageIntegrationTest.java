package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.client.RerankClient;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.support.ChatResponses;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.SearchHitFixtures;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Soru gönderme ucunu gerçek sunucu, gerçek PostgreSQL ve sahte model/retrieval/reranker ile uçtan uca sınar:
 * SSE biçimi ve başlıkları, doğrulama, Problem Details, tur kaydı, eşzamanlı soru ve istemci kopması.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SendMessageIntegrationTest extends PostgresIntegrationTest {

    private static final String SSE_ACCEPT = "text/event-stream, application/problem+json";
    private static final String QUESTION = "Kartımı kaybettim, ne yapmalıyım?";

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

    record SseEvent(String name, JsonNode data) {
    }

    private void stubHappyPath() {
        given(retrievalClient.search(anyString(), eq(20))).willReturn(SearchHitFixtures.hits(20));
        Double[] scores = new Double[20];
        Arrays.fill(scores, 1.0);
        scores[6] = 9.1;
        scores[1] = 8.0;
        scores[14] = 7.5;
        scores[0] = 6.0;
        scores[2] = 5.0;
        given(rerankClient.rerankChunks(anyString(), anyList())).willReturn(List.of(scores));
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Kartını "),
                ChatResponses.textWithUsage("kapat [1].", 2154, 61)));
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

    private long createConversation() {
        HttpResponse<String> response = post("/api/assistant/conversations", "{\"channel\":\"PREVIEW\"}",
                "application/json");
        assertThat(response.statusCode()).isEqualTo(201);
        return json.readTree(response.body()).get("conversationId").asLong();
    }

    private HttpResponse<String> sendMessage(long conversationId, String text) {
        String body = "{\"text\":" + json.writeValueAsString(text) + "}";
        return post("/api/assistant/conversations/" + conversationId + "/messages", body, SSE_ACCEPT);
    }

    private List<SseEvent> parseEvents(String body) {
        List<SseEvent> events = new ArrayList<>();
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
                events.add(new SseEvent(name, json.readTree(data)));
            }
        }
        return events;
    }

    private int turnCount(long conversationId) {
        return jdbc.queryForObject("select count(*) from conversation_turns where conversation_id = ?",
                Integer.class, conversationId);
    }

    @Test
    void givenFakeModel_whenSendMessage_thenEventStreamWithTokenSourcesDone() {
        stubHappyPath();
        long conversationId = createConversation();

        HttpResponse<String> response = sendMessage(conversationId, QUESTION);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/event-stream");
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-cache");
        assertThat(response.headers().firstValue("X-Accel-Buffering")).hasValue("no");
        List<SseEvent> events = parseEvents(response.body());
        assertThat(events).extracting(SseEvent::name).containsExactly("token", "token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).isEqualTo("Kartını ");
        assertThat(events.get(1).data().get("text").asString()).isEqualTo("kapat [1].");
        JsonNode sources = events.get(2).data();
        assertThat(sources.propertyNames().iterator().next()).isEqualTo("generationMode");
        assertThat(sources.get("generationMode").asString()).isEqualTo("RAG");
        assertThat(sources.get("retrievalStrategy").asString()).isEqualTo("RERANK");
        assertThat(sources.get("candidates").asInt()).isEqualTo(20);
        assertThat(sources.get("topK").asInt()).isEqualTo(5);
        assertThat(sources.get("citations").toString()).isEqualTo("[1]");
        assertThat(sources.get("contextChunks")).hasSize(5);
        assertThat(sources.get("contextChunks").get(0).get("vectorRank").asInt()).isEqualTo(7);
        assertThat(sources.get("contextChunks").get(0).get("chunkId").asLong()).isEqualTo(107L);
        assertThat(sources.get("contextChunks").get(0).get("n").asInt()).isEqualTo(1);
        JsonNode done = events.get(3).data();
        long turnId = jdbc.queryForObject("select id from conversation_turns where conversation_id = ?", Long.class,
                conversationId);
        assertThat(done.get("turnId").asLong()).isEqualTo(turnId);
        assertThat(done.get("promptTokens").asInt()).isEqualTo(2154);
        assertThat(done.get("completionTokens").asInt()).isEqualTo(61);
        assertThat(done.get("notFoundAnswer").asBoolean()).isFalse();
        assertThat(done.get("chatModel").asString()).isEqualTo("qwen3.5:9b");
        assertThat(done.has("hostLabel")).isTrue();
        assertThat(jdbc.queryForObject("select answer from conversation_turns where id = ?", String.class, turnId))
                .isEqualTo("Kartını kapat [1].");
    }

    @Test
    void givenEmptyKnowledgeBase_whenSendMessage_thenNotFoundSentenceStreamedWithoutModelCall() {
        given(retrievalClient.search(anyString(), anyInt())).willReturn(List.of());
        long conversationId = createConversation();

        HttpResponse<String> response = sendMessage(conversationId, "Bitcoin alıp satabilir miyim?");

        List<SseEvent> events = parseEvents(response.body());
        assertThat(events).extracting(SseEvent::name).containsExactly("token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).isEqualTo("Bu konuda rehberimizde bilgi bulamadım.");
        assertThat(events.get(1).data().get("contextChunks")).isEmpty();
        assertThat(events.get(2).data().get("notFoundAnswer").asBoolean()).isTrue();
        verify(chatModel, never()).stream(any(Prompt.class));
        verify(rerankClient, never()).rerankChunks(anyString(), anyList());
        assertThat(jdbc.queryForObject("select not_found_answer from conversation_turns where conversation_id = ?",
                Boolean.class, conversationId)).isTrue();
    }

    @Test
    void givenBlankText_whenSendMessage_thenReturns400() {
        long conversationId = createConversation();

        HttpResponse<String> response = sendMessage(conversationId, "   ");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        JsonNode problem = json.readTree(response.body());
        assertThat(problem.get("errorCode").asString()).isEqualTo("VALIDATION_FAILED");
        assertThat(problem.get("violations").get(0).get("field").asString()).isEqualTo("text");
        assertThat(problem.get("violations").get(0).get("message").asString()).isEqualTo("must not be blank");
        assertThat(turnCount(conversationId)).isZero();
    }

    @Test
    void given1001Chars_whenSendMessage_thenReturns400() {
        long conversationId = createConversation();

        HttpResponse<String> response = sendMessage(conversationId, "a".repeat(1001));

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode problem = json.readTree(response.body());
        assertThat(problem.get("violations").get(0).get("field").asString()).isEqualTo("text");
        assertThat(problem.get("violations").get(0).get("message").asString())
                .isEqualTo("size must be at most 1000 characters");
        assertThat(turnCount(conversationId)).isZero();
        verify(retrievalClient, never()).search(anyString(), anyInt());
    }

    @Test
    void given1000Chars_whenSendMessage_thenStreams() {
        stubHappyPath();
        long conversationId = createConversation();

        HttpResponse<String> response = sendMessage(conversationId, "ğ".repeat(1000));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select char_length(question) from conversation_turns "
                + "where conversation_id = ?", Integer.class, conversationId)).isEqualTo(1000);
    }

    @Test
    void givenUnknownConversationAndSseAccept_whenSendMessage_thenProblemJson404() {
        HttpResponse<String> response = sendMessage(987654321L, QUESTION);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(json.readTree(response.body()).get("errorCode").asString()).isEqualTo("CONVERSATION_NOT_FOUND");
        verify(retrievalClient, never()).search(anyString(), anyInt());
    }

    @Test
    void givenRetrievalDown_whenSendMessage_thenProblemJson503() {
        given(retrievalClient.search(anyString(), anyInt()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));
        long conversationId = createConversation();

        HttpResponse<String> response = sendMessage(conversationId, QUESTION);

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(json.readTree(response.body()).get("errorCode").asString()).isEqualTo("RETRIEVAL_UNAVAILABLE");
        Map<String, Object> turn = jdbc.queryForMap(
                "select error_code, answer from conversation_turns where conversation_id = ?", conversationId);
        assertThat(turn.get("error_code")).isEqualTo("RETRIEVAL_UNAVAILABLE");
        assertThat(turn.get("answer")).isNull();
        verify(chatModel, never()).stream(any(Prompt.class));
    }

    @Test
    void givenModelDown_whenSendMessage_thenStreamHasOnlyErrorEventAndTurnRecorded() {
        stubHappyPath();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.error(new IllegalStateException("refused")));
        long conversationId = createConversation();

        HttpResponse<String> response = sendMessage(conversationId, QUESTION);

        assertThat(response.statusCode()).isEqualTo(200);
        List<SseEvent> events = parseEvents(response.body());
        assertThat(events).extracting(SseEvent::name).containsExactly("error");
        assertThat(events.get(0).data().get("errorCode").asString()).isEqualTo("CHAT_MODEL_UNAVAILABLE");
        assertThat(jdbc.queryForObject("select error_code from conversation_turns where conversation_id = ?",
                String.class, conversationId)).isEqualTo("CHAT_MODEL_UNAVAILABLE");
    }

    @Test
    void givenTwoQuestionsInOneConversation_whenSent_thenTurnNumbersOneAndTwo() {
        stubHappyPath();
        long conversationId = createConversation();

        sendMessage(conversationId, QUESTION);
        stubHappyPath();
        sendMessage(conversationId, "Başka soru");

        assertThat(jdbc.queryForList("select turn_no from conversation_turns where conversation_id = ? "
                + "order by turn_no", Integer.class, conversationId)).containsExactly(1, 2);
    }

    @Test
    void givenAnsweredTurn_whenLogsCaptured_thenIdsAndCountsButNoQuestionAnswerOrChunkText(CapturedOutput output) {
        stubHappyPath();
        long conversationId = createConversation();

        sendMessage(conversationId, QUESTION);

        assertThat(output.getAll()).contains("Turn completed conversationId=" + conversationId)
                .contains("Reranked candidates=20")
                .doesNotContain("Kartımı kaybettim")
                .doesNotContain("Kartını kapat")
                .doesNotContain("C107").doesNotContain("C101")
                .doesNotContain("Cevap parçalarda yoksa");
    }

    @Test
    void givenAnswerInProgress_whenSecondQuestionAndClientDisconnects_then409ThenModelCancelledAndTurnCancelled()
            throws Exception {
        given(retrievalClient.search(anyString(), eq(20))).willReturn(SearchHitFixtures.hits(20));
        Double[] scores = new Double[20];
        Arrays.fill(scores, 1.0);
        given(rerankClient.rerankChunks(anyString(), anyList())).willReturn(List.of(scores));
        Sinks.Many<ChatResponse> sink = Sinks.many().unicast().onBackpressureBuffer();
        AtomicBoolean modelCancelled = new AtomicBoolean();
        given(chatModel.stream(any(Prompt.class))).willReturn(sink.asFlux().doOnCancel(() -> modelCancelled.set(true)));
        long conversationId = createConversation();

        CompletableFuture<HttpResponse<InputStream>> first = http.sendAsync(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assistant/conversations/"
                        + conversationId + "/messages"))
                        .header("Content-Type", "application/json").header("Accept", SSE_ACCEPT)
                        .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"" + QUESTION + "\"}",
                                StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            verify(chatModel).stream(any(Prompt.class));
            return true;
        });
        sink.tryEmitNext(ChatResponses.text("Kartını "));
        HttpResponse<InputStream> firstResponse = first.get(10, TimeUnit.SECONDS);
        BufferedReader reader = new BufferedReader(new InputStreamReader(firstResponse.body(), StandardCharsets.UTF_8));
        assertThat(reader.readLine()).startsWith("event:token");

        HttpResponse<String> second = sendMessage(conversationId, "İkinci soru");
        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(json.readTree(second.body()).get("errorCode").asString()).isEqualTo("TURN_IN_PROGRESS");
        assertThat(turnCount(conversationId)).as("409 must not write a turn").isZero();

        reader.close();
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            sink.tryEmitNext(ChatResponses.text("kapat "));
            Thread.sleep(100);
            return modelCancelled.get();
        });
        await().atMost(Duration.ofSeconds(5)).until(() -> turnCount(conversationId) == 1);
        Map<String, Object> turn = jdbc.queryForMap(
                "select error_code, answer from conversation_turns where conversation_id = ?", conversationId);
        assertThat(turn.get("error_code")).isEqualTo("STREAM_CANCELLED");
        assertThat((String) turn.get("answer")).startsWith("Kartını ");
        assertThat(turnCount(conversationId)).isEqualTo(1);
    }

    @Test
    void givenTwoQuestions_whenSecondSent_thenSourcesHaveStandaloneQueryAndDoneHasRewriteLatency() {
        stubHappyPath();
        long conversationId = createConversation();
        List<SseEvent> first = parseEvents(sendMessage(conversationId, QUESTION).body());
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.just(ChatResponses.text("Kaybolan kartın yenisi kaç günde gelir?")),
                Flux.just(ChatResponses.text("3 iş günü [1].")));

        List<SseEvent> second = parseEvents(sendMessage(conversationId, "Yenisi kaç günde gelir?").body());

        JsonNode firstSources = first.get(first.size() - 2).data();
        JsonNode firstDone = first.get(first.size() - 1).data();
        assertThat(firstSources.get("queryRewriteStatus").asString()).isEqualTo("NO_HISTORY");
        assertThat(firstSources.get("searchQuery").asString()).isEqualTo(QUESTION);
        assertThat(firstDone.get("queryRewriteLatencyMs").isNull()).isTrue();
        JsonNode sources = second.get(second.size() - 2).data();
        JsonNode done = second.get(second.size() - 1).data();
        assertThat(sources.get("queryRewriteStatus").asString()).isEqualTo("REWRITTEN");
        assertThat(sources.get("searchQuery").asString()).isEqualTo("Kaybolan kartın yenisi kaç günde gelir?");
        assertThat(done.get("queryRewriteLatencyMs").asLong()).isGreaterThanOrEqualTo(0L);
        verify(retrievalClient).search(eq("Kaybolan kartın yenisi kaç günde gelir?"), eq(20));
        verify(rerankClient).rerankChunks(eq("Kaybolan kartın yenisi kaç günde gelir?"), anyList());
        var turn = jdbc.queryForMap("select * from conversation_turns where conversation_id = ? and turn_no = 2",
                conversationId);
        assertThat(turn.get("question")).isEqualTo("Yenisi kaç günde gelir?");
        assertThat(turn.get("search_query")).isEqualTo("Kaybolan kartın yenisi kaç günde gelir?");
        assertThat(turn.get("query_rewrite_status")).isEqualTo("REWRITTEN");
        assertThat(turn.get("history_turns")).isEqualTo(1);
    }

    @Test
    void givenRewriteModelFailsOnFollowUp_whenSent_thenStreamContinuesWithOriginalQuestion() {
        stubHappyPath();
        long conversationId = createConversation();
        sendMessage(conversationId, QUESTION);
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.error(new IllegalStateException("refused")),
                Flux.just(ChatResponses.text("3 iş günü [1].")));

        List<SseEvent> events = parseEvents(sendMessage(conversationId, "Yenisi kaç günde gelir?").body());

        assertThat(events).extracting(SseEvent::name).containsExactly("token", "sources", "done");
        JsonNode sources = events.get(1).data();
        assertThat(sources.get("queryRewriteStatus").asString()).isEqualTo("FAILED");
        assertThat(sources.get("searchQuery").asString()).isEqualTo("Yenisi kaç günde gelir?");
        assertThat(events.get(2).data().get("queryRewriteLatencyMs").asLong()).isGreaterThanOrEqualTo(0L);
        verify(retrievalClient).search(eq("Yenisi kaç günde gelir?"), eq(20));
    }

    @Test
    void givenRewrittenTurn_whenLogsCaptured_thenLengthsButNoQueryText(CapturedOutput output) {
        stubHappyPath();
        long conversationId = createConversation();
        sendMessage(conversationId, QUESTION);
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.just(ChatResponses.text("Kaybolan kartın yenisi kaç günde gelir?")),
                Flux.just(ChatResponses.text("3 iş günü [1].")));

        sendMessage(conversationId, "Yenisi kaç günde gelir?");

        assertThat(output.getAll()).contains("Query rewrite conversationId=" + conversationId)
                .contains("status=REWRITTEN").contains("queryLength=39").contains("questionLength=23")
                .doesNotContain("Kaybolan kartın").doesNotContain("Yenisi kaç günde");
    }
}
