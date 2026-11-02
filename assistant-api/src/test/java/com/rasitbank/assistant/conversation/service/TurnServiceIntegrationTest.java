package com.rasitbank.assistant.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.conversation.domain.dto.CreateConversationRequest;
import com.rasitbank.assistant.conversation.domain.entity.Channel;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import com.rasitbank.assistant.support.ContextFixtures;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tur kaydının gerçek PostgreSQL'e doğru satırlarla (tur, 20 aday, prompt yerleri, atıflar) yazıldığını doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TurnServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-11-01T10:00:00Z");

    @Autowired
    private TurnService turnService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private JdbcTemplate jdbc;

    private long newConversation() {
        return conversationService.createConversation(new CreateConversationRequest(Channel.PREVIEW)).conversationId();
    }

    private static TurnDraft draft(long conversationId, RetrievedContext context) {
        return new TurnDraft(conversationId, GenerationMode.RAG, "Kartımı kaybettim, ne yapmalıyım?",
                "Kartımı kaybettim, ne yapmalıyım?", QueryRewriteStatus.NO_HISTORY, null, 0, "qwen3.5:9b", RECEIVED_AT, System.nanoTime(), context);
    }

    private static TurnOutcome successOutcome() {
        return new TurnOutcome("Kartını kapat [1]. Ara [3].", false, List.of(1, 3), 2154, 61, 3920L, 900L, 4730L, null);
    }

    @Test
    void givenRerankContext_whenRecordTurn_thenTurnRowAndTwentyCandidateRows() {
        long conversationId = newConversation();

        long turnId = turnService.recordTurn(draft(conversationId, ContextFixtures.rerankContext(7, 2, 15, 1, 3)),
                successOutcome());

        Map<String, Object> turn = jdbc.queryForMap(
                "select *, cited::text as cited_text from conversation_turns where id = ?", turnId);
        assertThat(turn.get("conversation_id")).isEqualTo(conversationId);
        assertThat(turn.get("turn_no")).isEqualTo(1);
        assertThat(turn.get("question")).isEqualTo("Kartımı kaybettim, ne yapmalıyım?");
        assertThat(turn.get("search_query")).isEqualTo(turn.get("question"));
        assertThat(turn.get("generation_mode")).isEqualTo("RAG");
        assertThat(turn.get("retrieval_strategy")).isEqualTo("RERANK");
        assertThat(turn.get("candidates")).isEqualTo(20);
        assertThat(turn.get("top_k")).isEqualTo(5);
        assertThat(turn.get("chat_model")).isEqualTo("qwen3.5:9b");
        assertThat(turn.get("answer")).isEqualTo("Kartını kapat [1]. Ara [3].");
        assertThat(turn.get("not_found_answer")).isEqualTo(false);
        assertThat(turn.get("cited_text")).isEqualTo("{1,3}");
        assertThat(turn.get("prompt_tokens")).isEqualTo(2154);
        assertThat(turn.get("completion_tokens")).isEqualTo(61);
        assertThat(turn.get("retrieval_latency_ms")).isEqualTo(84);
        assertThat(turn.get("rerank_latency_ms")).isEqualTo(712);
        assertThat(turn.get("generation_latency_ms")).isEqualTo(3920);
        assertThat(turn.get("time_to_first_token_ms")).isEqualTo(900);
        assertThat(turn.get("latency_ms")).isEqualTo(4730);
        assertThat(turn.get("error_code")).isNull();
        assertThat(((java.sql.Timestamp) turn.get("created_at")).toInstant()).isEqualTo(RECEIVED_AT);
        assertThat(((java.sql.Timestamp) turn.get("completed_at")).toInstant()).isAfter(RECEIVED_AT);

        List<Map<String, Object>> chunks = jdbc.queryForList(
                "select * from turn_chunks where turn_id = ? order by vector_rank", turnId);
        assertThat(chunks).hasSize(20);
        assertThat(chunks).extracting(row -> row.get("vector_rank"))
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList());
        assertThat(chunks).filteredOn(row -> row.get("prompt_position") != null).hasSize(5);
        Map<String, Object> vectorSeven = chunks.get(6);
        assertThat(vectorSeven.get("prompt_position")).isEqualTo(1);
        assertThat(vectorSeven.get("chunk_id")).isEqualTo(107L);
        assertThat(vectorSeven.get("chunk_no")).isEqualTo(7);
        assertThat(vectorSeven.get("page_number")).isEqualTo(17);
        assertThat(vectorSeven.get("heading")).isEqualTo("H107");
        assertThat(((Number) vectorSeven.get("distance")).doubleValue()).isCloseTo(0.37, within(1e-9));
        assertThat(((Number) vectorSeven.get("rerank_score")).doubleValue()).isCloseTo(9.3, within(1e-9));
        assertThat(chunks).extracting(row -> row.get("source_id")).containsOnly(10001L);
        assertThat(chunks).extracting(row -> row.get("version")).containsOnly("2026.10");
        assertThat(chunks).extracting(row -> row.get("rerank_score")).doesNotContainNull();
        assertThat(chunks).filteredOn(row -> row.get("prompt_position") != null)
                .extracting(row -> row.get("vector_rank"), row -> row.get("prompt_position"))
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple(7, 1),
                        org.assertj.core.groups.Tuple.tuple(2, 2), org.assertj.core.groups.Tuple.tuple(15, 3),
                        org.assertj.core.groups.Tuple.tuple(1, 4), org.assertj.core.groups.Tuple.tuple(3, 5));
    }

    @Test
    void givenConversationWithOneTurn_whenRecordTurn_thenTurnNoIsTwo() {
        long conversationId = newConversation();
        turnService.recordTurn(draft(conversationId, ContextFixtures.topKContext()), successOutcome());

        long secondTurnId = turnService.recordTurn(draft(conversationId, ContextFixtures.topKContext()),
                successOutcome());

        assertThat(jdbc.queryForObject("select turn_no from conversation_turns where id = ?", Integer.class,
                secondTurnId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from conversation_turns where conversation_id = ?",
                Integer.class, conversationId)).isEqualTo(2);
    }

    @Test
    void givenTopKContext_whenRecordTurn_thenFiveRowsAllInPromptAndNoRerankScore() {
        long conversationId = newConversation();

        long turnId = turnService.recordTurn(draft(conversationId, ContextFixtures.topKContext()), successOutcome());

        assertThat(jdbc.queryForObject("select retrieval_strategy from conversation_turns where id = ?",
                String.class, turnId)).isEqualTo("TOP_K");
        assertThat(jdbc.queryForObject("select rerank_latency_ms from conversation_turns where id = ?",
                Integer.class, turnId)).isNull();
        List<Map<String, Object>> chunks = jdbc.queryForList(
                "select * from turn_chunks where turn_id = ? order by vector_rank", turnId);
        assertThat(chunks).hasSize(5);
        assertThat(chunks).extracting(row -> row.get("prompt_position")).containsExactly(1, 2, 3, 4, 5);
        assertThat(chunks).extracting(row -> row.get("rerank_score")).containsOnlyNulls();
    }

    @Test
    void givenRetrievalFailure_whenRecordTurn_thenTurnWithErrorCodeNoAnswerAndNoCandidateRows() {
        long conversationId = newConversation();
        TurnOutcome failed = TurnOutcome.failed(null, "RETRIEVAL_UNAVAILABLE", null, null, 1500L);

        long turnId = turnService.recordTurn(draft(conversationId, null), failed);

        Map<String, Object> turn = jdbc.queryForMap("select * from conversation_turns where id = ?", turnId);
        assertThat(turn.get("error_code")).isEqualTo("RETRIEVAL_UNAVAILABLE");
        assertThat(turn.get("answer")).isNull();
        assertThat(turn.get("retrieval_strategy")).isEqualTo("RERANK");
        assertThat(turn.get("candidates")).isEqualTo(20);
        assertThat(turn.get("top_k")).isEqualTo(5);
        assertThat(turn.get("retrieval_latency_ms")).isNull();
        assertThat(turn.get("latency_ms")).isEqualTo(1500);
        assertThat(jdbc.queryForObject("select count(*) from turn_chunks where turn_id = ?", Integer.class, turnId))
                .isZero();
    }

    @Test
    void givenNotFoundOutcome_whenRecordTurn_thenNotFoundFlagAndEmptyCitationsStored() {
        long conversationId = newConversation();
        TurnOutcome outcome = new TurnOutcome("Bu konuda rehberimizde bilgi bulamadım.", true, List.of(), null, null,
                null, null, 20L, null);

        long turnId = turnService.recordTurn(draft(conversationId, ContextFixtures.emptyContext(null)), outcome);

        Map<String, Object> turn = jdbc.queryForMap(
                "select not_found_answer, cited::text as cited_text, prompt_tokens from conversation_turns "
                        + "where id = ?", turnId);
        assertThat(turn.get("not_found_answer")).isEqualTo(true);
        assertThat(turn.get("cited_text")).isEqualTo("{}");
        assertThat(turn.get("prompt_tokens")).isNull();
    }

    @Test
    void givenRewrittenDraft_whenRecordTurn_thenStoresSearchQueryStatusLatencyAndHistoryTurns() {
        long conversationId = newConversation();
        TurnDraft rewritten = new TurnDraft(conversationId, GenerationMode.RAG, "Yenisi kaç günde gelir?",
                "Kaybolan kartın yenisi kaç günde gelir?", QueryRewriteStatus.REWRITTEN, 812L, 1, "qwen3.5:9b",
                RECEIVED_AT, System.nanoTime(), ContextFixtures.topKContext());

        long turnId = turnService.recordTurn(rewritten, successOutcome());

        Map<String, Object> turn = jdbc.queryForMap("select * from conversation_turns where id = ?", turnId);
        assertThat(turn.get("question")).isEqualTo("Yenisi kaç günde gelir?");
        assertThat(turn.get("search_query")).isEqualTo("Kaybolan kartın yenisi kaç günde gelir?");
        assertThat(turn.get("query_rewrite_status")).isEqualTo("REWRITTEN");
        assertThat(turn.get("query_rewrite_latency_ms")).isEqualTo(812);
        assertThat(turn.get("history_turns")).isEqualTo(1);
    }

    @Test
    void givenNoHistoryDraft_whenRecordTurn_thenStatusNoHistoryAndNullLatency() {
        long conversationId = newConversation();

        long turnId = turnService.recordTurn(draft(conversationId, ContextFixtures.topKContext()), successOutcome());

        Map<String, Object> turn = jdbc.queryForMap("select * from conversation_turns where id = ?", turnId);
        assertThat(turn.get("query_rewrite_status")).isEqualTo("NO_HISTORY");
        assertThat(turn.get("query_rewrite_latency_ms")).isNull();
        assertThat(turn.get("history_turns")).isEqualTo(0);
    }

    private static TurnDraft llmOnlyDraft(long conversationId, int historyTurns) {
        return new TurnDraft(conversationId, GenerationMode.LLM_ONLY, "Kartımı kaybettim, ne yapmalıyım?", null,
                QueryRewriteStatus.DISABLED, null, historyTurns, "qwen3.5:9b", RECEIVED_AT, System.nanoTime(), null);
    }

    @Test
    void givenLlmOnlyDraft_whenRecordTurn_thenNoneStrategyAndNoChunks() {
        // Arrange
        long conversationId = newConversation();
        TurnOutcome outcome = new TurnOutcome("Kartını kapat [1].", false, List.of(), 63, 9, 1300L, 400L, 1400L, null);

        // Act
        long turnId = turnService.recordTurn(llmOnlyDraft(conversationId, 0), outcome);

        // Assert
        Map<String, Object> turn = jdbc.queryForMap(
                "select *, cited::text as cited_text from conversation_turns where id = ?", turnId);
        assertThat(turn.get("generation_mode")).isEqualTo("LLM_ONLY");
        assertThat(turn.get("retrieval_strategy")).isEqualTo("NONE");
        assertThat(turn.get("search_query")).isNull();
        assertThat(turn.get("candidates")).isNull();
        assertThat(turn.get("top_k")).isNull();
        assertThat(turn.get("retrieval_latency_ms")).isNull();
        assertThat(turn.get("rerank_latency_ms")).isNull();
        assertThat(turn.get("query_rewrite_status")).isEqualTo("DISABLED");
        assertThat(turn.get("cited_text")).isEqualTo("{}");
        assertThat(turn.get("not_found_answer")).isEqualTo(false);
        assertThat(turn.get("answer")).isEqualTo("Kartını kapat [1].");
        assertThat(turn.get("prompt_tokens")).isEqualTo(63);
        assertThat(jdbc.queryForObject("select count(*) from turn_chunks where turn_id = ?", Integer.class, turnId))
                .isZero();
    }

    @Test
    void givenLlmOnlyDraftWithUnexpectedContext_whenRecordTurn_thenContextIgnored() {
        // Arrange
        long conversationId = newConversation();
        TurnDraft draft = new TurnDraft(conversationId, GenerationMode.LLM_ONLY, "q", "must-not-be-stored",
                QueryRewriteStatus.DISABLED, null, 0, "qwen3.5:9b", RECEIVED_AT, System.nanoTime(),
                ContextFixtures.topKContext());

        // Act
        long turnId = turnService.recordTurn(draft, successOutcome());

        // Assert
        Map<String, Object> turn = jdbc.queryForMap("select * from conversation_turns where id = ?", turnId);
        assertThat(turn.get("retrieval_strategy")).isEqualTo("NONE");
        assertThat(turn.get("search_query")).isNull();
        assertThat(turn.get("candidates")).isNull();
        assertThat(jdbc.queryForObject("select count(*) from turn_chunks where turn_id = ?", Integer.class, turnId))
                .isZero();
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "[{index}] {0}/{1} search={2} candidates={3} retrievalMs={4}")
    @org.junit.jupiter.params.provider.CsvSource(value = {
            "LLM_ONLY, RERANK, null, null, null",
            "RAG, NONE, null, null, null",
            "LLM_ONLY, NONE, null, 20, null",
            "LLM_ONLY, NONE, null, null, 84",
            "RAG, RERANK, null, 20, null",
            "RAG, TOP_K, q, null, null"}, nullValues = "null")
    void givenMigrations_whenInconsistentModeAndStrategy_thenCheckViolation(String mode, String strategy,
            String searchQuery, Integer candidates, Integer retrievalLatencyMs) {
        // Arrange
        long conversationId = newConversation();

        // Act
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> insertTurn(conversationId, mode,
                strategy, searchQuery, candidates, candidates == null ? null : 5, retrievalLatencyMs));

        // Assert
        assertThat(thrown).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("violates check constraint");
    }

    @Test
    void givenMigrations_whenConsistentLlmOnlyRow_thenInsertAccepted() {
        // Arrange
        long conversationId = newConversation();

        // Act
        insertTurn(conversationId, "LLM_ONLY", "NONE", null, null, null, null);

        // Assert
        assertThat(jdbc.queryForObject("select count(*) from conversation_turns where conversation_id = ?",
                Integer.class, conversationId)).isEqualTo(1);
    }

    private void insertTurn(long conversationId, String mode, String strategy, String searchQuery, Integer candidates,
            Integer topK, Integer retrievalLatencyMs) {
        jdbc.update("insert into conversation_turns (conversation_id, turn_no, question, search_query, "
                + "generation_mode, retrieval_strategy, candidates, top_k, chat_model, retrieval_latency_ms, "
                + "query_rewrite_status, "
                + "latency_ms, created_at, completed_at) values (?, 1, 'q', ?, ?, ?, ?, ?, 'm', ?, 'DISABLED', 10, now(), now())",
                conversationId, searchQuery, mode, strategy, candidates, topK, retrievalLatencyMs);
    }
}
