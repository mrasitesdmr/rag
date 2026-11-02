package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.conversation.service.ActiveTurnRegistry;
import com.rasitbank.assistant.support.FixedClockTestConfiguration;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.TurnFixtures;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Konuşma liste ve detay uçlarını gerçek PostgreSQL ile sınar: sıra, kapsam, sayfalama, süren tur, çift görünmeme,
 * tur durumlarının tek tek hesaplanması, parça tablosu ve aktarma kaydı.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(FixedClockTestConfiguration.class)
class ConversationReadControllerIntegrationTest extends PostgresIntegrationTest {

    private static final String LIST = "/api/assistant/conversations";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ActiveTurnRegistry registry;

    private TurnFixtures turns;

    @BeforeEach
    void setUp() {
        turns = new TurnFixtures(jdbc);
        turns.clean();
    }

    @AfterEach
    void clearRegistry() {
        registry.activeTurns().keySet().forEach(registry::finish);
    }

    private org.springframework.test.web.servlet.assertj.MvcTestResult detail(long conversationId) {
        return mvc.get().uri(LIST + "/" + conversationId).exchange();
    }

    private void feedback(long turnId, boolean helpful) {
        var result = mvc.post().uri("/api/assistant/turns/" + turnId + "/feedback")
                .contentType(MediaType.APPLICATION_JSON).content("{\"helpful\":" + helpful + "}").exchange();
        assertThat(result).hasStatus(204);
    }

    @Test
    void givenConversationsWithAndWithoutTurns_whenListConversations_thenNewestFirstWithoutEmptyOnes() {
        // Arrange
        long older = turns.conversationCreatedAt("2026-11-02T10:00:00Z");
        long first = turns.answeredTurn(older);
        turns.describeTurn(first, "Kartımı kaybettim", "Kartımı kaybettim", new int[] {1});
        long second = turns.notFoundTurn(older);
        turns.describeTurn(second, "Kripto hesabı açılır mı?", "Kripto hesabı açılır mı?", new int[] {});
        turns.conversationCreatedAt("2026-11-02T10:05:00Z");
        long newer = turns.conversationCreatedAt("2026-11-02T10:10:00Z");
        long newerTurn = turns.notFoundTurn(newer);
        mvc.post().uri(LIST + "/" + newer + "/handoff").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"NO_ANSWER\",\"turnId\":" + newerTurn + "}").exchange();

        // Act
        var result = mvc.get().uri(LIST).exchange();

