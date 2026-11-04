package com.rasitbank.assistant.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rasitbank.assistant.conversation.domain.dto.CreateConversationRequest;
import com.rasitbank.assistant.conversation.domain.entity.Channel;
import com.rasitbank.assistant.conversation.domain.entity.AnswerSource;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.generation.HistoryTurn;
import com.rasitbank.assistant.support.ContextFixtures;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Geçmiş okumanın (son N cevabı tamamlanmış tur, eskiden yeniye, alıntısız) ve V2 migration kısıtlarının gerçek
 * PostgreSQL üzerinde çalıştığını doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ConversationServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-11-01T10:00:00Z");

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private TurnService turnService;

    @Autowired
    private JdbcTemplate jdbc;

    private long newConversation() {
        return conversationService.createConversation(new CreateConversationRequest(Channel.PREVIEW)).conversationId();
    }

    private void recordAnswered(long conversationId, String question, String answer) {
        turnService.recordTurn(draft(conversationId, question),
                new TurnOutcome(answer, false, List.of(1), 100, 10, 500L, 100L, 600L, null, AnswerSource.MODEL));
    }

    private static TurnDraft draft(long conversationId, String question) {
        return new TurnDraft(conversationId, GenerationMode.RAG, question, question, QueryRewriteStatus.NO_HISTORY, null, 0, "qwen3.5:9b",
                RECEIVED_AT, System.nanoTime(), ContextFixtures.topKContext(), null, null);
    }

    @Test
    void givenFiveAnsweredTurnsAndOneFailed_whenGetHistory_thenLastThreeAnsweredOldestFirst() {
        long conversationId = newConversation();
        for (int turnNo = 1; turnNo <= 5; turnNo++) {
            recordAnswered(conversationId, "Soru " + turnNo, "Cevap " + turnNo + " [1]");
        }
        turnService.recordTurn(draft(conversationId, "Soru 6"),
                TurnOutcome.failed(null, "CHAT_MODEL_UNAVAILABLE", null, null, 50L));

        List<HistoryTurn> history = conversationService.getHistory(conversationId);

        assertThat(history).containsExactly(new HistoryTurn("Soru 3", "Cevap 3"),
                new HistoryTurn("Soru 4", "Cevap 4"), new HistoryTurn("Soru 5", "Cevap 5"));
    }

    @Test
    void givenCancelledTurnWithPartialAnswer_whenGetHistory_thenSkipped() {
        long conversationId = newConversation();
        recordAnswered(conversationId, "Soru 1", "Cevap 1");
        turnService.recordTurn(draft(conversationId, "Soru 2"),
                TurnOutcome.failed("Yarım cev", "STREAM_CANCELLED", 100L, 50L, 150L));

        List<HistoryTurn> history = conversationService.getHistory(conversationId);

        assertThat(history).containsExactly(new HistoryTurn("Soru 1", "Cevap 1"));
    }

    @Test
    void givenFewerTurnsThanLimit_whenGetHistory_thenAllOldestFirst() {
        long conversationId = newConversation();
        recordAnswered(conversationId, "Soru 1", "Cevap 1");
        recordAnswered(conversationId, "Soru 2", "Cevap 2");

        assertThat(conversationService.getHistory(conversationId)).extracting(HistoryTurn::question)
                .containsExactly("Soru 1", "Soru 2");
    }

    @Test
    void givenAnotherConversationWithTurns_whenGetHistory_thenOnlyOwnTurns() {
        long other = newConversation();
        recordAnswered(other, "Başka soru", "Başka cevap");
        long conversationId = newConversation();

        assertThat(conversationService.getHistory(conversationId)).isEmpty();
    }

    @Test
    void givenMigrations_whenApplied_thenRewriteColumnsHaveConstraints() {
        long conversationId = newConversation();
        String insert = "insert into conversation_turns (conversation_id, turn_no, question, search_query, "
                + "generation_mode, retrieval_strategy, candidates, top_k, chat_model, latency_ms, created_at, "
                + "completed_at, query_rewrite_status, history_turns) "
                + "values (?, ?, 'q', 'q', 'RAG', 'TOP_K', 5, 5, 'm', 1, now(), now(), ?, ?)";

        assertThatThrownBy(() -> jdbc.update(insert, conversationId, 1, "FOO", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, conversationId, 1, "REWRITTEN", -1))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.update(insert, conversationId, 1, "EMPTY", 2)).isEqualTo(1);
    }

    @Test
    void givenTurnWithoutStatus_whenInserted_thenNotNullViolation() {
        long conversationId = newConversation();

        assertThatThrownBy(() -> jdbc.update("insert into conversation_turns (conversation_id, turn_no, question, "
                + "search_query, generation_mode, retrieval_strategy, candidates, top_k, chat_model, latency_ms, "
                + "created_at, completed_at) values (?, 1, 'q', 'q', 'RAG', 'TOP_K', 5, 5, 'm', 1, now(), now())",
                conversationId)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
