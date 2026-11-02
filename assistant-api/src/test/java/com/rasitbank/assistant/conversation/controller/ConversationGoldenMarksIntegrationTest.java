package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

import com.rasitbank.assistant.evaluation.domain.dto.GoldenQuestionRequest;
import com.rasitbank.assistant.evaluation.domain.dto.ReplaceGoldenSetRequest;
import com.rasitbank.assistant.evaluation.service.GoldenSetService;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.client.ChunkMatchesResponse;
import com.rasitbank.assistant.retrieval.client.ChunkTextMatch;
import com.rasitbank.assistant.retrieval.client.MatchedChunk;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.TurnFixtures;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Golden set'teki sorular için Konuşmalar'ın tur durumunu (KAÇIRDI, ÜRETİM HATASI) ve "cevap burada" işaretini gerçek
 * veritabanı ve sahte retrieval-api ile doğrular; retrieval-api kapalıyken sayfanın işaretsiz açıldığını da.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ConversationGoldenMarksIntegrationTest extends PostgresIntegrationTest {

    private static final String LIST = "/api/assistant/conversations";
    private static final String QUESTION_A = "Bitcoin alıp satabilir miyim?";
    private static final String QUESTION_B = "Kripto hesabı açılır mı?";
    private static final String ANSWER_A = "Kripto para alım satımı bankamız üzerinden yapılamaz.";
    private static final String ANSWER_B = "Kripto hesabı bankamızda açılmaz ve saklama hizmeti verilmez.";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private GoldenSetService goldenSetService;

    @MockitoBean
    private RetrievalClient retrievalClient;

    private TurnFixtures turns;

    @BeforeEach
    void setUp() {
        turns = new TurnFixtures(jdbc);
        turns.clean();
        jdbc.execute("truncate golden_sets restart identity cascade");
        reset(retrievalClient);
        goldenSetService.replaceGoldenSet(new ReplaceGoldenSetRequest("rehber-golden-v1", List.of(
                new GoldenQuestionRequest("Yatırım", QUESTION_A, 40, ANSWER_A),
                new GoldenQuestionRequest("Yatırım", QUESTION_B, 41, ANSWER_B))));
    }

    private static ChunkTextMatch match(String text, long... chunkIds) {
        List<MatchedChunk> chunks = java.util.Arrays.stream(chunkIds)
                .mapToObj(id -> new MatchedChunk(id, 10001, "Rehber", "2026.10", 40)).toList();
        return new ChunkTextMatch(text, chunks);
    }

    private long notFoundTurnWithPrompt(long conversationId, String question) {
        long turnId = turns.notFoundTurn(conversationId);
        turns.describeTurn(turnId, question, question, new int[] {});
        for (int rank = 1; rank <= 5; rank++) {
            turns.addChunk(turnId, rank, 100L + rank, rank, 0.3, 1.0);
        }
        turns.addChunk(turnId, 7, 150, null, 0.4, 0.5);
        return turnId;
    }

    @Test
    void givenGoldenNotFoundTurn_whenGetConversationDetail_thenMissedAndNoChunkContainsAnswer() {
        // Arrange: A'nın cevap parçası 150 (aday, prompt'a girmedi); B'nin cevap parçası 103 (prompt'ta 3.)
        long conversationId = turns.conversation();
        notFoundTurnWithPrompt(conversationId, QUESTION_A);
        notFoundTurnWithPrompt(conversationId, QUESTION_B);
        given(retrievalClient.findChunkMatches(anyList())).willReturn(
                new ChunkMatchesResponse(List.of(match(ANSWER_A, 150), match(ANSWER_B, 103))));

        // Act
        var result = mvc.get().uri(LIST + "/" + conversationId).exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.turns[0].status").isEqualTo("MISSED");
        assertThat(result).bodyJson().extractingPath("$.turns[0].contextChunks[*].containsAnswer").asList()
                .containsExactly(false, false, false, false, false);
        assertThat(result).bodyJson().extractingPath("$.turns[1].status").isEqualTo("GENERATION_ERROR");
        assertThat(result).bodyJson().extractingPath("$.turns[1].contextChunks[*].containsAnswer").asList()
                .containsExactly(false, false, true, false, false);
    }

    @Test
    void givenQuestionNotInGoldenSet_whenGetConversationDetail_thenDontKnowAndNoMarks() {
        // Arrange
        long conversationId = turns.conversation();
        notFoundTurnWithPrompt(conversationId, "Altın hesabı var mı?");

        // Act
        var result = mvc.get().uri(LIST + "/" + conversationId).exchange();

        // Assert
        assertThat(result).bodyJson().extractingPath("$.turns[0].status").isEqualTo("DONT_KNOW");
        assertThat(result).bodyJson().extractingPath("$.turns[0].contextChunks[*].containsAnswer").asList()
                .containsOnly(false);
        org.mockito.Mockito.verifyNoInteractions(retrievalClient);
    }

    @Test
    void givenRetrievalDown_whenGetConversationDetail_thenDontKnowWithoutMarks() {
        // Arrange
        long conversationId = turns.conversation();
        notFoundTurnWithPrompt(conversationId, QUESTION_B);
        given(retrievalClient.findChunkMatches(anyList()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        // Act
        var result = mvc.get().uri(LIST + "/" + conversationId).exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.turns[0].status").isEqualTo("DONT_KNOW");
        assertThat(result).bodyJson().extractingPath("$.turns[0].contextChunks[*].containsAnswer").asList()
                .containsOnly(false);
    }

    @Test
    void givenLastTurnMissed_whenListConversations_thenLastTurnStatusMissed() {
        // Arrange
        long conversationId = turns.conversation();
        notFoundTurnWithPrompt(conversationId, QUESTION_A);
        given(retrievalClient.findChunkMatches(anyList()))
                .willReturn(new ChunkMatchesResponse(List.of(match(ANSWER_A, 150))));

        // Act
        var result = mvc.get().uri(LIST).exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("MISSED");
    }

    @Test
    void givenLastTurnAnswerChunkInPrompt_whenListConversations_thenGenerationError() {
        // Arrange
        long conversationId = turns.conversation();
        notFoundTurnWithPrompt(conversationId, QUESTION_B);
        given(retrievalClient.findChunkMatches(anyList()))
                .willReturn(new ChunkMatchesResponse(List.of(match(ANSWER_B, 102))));

        // Act
        var result = mvc.get().uri(LIST).exchange();

        // Assert
        assertThat(result).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("GENERATION_ERROR");
    }

    @Test
    void givenRetrievalDown_whenListConversations_thenListOpensWithDontKnow() {
        // Arrange
        long conversationId = turns.conversation();
        notFoundTurnWithPrompt(conversationId, QUESTION_A);
        given(retrievalClient.findChunkMatches(anyList()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        // Act
        var result = mvc.get().uri(LIST).exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("DONT_KNOW");
    }

    @Test
    void givenAnsweredLastTurnOfGoldenQuestion_whenListConversations_thenRetrievalNotAskedAndGrounded() {
        // Arrange
        long conversationId = turns.conversation();
        long turnId = turns.answeredTurn(conversationId);
        turns.describeTurn(turnId, QUESTION_A, QUESTION_A, new int[] {1});

        // Act
        var result = mvc.get().uri(LIST).exchange();

        // Assert
        assertThat(result).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("GROUNDED");
        org.mockito.Mockito.verifyNoInteractions(retrievalClient);
    }
}
