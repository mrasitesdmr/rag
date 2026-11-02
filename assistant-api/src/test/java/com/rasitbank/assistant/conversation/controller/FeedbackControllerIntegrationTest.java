package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.support.FixedClockTestConfiguration;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.TurnFixtures;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Geri bildirimin gerçek PostgreSQL'e yazıldığını, son seçimin geçerli olduğunu ve tamamlanmamış ya da olmayan
 * tura reddedildiğini doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(FixedClockTestConfiguration.class)
class FeedbackControllerIntegrationTest extends PostgresIntegrationTest {

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

    private org.springframework.test.web.servlet.assertj.MvcTestResult post(long turnId, String body) {
        return mvc.post().uri("/api/assistant/turns/" + turnId + "/feedback").contentType(MediaType.APPLICATION_JSON)
                .content(body).exchange();
    }

    private Map<String, Object> feedbackColumns(long turnId) {
        return jdbc.queryForMap("select helpful, feedback_at from conversation_turns where id = ?", turnId);
    }

    @Test
    void givenAnsweredTurn_whenSubmitFeedback_thenStoresHelpfulAndTime() {
        long turnId = turns.answeredTurn(conversationId);

        var result = post(turnId, "{\"helpful\":true}");

        assertThat(result).hasStatus(204);
        assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
        Map<String, Object> row = feedbackColumns(turnId);
        assertThat(row.get("helpful")).isEqualTo(true);
        assertThat(((Timestamp) row.get("feedback_at")).toInstant()).isEqualTo(FixedClockTestConfiguration.NOW);
    }

    @Test
    void givenNotFoundAnswerTurn_whenSubmitFeedback_thenStored() {
        long turnId = turns.notFoundTurn(conversationId);

        assertThat(post(turnId, "{\"helpful\":false}")).hasStatus(204);

        assertThat(feedbackColumns(turnId).get("helpful")).isEqualTo(false);
    }

    @Test
    void givenHelpfulTrue_whenSubmitFalse_thenFalseStored() {
        long turnId = turns.answeredTurn(conversationId);
        assertThat(post(turnId, "{\"helpful\":true}")).hasStatus(204);

        assertThat(post(turnId, "{\"helpful\":false}")).hasStatus(204);

        assertThat(feedbackColumns(turnId).get("helpful")).isEqualTo(false);
    }

    @Test
    void givenHelpfulTrue_whenSubmitTrueAgain_thenStillTrue() {
        long turnId = turns.answeredTurn(conversationId);
        assertThat(post(turnId, "{\"helpful\":true}")).hasStatus(204);

        assertThat(post(turnId, "{\"helpful\":true}")).hasStatus(204);

        assertThat(feedbackColumns(turnId).get("helpful")).isEqualTo(true);
    }

    @Test
    void givenFeedbackOnOneTurn_whenSubmit_thenOtherTurnUntouched() {
        long first = turns.answeredTurn(conversationId);
        long second = turns.answeredTurn(conversationId);

        assertThat(post(first, "{\"helpful\":true}")).hasStatus(204);

        assertThat(feedbackColumns(second).get("helpful")).isNull();
        assertThat(feedbackColumns(second).get("feedback_at")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHAT_MODEL_UNAVAILABLE", "STREAM_CANCELLED", "RETRIEVAL_UNAVAILABLE"})
    void givenFailedTurn_whenSubmitFeedback_thenTurnNotCompleted(String errorCode) {
        long turnId = turns.failedTurn(conversationId, errorCode);

        var result = post(turnId, "{\"helpful\":true}");

        assertThat(result).hasStatus(409);
        assertThat(result).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("TURN_NOT_COMPLETED");
        assertThat(feedbackColumns(turnId).get("helpful")).isNull();
        assertThat(feedbackColumns(turnId).get("feedback_at")).isNull();
    }

    @Test
    void givenUnknownTurn_whenSubmitFeedback_thenTurnNotFound() {
        var result = post(9999, "{\"helpful\":true}");

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("TURN_NOT_FOUND");
    }

    @Test
    void givenFailedTurn_whenHelpfulWrittenDirectly_thenCheckViolation() {
        long turnId = turns.failedTurn(conversationId, "STREAM_CANCELLED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                "update conversation_turns set helpful = true, feedback_at = now() where id = ?", turnId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_conversation_turns_feedback_completed");
    }

    @Test
    void givenHelpfulWithoutTime_whenWrittenDirectly_thenPairCheckViolation() {
        long turnId = turns.answeredTurn(conversationId);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> jdbc.update("update conversation_turns set helpful = true where id = ?", turnId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_conversation_turns_feedback_pair");
    }
}
