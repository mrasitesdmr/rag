package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rasitbank.assistant.retrieval.client.RerankClient;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.support.ChatResponses;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.SearchHitFixtures;
import com.rasitbank.assistant.support.TestProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cevabın dayanmadığı parçaya alıntı vermemesini gerçek sunucu, gerçek PostgreSQL ve sahte model/retrieval/reranker
 * ile uçtan uca sınar: eşik altı turlar, etiket yolu, kalıp listesi, "bulamadım" cümlesinin akışta tek parça gitmesi,
 * giriş cümlesine bağlanan numaranın silinmesi ve selamlaşma turlarının geçmişe girmemesi.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SendMessageGroundedAnswerIntegrationTest extends PostgresIntegrationTest {

    private static final String LONG_GREETING = "merhaba ben bugün çok keyifli hissediyorum sizin nasıl geçiyor günleriniz acaba";
    private static final String LABEL_PROMPT_START = "Bir bankanın dijital asistanına gelen müşteri mesajını";
    private static final String QUESTION = "Kartımı kaybettim, ne yapmalıyım?";
    private static final String NOT_FOUND = TestProperties.NOT_FOUND_ANSWER;
    private static final double BELOW_THRESHOLD_SCORE = -5.0;

    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final AtomicReference<Flux<ChatResponse>> labelOutput = new AtomicReference<>();
    private final AtomicReference<List<Flux<ChatResponse>>> answerOutputs = new AtomicReference<>(List.of());
    private final AtomicReference<Flux<ChatResponse>> rewriteOutput = new AtomicReference<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private int answerCalls;

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

    @BeforeEach
    void routeFakeModelByPrompt() {
        jdbc.execute("truncate conversations, conversation_turns, turn_chunks, handoffs restart identity cascade");
        jdbc.execute("truncate golden_sets restart identity cascade");
        prompts.clear();
        answerCalls = 0;
        labelOutput.set(Flux.just(ChatResponses.text("DIGER")));
        rewriteOutput.set(Flux.just(ChatResponses.text("Ek kart nasıl alınır?")));
        given(chatModel.stream(any(Prompt.class))).willAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            synchronized (prompts) {
                prompts.add(prompt);
            }
            Message first = prompt.getInstructions().getFirst();
            if (first instanceof SystemMessage && first.getText().startsWith(LABEL_PROMPT_START)) {
                return labelOutput.get();
            }
            String last = prompt.getInstructions().getLast().getText();
            if (last.contains("<parcalar>")) {
                List<Flux<ChatResponse>> outputs = answerOutputs.get();
                return outputs.get(Math.min(answerCalls++, outputs.size() - 1));
            }
            return rewriteOutput.get();
        });
    }

    private void answerWith(ChatResponse... tokens) {
        answerOutputs.set(List.of(Flux.just(tokens)));
    }

    private void stubSearchWithTopScore(double topScore) {
        given(retrievalClient.search(anyString(), eq(20))).willReturn(SearchHitFixtures.hits(20));
        Double[] scores = new Double[20];
        for (int i = 0; i < scores.length; i++) {
            scores[i] = topScore - i / 10.0;
        }
        given(rerankClient.rerankChunks(anyString(), anyList())).willReturn(List.of(scores));
    }

    private void stubGroundedSearch() {
        stubSearchWithTopScore(9.0);
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
        return json.readTree(response.body()).get("conversationId").asLong();
    }

    private List<SseEvent> send(long conversationId, String text) {
        HttpResponse<String> response = post("/api/assistant/conversations/" + conversationId + "/messages",
                "{\"text\":" + json.writeValueAsString(text) + "}", "text/event-stream, application/problem+json");
        assertThat(response.statusCode()).isEqualTo(200);
        return parseEvents(response.body());
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

    private Map<String, Object> turn(long conversationId, int turnNo) {
        return jdbc.queryForMap("select * from conversation_turns where conversation_id = ? and turn_no = ?",
                conversationId, turnNo);
    }

    private String citedText(long conversationId, int turnNo) {
        return jdbc.queryForObject("select cited::text from conversation_turns where conversation_id = ? "
                + "and turn_no = ?", String.class, conversationId, turnNo);
    }

    private static List<String> names(List<SseEvent> events) {
        return events.stream().map(SseEvent::name).toList();
    }

    private long labelCalls() {
        synchronized (prompts) {
            return prompts.stream()
                    .filter(prompt -> prompt.getInstructions().getFirst().getText().startsWith(LABEL_PROMPT_START))
                    .count();
        }
    }

    private int modelCalls() {
        synchronized (prompts) {
            return prompts.size();
        }
    }

    @Test
    void givenTopRerankScoreBelowThresholdAndLabelOther_whenSendMessage_thenNotFoundWithoutAnswerModelCall() {
        // Arrange
        stubSearchWithTopScore(BELOW_THRESHOLD_SCORE);
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "Bana bir şiir yaz");

        // Assert
        assertThat(names(events)).containsExactly("token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).isEqualTo(NOT_FOUND);
        JsonNode sources = events.get(1).data();
        assertThat(sources.get("retrievalStrategy").asString()).isEqualTo("RERANK");
        assertThat(sources.get("contextChunks")).isEmpty();
        assertThat(sources.get("citations")).isEmpty();
        JsonNode done = events.get(2).data();
        assertThat(done.get("notFoundAnswer").asBoolean()).isTrue();
        assertThat(done.get("answer").asString()).isEqualTo(NOT_FOUND);
        assertThat(done.get("answerSource").asString()).isEqualTo("BELOW_RERANK_THRESHOLD");
        assertThat(done.get("generationLatencyMs").isNull()).isTrue();
        assertThat(modelCalls()).as("only the label call reached the model").isEqualTo(1);
        assertThat(prompts.getFirst().getInstructions().getFirst().getText()).startsWith(LABEL_PROMPT_START);
        long turnId = done.get("turnId").asLong();
        assertThat(jdbc.queryForObject("select count(*) from turn_chunks where turn_id = ?", Integer.class, turnId))
                .isEqualTo(20);
        assertThat(jdbc.queryForObject("select count(prompt_position) from turn_chunks where turn_id = ?",
                Integer.class, turnId)).isZero();
        Map<String, Object> turn = turn(conversationId, 1);
        assertThat(turn.get("answer_source")).isEqualTo("BELOW_RERANK_THRESHOLD");
        assertThat(turn.get("message_label")).isEqualTo("OTHER");
        assertThat(turn.get("message_label_latency_ms")).isNotNull();
        assertThat(turn.get("not_found_answer")).isEqualTo(true);
        assertThat(citedText(conversationId, 1)).isEqualTo("{}");
        assertThat(turn.get("retrieval_strategy")).isEqualTo("RERANK");
    }

    @Test
    void givenLongMessageBelowThresholdAndLabelGreeting_whenSendMessage_thenGreetingReplyWithRetrievalRecorded() {
        // Arrange
        stubSearchWithTopScore(BELOW_THRESHOLD_SCORE);
        labelOutput.set(Flux.just(ChatResponses.text("SELAM")));
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, LONG_GREETING);

        // Assert
        assertThat(names(events)).containsExactly("token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString())
                .startsWith("Merhaba, ben Raşit Bankası'nın dijital asistanıyım.");
        JsonNode sources = events.get(1).data();
        assertThat(sources.get("retrievalStrategy").asString()).isEqualTo("RERANK");
        assertThat(sources.get("searchQuery").asString()).isEqualTo(LONG_GREETING);
        assertThat(sources.get("contextChunks")).isEmpty();
        JsonNode done = events.get(2).data();
        assertThat(done.get("answerSource").asString()).isEqualTo("SMALL_TALK");
        assertThat(done.get("notFoundAnswer").asBoolean()).isFalse();
        Map<String, Object> turn = turn(conversationId, 1);
        assertThat(turn.get("message_label")).isEqualTo("GREETING");
        assertThat(turn.get("retrieval_strategy")).isEqualTo("RERANK");
        assertThat(turn.get("answer_source")).isEqualTo("SMALL_TALK");
        long turnId = done.get("turnId").asLong();
        assertThat(jdbc.queryForObject("select count(*) from turn_chunks where turn_id = ?", Integer.class, turnId))
                .isEqualTo(20);
        assertThat(jdbc.queryForObject("select count(prompt_position) from turn_chunks where turn_id = ?",
                Integer.class, turnId)).isZero();
    }

    @Test
    void givenBelowThresholdAndLabelModelFails_whenSendMessage_thenNotFoundAndNoError() {
        // Arrange
        stubSearchWithTopScore(BELOW_THRESHOLD_SCORE);
        labelOutput.set(Flux.error(new IllegalStateException("model down")));
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "asdfgh");

        // Assert
        assertThat(names(events)).containsExactly("token", "sources", "done");
        assertThat(events.get(2).data().get("answerSource").asString()).isEqualTo("BELOW_RERANK_THRESHOLD");
        assertThat(turn(conversationId, 1).get("message_label")).isEqualTo("OTHER");
    }

    @Test
    void givenModelWritesNotFoundWithCitation_whenSendMessage_thenSingleSentenceTokenAndNoCitation() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text("Bu konuda "), ChatResponses.text("rehberimizde bilgi bulamadım."),
                ChatResponses.textWithUsage(" [5]", 120, 9));
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "Bitcoin alıp satabilir miyim?");

        // Assert
        assertThat(names(events)).containsExactly("token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).isEqualTo(NOT_FOUND);
        assertThat(events.get(1).data().get("citations")).isEmpty();
        JsonNode done = events.get(2).data();
        assertThat(done.get("notFoundAnswer").asBoolean()).isTrue();
        assertThat(done.get("answer").asString()).isEqualTo(NOT_FOUND);
        assertThat(done.get("answerSource").asString()).isEqualTo("MODEL");
        assertThat(done.get("completionTokens").asInt()).isEqualTo(9);
        Map<String, Object> turn = turn(conversationId, 1);
        assertThat(turn.get("answer")).isEqualTo(NOT_FOUND);
        assertThat(citedText(conversationId, 1)).isEqualTo("{}");
        assertThat(turn.get("not_found_answer")).isEqualTo(true);
        assertThat(turn.get("completion_tokens")).isEqualTo(9);
        assertThat(labelCalls()).as("short message labeled once before search").isEqualTo(1);
        assertThat(modelCalls()).as("label call plus answer call").isEqualTo(2);
    }

    @Test
    void givenGroundedModelAnswer_whenSendMessage_thenTokensCitationAndAnswerUnchanged() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text("Kartını hemen "), ChatResponses.text("kapat [1]."));
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, QUESTION);

        // Assert
        assertThat(names(events)).containsExactly("token", "token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).isEqualTo("Kartını hemen ");
        assertThat(events.get(1).data().get("text").asString()).isEqualTo("kapat [1].");
        assertThat(events.get(2).data().get("citations").toString()).isEqualTo("[1]");
        JsonNode done = events.get(3).data();
        assertThat(done.get("answer").asString()).isEqualTo("Kartını hemen kapat [1].");
        assertThat(done.get("answerSource").asString()).isEqualTo("MODEL");
        assertThat(done.get("notFoundAnswer").asBoolean()).isFalse();
        assertThat(citedText(conversationId, 1)).isEqualTo("{1}");
    }

    @Test
    void givenModelWritesParaphrasedNotFound_whenSendMessage_thenDoneAnswerIsFixedSentenceAndNoCitations() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text("Diğer bankaların EFT ücretlerini "),
                ChatResponses.text("rehberimizde bulamadım. [3]"));
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "Diğer bankalar EFT için ne kadar ücret alıyor?");

        // Assert
        assertThat(names(events)).containsExactly("token", "token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).isEqualTo("Diğer bankaların EFT ücretlerini ");
        assertThat(events.get(1).data().get("text").asString()).isEqualTo("rehberimizde bulamadım. [3]");
        assertThat(events.get(2).data().get("citations")).isEmpty();
        JsonNode done = events.get(3).data();
        assertThat(done.get("notFoundAnswer").asBoolean()).isTrue();
        assertThat(done.get("answer").asString()).isEqualTo(NOT_FOUND);
        Map<String, Object> turn = turn(conversationId, 1);
        assertThat(turn.get("answer")).isEqualTo(NOT_FOUND);
        assertThat(citedText(conversationId, 1)).isEqualTo("{}");
        assertThat(turn.get("not_found_answer")).isEqualTo(true);
    }

    @Test
    void givenModelCitesOpener_whenSendMessage_thenDoneAnswerAndCitationsExcludeOpenerCitation() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text(TestProperties.PERSONAL_INFO_OPENER + " [1] "),
                ChatResponses.text("Kumbara hesabını Raşit Mobil'den açabilirsin [2]."));
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "Kumbara hesabımda kaç lira birikti?");

        // Assert
        assertThat(names(events)).containsExactly("token", "token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).isEqualTo(TestProperties.PERSONAL_INFO_OPENER + " [1] ");
        assertThat(events.get(2).data().get("citations").toString()).isEqualTo("[2]");
        String expected = TestProperties.PERSONAL_INFO_OPENER + " Kumbara hesabını Raşit Mobil'den açabilirsin [2].";
        JsonNode done = events.get(3).data();
        assertThat(done.get("answer").asString()).isEqualTo(expected);
        assertThat(done.get("notFoundAnswer").asBoolean()).isFalse();
        Map<String, Object> turn = turn(conversationId, 1);
        assertThat(turn.get("answer")).isEqualTo(expected);
        assertThat(citedText(conversationId, 1)).isEqualTo("{2}");
    }

    @Test
    void givenAnsweredTurn_whenThanksSent_thenSmallTalkTurnWithoutRewriteOrCitation() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text("Kartını hemen kapat [1]."));
        long conversationId = createConversation();
        send(conversationId, QUESTION);
        int callsAfterFirstTurn = modelCalls();
        labelOutput.set(Flux.just(ChatResponses.text("TESEKKUR")));

        // Act
        List<SseEvent> events = send(conversationId, "Teşekkürler");

        // Assert
        assertThat(names(events)).containsExactly("token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).startsWith("Rica ederim.");
        JsonNode sources = events.get(1).data();
        assertThat(sources.get("retrievalStrategy").asString()).isEqualTo("NONE");
        assertThat(sources.get("searchQuery").isNull()).isTrue();
        assertThat(sources.get("queryRewriteStatus").asString()).isEqualTo("DISABLED");
        assertThat(sources.get("contextChunks")).isEmpty();
        assertThat(sources.get("citations")).isEmpty();
        assertThat(events.get(2).data().get("answerSource").asString()).isEqualTo("SMALL_TALK");
        Map<String, Object> turn = turn(conversationId, 2);
        assertThat(turn.get("answer_source")).isEqualTo("SMALL_TALK");
        assertThat(turn.get("retrieval_strategy")).isEqualTo("NONE");
        assertThat(turn.get("search_query")).isNull();
        assertThat(citedText(conversationId, 2)).isEqualTo("{}");
        assertThat(turn.get("message_label")).isEqualTo("THANKS");
        assertThat(turn.get("message_label_latency_ms")).isNotNull();
        assertThat(modelCalls()).as("the second turn only reached the label call").isEqualTo(callsAfterFirstTurn + 1);
        verify(retrievalClient, times(1)).search(anyString(), eq(20));
    }

    @Test
    void givenSmallTalkBetweenTurns_whenNextQuestion_thenHistoryExcludesSmallTalk() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text("Kartını hemen kapat [1]."));
        long conversationId = createConversation();
        send(conversationId, QUESTION);
        labelOutput.set(Flux.just(ChatResponses.text("TESEKKUR")));
        send(conversationId, "Teşekkürler");
        labelOutput.set(Flux.just(ChatResponses.text("DIGER")));
        answerWith(ChatResponses.text("Ek kartı şubeden alabilirsin [2]."));
        int callsBefore = modelCalls();

        // Act
        send(conversationId, "Peki ek kart?");

        // Assert
        Prompt rewritePrompt;
        synchronized (prompts) {
            rewritePrompt = prompts.subList(callsBefore, prompts.size()).stream()
                    .filter(prompt -> prompt.getInstructions().getLast().getText().contains("Arama sorusu:"))
                    .findFirst().orElseThrow();
        }
        String rewriteText = rewritePrompt.getInstructions().getLast().getText();
        assertThat(rewriteText).contains("- " + QUESTION).doesNotContain("Kartını hemen kapat")
                .doesNotContain("Teşekkürler").doesNotContain("Rica ederim");
        assertThat(turn(conversationId, 3).get("history_turns")).isEqualTo(1);
    }

    @Test
    void givenPreSearchLabelTurn_whenLogsCaptured_thenRouteLoggedWithoutMessageText(CapturedOutput output) {
        // Arrange
        stubSearchWithTopScore(BELOW_THRESHOLD_SCORE);
        long conversationId = createConversation();

        // Act
        labelOutput.set(Flux.just(ChatResponses.text("SELAM")));
        send(conversationId, "Selam");
        labelOutput.set(Flux.just(ChatResponses.text("DIGER")));
        send(conversationId, "Bana bir şiir yaz");

        // Assert
        assertThat(output.getAll()).contains("Message labeled before search conversationId=" + conversationId
                        + " label=GREETING")
                .contains("Small talk answered conversationId=" + conversationId
                        + " label=GREETING route=PRE_SEARCH_LABEL")
                .contains("Answer stopped by rerank threshold conversationId=" + conversationId + " topRerankScore=-5.0")
                .contains("Message labeled before search conversationId=" + conversationId + " label=OTHER")
                .doesNotContain("route=PATTERN")
                .doesNotContain("Bana bir şiir yaz").doesNotContain("Selam").doesNotContain("dijital asistanıyım")
                .doesNotContain("DIGER");
    }

    @Test
    void givenEmptyKnowledgeBase_whenSendMessage_thenAnswerSourceEmptyKnowledgeBaseAndNoAnswerModelCall() {
        // Arrange
        given(retrievalClient.search(anyString(), eq(20))).willReturn(List.of());
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "Bitcoin alıp satabilir miyim?");

        // Assert
        JsonNode done = events.get(events.size() - 1).data();
        assertThat(done.get("answerSource").asString()).isEqualTo("EMPTY_KNOWLEDGE_BASE");
        assertThat(done.get("notFoundAnswer").asBoolean()).isTrue();
        assertThat(done.get("answer").asString()).isEqualTo(NOT_FOUND);
        assertThat(modelCalls()).as("only the label call").isEqualTo(1);
        assertThat(labelCalls()).isEqualTo(1);
        assertThat(turn(conversationId, 1).get("answer_source")).isEqualTo("EMPTY_KNOWLEDGE_BASE");
    }

    @Test
    void givenAnsweredTurnThenUnlistedThanks_whenSendMessage_thenSmallTalkWithoutSearchOrCitation() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text("Kartını hemen kapat [1]."));
        long conversationId = createConversation();
        send(conversationId, QUESTION);
        labelOutput.set(Flux.just(ChatResponses.text("TESEKKUR")));
        int callsAfterFirstTurn = modelCalls();

        // Act
        List<SseEvent> events = send(conversationId, "Allah razı olsun");

        // Assert
        assertThat(names(events)).containsExactly("token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).startsWith("Rica ederim.");
        JsonNode sources = events.get(1).data();
        assertThat(sources.get("retrievalStrategy").asString()).isEqualTo("NONE");
        assertThat(sources.get("contextChunks")).isEmpty();
        assertThat(sources.get("citations")).isEmpty();
        assertThat(events.get(2).data().get("answerSource").asString()).isEqualTo("SMALL_TALK");
        Map<String, Object> turn = turn(conversationId, 2);
        assertThat(turn.get("retrieval_strategy")).isEqualTo("NONE");
        assertThat(turn.get("search_query")).isNull();
        assertThat(turn.get("message_label")).isEqualTo("THANKS");
        assertThat(turn.get("message_label_latency_ms")).isNotNull();
        assertThat(citedText(conversationId, 2)).isEqualTo("{}");
        assertThat(modelCalls()).as("only the label call").isEqualTo(callsAfterFirstTurn + 1);
        synchronized (prompts) {
            assertThat(prompts.get(callsAfterFirstTurn).getInstructions().getFirst().getText())
                    .startsWith(LABEL_PROMPT_START);
        }
        verify(retrievalClient, times(1)).search(anyString(), eq(20));
    }

    @Test
    void givenAnsweredTurnThenShortOtherMessage_whenSendMessage_thenLabelAskedOnceAndAnswerStaysGrounded() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text("Kartını hemen kapat [1]."));
        long conversationId = createConversation();
        send(conversationId, QUESTION);
        long labelCallsBefore = labelCalls();

        // Act
        List<SseEvent> events = send(conversationId, "Ya yurt dışında");

        // Assert
        assertThat(events.get(events.size() - 1).data().get("answerSource").asString()).isEqualTo("MODEL");
        Map<String, Object> turn = turn(conversationId, 2);
        assertThat(turn.get("message_label")).isEqualTo("OTHER");
        assertThat(turn.get("retrieval_strategy")).isEqualTo("RERANK");
        assertThat(labelCalls() - labelCallsBefore).isEqualTo(1);
    }

    @Test
    void givenFirstMessageGreeting_whenSendMessage_thenLabelThenFixedReplyWithoutSearch() {
        // Arrange
        stubGroundedSearch();
        labelOutput.set(Flux.just(ChatResponses.text("SELAM")));
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "Selam");

        // Assert
        assertThat(names(events)).containsExactly("token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString())
                .startsWith("Merhaba, ben Raşit Bankası'nın dijital asistanıyım.");
        JsonNode sources = events.get(1).data();
        assertThat(sources.get("retrievalStrategy").asString()).isEqualTo("NONE");
        assertThat(sources.get("searchQuery").isNull()).isTrue();
        assertThat(sources.get("queryRewriteStatus").asString()).isEqualTo("DISABLED");
        assertThat(sources.get("contextChunks")).isEmpty();
        assertThat(sources.get("citations")).isEmpty();
        JsonNode done = events.get(2).data();
        assertThat(done.get("answerSource").asString()).isEqualTo("SMALL_TALK");
        assertThat(done.get("notFoundAnswer").asBoolean()).isFalse();
        Map<String, Object> turn = turn(conversationId, 1);
        assertThat(turn.get("message_label")).isEqualTo("GREETING");
        assertThat(turn.get("message_label_latency_ms")).isNotNull();
        assertThat(turn.get("retrieval_strategy")).isEqualTo("NONE");
        assertThat(modelCalls()).as("only the label call").isEqualTo(1);
        verify(retrievalClient, never()).search(anyString(), anyInt());
    }

    @Test
    void givenShortPersonalQuestionLabeledOther_whenSendMessage_thenOpenerAndCitedInfo() {
        // Arrange
        stubGroundedSearch();
        answerWith(ChatResponses.text(TestProperties.PERSONAL_INFO_OPENER + " "),
                ChatResponses.text("Limitini Raşit Mobil'de Kartlarım > Limit işlemleri'nden artırabilirsin [1]."));
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "Kredi limitim ne kadar");

        // Assert
        JsonNode sources = events.get(events.size() - 2).data();
        assertThat(sources.get("citations").toString()).isEqualTo("[1]");
        JsonNode done = events.get(events.size() - 1).data();
        assertThat(done.get("answer").asString()).isEqualTo(TestProperties.PERSONAL_INFO_OPENER
                + " Limitini Raşit Mobil'de Kartlarım > Limit işlemleri'nden artırabilirsin [1].");
        assertThat(done.get("answerSource").asString()).isEqualTo("MODEL");
        Map<String, Object> turn = turn(conversationId, 1);
        assertThat(turn.get("message_label")).isEqualTo("OTHER");
        assertThat(citedText(conversationId, 1)).isEqualTo("{1}");
    }

    @Test
    void givenMessageWithoutWords_whenSendMessage_thenSingleNotFoundTokenAndNoWordsTurn() {
        // Arrange
        stubGroundedSearch();
        long conversationId = createConversation();

        // Act
        List<SseEvent> events = send(conversationId, "???");

        // Assert
        assertThat(names(events)).containsExactly("token", "sources", "done");
        assertThat(events.get(0).data().get("text").asString()).isEqualTo(NOT_FOUND);
        JsonNode sources = events.get(1).data();
        assertThat(sources.get("retrievalStrategy").asString()).isEqualTo("NONE");
        assertThat(sources.get("searchQuery").isNull()).isTrue();
        assertThat(sources.get("queryRewriteStatus").asString()).isEqualTo("DISABLED");
        assertThat(sources.get("contextChunks")).isEmpty();
        assertThat(sources.get("citations")).isEmpty();
        JsonNode done = events.get(2).data();
        assertThat(done.get("notFoundAnswer").asBoolean()).isTrue();
        assertThat(done.get("answerSource").asString()).isEqualTo("NO_WORDS");
        assertThat(done.get("answer").asString()).isEqualTo(NOT_FOUND);
        assertThat(done.get("generationLatencyMs").isNull()).isTrue();
        assertThat(done.get("promptTokens").isNull()).isTrue();
        Map<String, Object> turn = turn(conversationId, 1);
        assertThat(turn.get("answer_source")).isEqualTo("NO_WORDS");
        assertThat(turn.get("retrieval_strategy")).isEqualTo("NONE");
        assertThat(turn.get("not_found_answer")).isEqualTo(true);
        assertThat(turn.get("message_label")).isNull();
        assertThat(citedText(conversationId, 1)).isEqualTo("{}");
        assertThat(modelCalls()).isZero();
        verify(retrievalClient, never()).search(anyString(), anyInt());
    }
}
