package com.rasitbank.assistant.handoff.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.support.FixedClockTestConfiguration;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.TurnFixtures;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Aktarmanın gerçek PostgreSQL'e konuşma başına tek kayıt yazdığını, tekrar isteğin ilk kaydı döndüğünü ve "cevap
 * yok" koşulunun uygulandığını doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(FixedClockTestConfiguration.class)
class HandoffControllerIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private TurnFixtures turns;
    private long conversationId;

    @BeforeEach
    void setUp() {
        turns = new TurnFixtures(jdbc);
        turns.clean();
        conversationId = turns.conversation();
    }

    private MvcTestResult handoff(long conversation, long turnId) {
        return mvc.post().uri("/api/assistant/conversations/" + conversation + "/handoff")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"NO_ANSWER\",\"turnId\":" + turnId + "}").exchange();
    }

    @Test
    void givenNotFoundTurn_whenRequestHandoff_thenAcceptedWithQueueAndReason() {
        long turnId = turns.notFoundTurn(conversationId);

        var result = handoff(conversationId, turnId);

        assertThat(result).hasStatus(202);
        assertThat(result.getResponse().getHeader("Location")).isNull();
        assertThat(new String(result.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("{\"conversationId\":" + conversationId
                + ",\"turnId\":" + turnId
                + ",\"reason\":\"NO_ANSWER\",\"queue\":\"bireysel\",\"createdAt\":\"2026-11-02T10:31:05Z\"}");
        Map<String, Object> row = jdbc.queryForMap("select * from handoffs");
        assertThat(row.get("conversation_id")).isEqualTo(conversationId);
        assertThat(row.get("turn_id")).isEqualTo(turnId);
        assertThat(row.get("reason")).isEqualTo("NO_ANSWER");
        assertThat(row.get("queue")).isEqualTo("bireysel");
        assertThat(turns.count("handoffs")).isEqualTo(1);
    }

    @Test
    void givenHandoff_whenRequested_thenNoTurnIsAdded() {
        long turnId = turns.notFoundTurn(conversationId);
        int before = turns.count("conversation_turns");

        handoff(conversationId, turnId);

        assertThat(turns.count("conversation_turns")).isEqualTo(before);
    }

    @Test
    void givenHandedOffConversation_whenRequestAgain_thenSameRecordAndOneRow() {
        long turnId = turns.notFoundTurn(conversationId);
        String first = new String(handoff(conversationId, turnId).getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);

        var second = handoff(conversationId, turnId);

        assertThat(second).hasStatus(202);
        assertThat(new String(second.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(first);
        assertThat(turns.count("handoffs")).isEqualTo(1);
    }

    @Test
    void givenHandedOffConversation_whenRequestWithAnotherNotFoundTurn_thenFirstRecordReturned() {
        long firstTurn = turns.notFoundTurn(conversationId);
        long secondTurn = turns.notFoundTurn(conversationId);
        handoff(conversationId, firstTurn);

        var result = handoff(conversationId, secondTurn);

        assertThat(result).hasStatus(202);
        assertThat(result).bodyJson().extractingPath("$.turnId").isEqualTo((int) firstTurn);
        assertThat(turns.count("handoffs")).isEqualTo(1);
    }

    @Test
    void givenTwoConversations_whenEachHandedOff_thenTwoRows() {
        long otherConversation = turns.conversation();
        long turnA = turns.notFoundTurn(conversationId);
        long turnB = turns.notFoundTurn(otherConversation);

        handoff(conversationId, turnA);
        handoff(otherConversation, turnB);

        assertThat(turns.count("handoffs")).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ANSWERED", "CHAT_MODEL_UNAVAILABLE", "STREAM_CANCELLED", "LLM_ONLY"})
    void givenTurnWithoutNotFoundAnswer_whenRequestHandoff_thenHandoffNotAllowed(String kind) {
        long turnId = switch (kind) {
            case "ANSWERED" -> turns.answeredTurn(conversationId);
            case "LLM_ONLY" -> turns.llmOnlyTurn(conversationId);
            default -> turns.failedTurn(conversationId, kind);
        };

        var result = handoff(conversationId, turnId);

        assertThat(result).hasStatus(409);
        assertThat(result).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("HANDOFF_NOT_ALLOWED");
        assertThat(turns.count("handoffs")).isZero();
    }

    @Test
    void givenUnknownConversation_whenRequestHandoff_thenConversationNotFound() {
        var result = handoff(9999, 1);

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("CONVERSATION_NOT_FOUND");
        assertThat(turns.count("handoffs")).isZero();
    }

    @Test
    void givenTurnOfAnotherConversation_whenRequestHandoff_thenTurnNotFound() {
        long otherConversation = turns.conversation();
        long foreignTurn = turns.notFoundTurn(otherConversation);

        var result = handoff(conversationId, foreignTurn);

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("TURN_NOT_FOUND");
        assertThat(turns.count("handoffs")).isZero();
    }

    @Test
    void givenUnknownTurn_whenRequestHandoff_thenTurnNotFound() {
        var result = handoff(conversationId, 9999);

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("TURN_NOT_FOUND");
        assertThat(turns.count("handoffs")).isZero();
    }

    @Test
    void givenHandedOffConversation_whenRetryWithWrongTurn_thenStillValidated() {
        long turnId = turns.notFoundTurn(conversationId);
        handoff(conversationId, turnId);

        var result = handoff(conversationId, 9999);

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("TURN_NOT_FOUND");
    }
}