        // Assert
        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.content[0].conversationId").isEqualTo((int) newer);
        assertThat(result).bodyJson().extractingPath("$.content[0].handedOff").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.content[0].turnCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.content[1].conversationId").isEqualTo((int) older);
        assertThat(result).bodyJson().extractingPath("$.content[1].turnCount").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.content[1].firstQuestion").isEqualTo("Kartımı kaybettim");
        assertThat(result).bodyJson().extractingPath("$.content[1].lastTurnStatus").isEqualTo("DONT_KNOW");
        assertThat(result).bodyJson().extractingPath("$.content[1].handedOff").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.content[1].channel").isEqualTo("PREVIEW");
    }

    @Test
    void givenNoConversations_whenListConversations_thenEmptyPage() {
        var result = mvc.get().uri(LIST).exchange();

        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.content").asList().isEmpty();
    }

    @Test
    void givenThreeListedConversations_whenPageSizeTwo_thenTwoPages() {
        // Arrange
        for (String createdAt : new String[] {"2026-11-02T09:00:00Z", "2026-11-02T09:01:00Z", "2026-11-02T09:02:00Z"}) {
            turns.answeredTurn(turns.conversationCreatedAt(createdAt));
        }

        // Act
        var first = mvc.get().uri(LIST + "?page=0&size=2").exchange();
        var second = mvc.get().uri(LIST + "?page=1&size=2").exchange();

        // Assert
        assertThat(first).bodyJson().extractingPath("$.content").asList().hasSize(2);
        assertThat(first).bodyJson().extractingPath("$.totalElements").isEqualTo(3);
        assertThat(first).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
        assertThat(second).bodyJson().extractingPath("$.content").asList().hasSize(1);
        assertThat(second).bodyJson().extractingPath("$.page").isEqualTo(1);
    }

    @Test
    void givenSameCreatedAt_whenListConversations_thenHigherIdFirst() {
        long low = turns.conversationCreatedAt("2026-11-02T09:00:00Z");
        long high = turns.conversationCreatedAt("2026-11-02T09:00:00Z");
        turns.answeredTurn(low);
        turns.answeredTurn(high);

        var result = mvc.get().uri(LIST).exchange();

        assertThat(result).bodyJson().extractingPath("$.content[0].conversationId").isEqualTo((int) high);
    }

    @Test
    void givenRecordedTurnAndActiveTurn_whenRead_thenInProgressShown() {
        // Arrange
        long conversationId = turns.conversationCreatedAt("2026-11-02T10:00:00Z");
        turns.setTurnCreatedAt(turns.answeredTurn(conversationId), "2026-11-02T10:00:00Z");
        registry.tryStart(conversationId, "Yenisi kaç günde gelir?", Instant.parse("2026-11-02T10:31:05Z"));

        // Act
        var list = mvc.get().uri(LIST).exchange();
        var detail = detail(conversationId);

        // Assert
        assertThat(list).bodyJson().extractingPath("$.content[0].turnCount").isEqualTo(2);
        assertThat(list).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("IN_PROGRESS");
        assertThat(detail).bodyJson().extractingPath("$.turns").asList().hasSize(1);
        assertThat(detail).bodyJson().extractingPath("$.inProgressTurn.turnNo").isEqualTo(2);
        assertThat(detail).bodyJson().extractingPath("$.inProgressTurn.question").isEqualTo("Yenisi kaç günde gelir?");
        assertThat(detail).bodyJson().extractingPath("$.inProgressTurn.startedAt").isEqualTo("2026-11-02T10:31:05Z");
    }

    @Test
    void givenOnlyActiveTurn_whenListConversations_thenListedWithActiveQuestionUntilFinished() {
        // Arrange
        long conversationId = turns.conversation();
        registry.tryStart(conversationId, "İlk soru süren", Instant.parse("2026-11-02T10:31:05Z"));

        // Act
        var during = mvc.get().uri(LIST).exchange();
        registry.finish(conversationId);
        var after = mvc.get().uri(LIST).exchange();

        // Assert
        assertThat(during).bodyJson().extractingPath("$.totalElements").isEqualTo(1);
        assertThat(during).bodyJson().extractingPath("$.content[0].turnCount").isEqualTo(1);
        assertThat(during).bodyJson().extractingPath("$.content[0].firstQuestion").isEqualTo("İlk soru süren");
        assertThat(during).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("IN_PROGRESS");
        assertThat(after).bodyJson().extractingPath("$.totalElements").isEqualTo(0);
    }

    @Test
    void givenTurnRecordedButRegistryNotFinished_whenRead_thenTurnShownOnce() {
        // Arrange
        long conversationId = turns.conversation();
        long turnId = turns.answeredTurn(conversationId);
        turns.describeTurn(turnId, "Soru", "Soru", new int[] {1});
        turns.setTurnCreatedAt(turnId, "2026-11-02T10:31:05.000001Z");
        registry.tryStart(conversationId, "Soru", Instant.parse("2026-11-02T10:31:05.000001789Z"));

        // Act
        var list = mvc.get().uri(LIST).exchange();
        var detail = detail(conversationId);

        // Assert
        assertThat(list).bodyJson().extractingPath("$.content[0].turnCount").isEqualTo(1);
        assertThat(list).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("GROUNDED");
        assertThat(detail).bodyJson().extractingPath("$.inProgressTurn").isNull();
        assertThat(detail).bodyJson().extractingPath("$.turns").asList().hasSize(1);
    }

    @Test
    void givenRerankFollowUpTurn_whenGetConversationDetail_thenPromptChunksInPromptOrder() {
        // Arrange
        long conversationId = turns.conversation();
        long firstTurn = turns.answeredTurn(conversationId);
        long turnId = turns.answeredTurn(conversationId);
        turns.describeTurn(turnId, "Yenisi kaç günde gelir?", "Kaybolan kartın yenisi kaç günde gelir?", new int[] {1});
        int[] promptOrderVectorRanks = {7, 1, 3, 2, 9};
        for (int rank = 1; rank <= 20; rank++) {
            int position = java.util.Arrays.stream(promptOrderVectorRanks).boxed().toList().indexOf(rank);
            turns.addChunk(turnId, rank, 150 + rank, position < 0 ? null : position + 1, rank == 7 ? 0.3124 : 0.4,
                    position < 0 ? null : 5.36 - position);
        }
        turns.addChunk(firstTurn, 1, 900, 1, 0.1, null);

        // Act
        var result = detail(conversationId);

        // Assert
        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.turns[0].turnNo").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.turns[1].turnNo").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.turns[1].searchQuery")
                .isEqualTo("Kaybolan kartın yenisi kaç günde gelir?");
        assertThat(result).bodyJson().extractingPath("$.turns[1].cited").asList().containsExactly(1);
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks").asList().hasSize(5);
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks[*].vectorRank").asList()
                .containsExactly(7, 1, 3, 2, 9);
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks[*].n").asList()
                .containsExactly(1, 2, 3, 4, 5);
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks[0].chunkId").isEqualTo(157);
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks[0].distance").isEqualTo(0.3124);
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks[0].rerankScore").isEqualTo(5.36);
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks[0].pageNumber").isEqualTo(19);
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks[0].version").isEqualTo("2026.10");
        assertThat(result).bodyJson().extractingPath("$.turns[0].contextChunks").asList().hasSize(1);
    }

    @Test
    void givenHandedOffConversation_whenGetConversationDetail_thenHandoffIncluded() {
        // Arrange
        long handed = turns.conversation();
        long turnId = turns.notFoundTurn(handed);
        long plain = turns.conversation();
        turns.answeredTurn(plain);
        mvc.post().uri(LIST + "/" + handed + "/handoff").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"NO_ANSWER\",\"turnId\":" + turnId + "}").exchange();

        // Act
        var withHandoff = detail(handed);
        var without = detail(plain);

        // Assert
        assertThat(withHandoff).bodyJson().extractingPath("$.handoff.queue").isEqualTo("bireysel");
        assertThat(withHandoff).bodyJson().extractingPath("$.handoff.reason").isEqualTo("NO_ANSWER");
        assertThat(withHandoff).bodyJson().extractingPath("$.handoff.turnId").isEqualTo((int) turnId);
        assertThat(withHandoff).bodyJson().extractingPath("$.handoff.createdAt").isEqualTo("2026-11-02T10:31:05Z");
        assertThat(without).bodyJson().extractingPath("$.handoff").isNull();
    }

    @Test
    void givenCitedTurn_whenFeedbackChanges_thenStatusChanges() {
        // Arrange
        long conversationId = turns.conversation();
        long turnId = turns.answeredTurn(conversationId);
        turns.describeTurn(turnId, "q", "q", new int[] {1});

        // Act
        var before = detail(conversationId);
        feedback(turnId, false);
        var wrong = detail(conversationId);
        var wrongList = mvc.get().uri(LIST).exchange();
        feedback(turnId, true);
        var grounded = detail(conversationId);

        // Assert
        assertThat(before).bodyJson().extractingPath("$.turns[0].status").isEqualTo("GROUNDED");
        assertThat(wrong).bodyJson().extractingPath("$.turns[0].status").isEqualTo("WRONG");
        assertThat(wrong).bodyJson().extractingPath("$.turns[0].helpful").isEqualTo(false);
        assertThat(wrongList).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("WRONG");
        assertThat(grounded).bodyJson().extractingPath("$.turns[0].status").isEqualTo("GROUNDED");
    }

    @Test
    void givenOneTurnPerStatusRule_whenGetConversationDetail_thenEachStatusReported() {
        // Arrange
        long conversationId = turns.conversation();
        long grounded = turns.answeredTurn(conversationId);
        turns.describeTurn(grounded, "q1", "q1", new int[] {1});
        long ungrounded = turns.answeredTurn(conversationId);
        long dontKnow = turns.notFoundTurn(conversationId);
        long incomplete = turns.failedTurn(conversationId, "CHAT_MODEL_UNAVAILABLE");
        long llmOnly = turns.llmOnlyTurn(conversationId);
        long wrong = turns.answeredTurn(conversationId);
        turns.describeTurn(wrong, "q6", "q6", new int[] {1});
        feedback(wrong, false);

        // Act
        var result = detail(conversationId);

        // Assert
        assertThat(result).bodyJson().extractingPath("$.turns[*].status").asList().containsExactly(
                "GROUNDED", "UNGROUNDED", "DONT_KNOW", "INCOMPLETE", "UNGROUNDED", "WRONG");
        assertThat(result).bodyJson().extractingPath("$.turns[3].errorCode").isEqualTo("CHAT_MODEL_UNAVAILABLE");
        assertThat(result).bodyJson().extractingPath("$.turns[3].answer").isNull();
        assertThat(java.util.List.of(ungrounded, dontKnow, incomplete, llmOnly)).hasSize(4);
    }

    @Test
    void givenLlmOnlyTurn_whenGetConversationDetail_thenNoChunksAndUngrounded() {
        long conversationId = turns.conversation();
        turns.llmOnlyTurn(conversationId);

        var result = detail(conversationId);

        assertThat(result).bodyJson().extractingPath("$.turns[0].generationMode").isEqualTo("LLM_ONLY");
        assertThat(result).bodyJson().extractingPath("$.turns[0].retrievalStrategy").isEqualTo("NONE");
        assertThat(result).bodyJson().extractingPath("$.turns[0].searchQuery").isNull();
        assertThat(result).bodyJson().extractingPath("$.turns[0].contextChunks").asList().isEmpty();
        assertThat(result).bodyJson().extractingPath("$.turns[0].status").isEqualTo("UNGROUNDED");
    }

    @Test
    void givenConversationWithoutTurns_whenGetConversationDetail_thenEmptyTurns() {
        long conversationId = turns.conversation();

        var result = detail(conversationId);

        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.turns").asList().isEmpty();
        assertThat(result).bodyJson().extractingPath("$.inProgressTurn").isNull();
        assertThat(result).bodyJson().extractingPath("$.handoff").isNull();
        assertThat(mvc.get().uri(LIST).exchange()).bodyJson().extractingPath("$.totalElements").isEqualTo(0);
    }

    @Test
    void givenUnknownConversation_whenGetConversationDetail_thenNotFoundProblem() {
        var result = detail(424242);

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("CONVERSATION_NOT_FOUND");
    }
}
