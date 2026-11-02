package com.rasitbank.assistant.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tur satırlarını doğrudan SQL ile yazar; geri bildirim ve aktarma testlerinin ön koşullarını kurar.
 */
public final class TurnFixtures {

    private final JdbcTemplate jdbc;

    public TurnFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void clean() {
        jdbc.execute("truncate conversations, conversation_turns, turn_chunks, handoffs restart identity cascade");
    }

    public long conversation() {
        return jdbc.queryForObject(
                "insert into conversations (channel, created_at) values ('PREVIEW', now()) returning id", Long.class);
    }

    public long answeredTurn(long conversationId) {
        return ragTurn(conversationId, "Kartını kapat [1].", false, null);
    }

    public long notFoundTurn(long conversationId) {
        return ragTurn(conversationId, TestProperties.NOT_FOUND_ANSWER, true, null);
    }

    public long failedTurn(long conversationId, String errorCode) {
        return ragTurn(conversationId, null, false, errorCode);
    }

    public long llmOnlyTurn(long conversationId) {
        int turnNo = nextTurnNo(conversationId);
        return jdbc.queryForObject("""
                insert into conversation_turns (conversation_id, turn_no, question, search_query, query_rewrite_status,
                    generation_mode, retrieval_strategy, candidates, top_k, chat_model, answer, not_found_answer,
                    latency_ms, created_at, completed_at)
                values (?, ?, 'q', null, 'DISABLED', 'LLM_ONLY', 'NONE', null, null, 'm', 'a', false, 10, now(), now())
                returning id""", Long.class, conversationId, turnNo);
    }

    public long conversationCreatedAt(String createdAt) {
        return jdbc.queryForObject(
                "insert into conversations (channel, created_at) values ('PREVIEW', ?::timestamptz) returning id",
                Long.class, createdAt);
    }

    public void describeTurn(long turnId, String question, String searchQuery, int[] cited) {
        jdbc.update("update conversation_turns set question = ?, search_query = ?, cited = ?::int[] where id = ?",
                question, searchQuery, "{" + java.util.Arrays.stream(cited).mapToObj(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(",")) + "}", turnId);
    }

    public void setTurnCreatedAt(long turnId, String createdAt) {
        jdbc.update("update conversation_turns set created_at = ?::timestamptz where id = ?", createdAt, turnId);
    }

    public void addChunk(long turnId, int vectorRank, long chunkId, Integer promptPosition, double distance,
            Double rerankScore) {
        jdbc.update("""
                insert into turn_chunks (turn_id, vector_rank, chunk_id, source_id, version, chunk_no, page_number,
                    heading, distance, rerank_score, prompt_position)
                values (?, ?, ?, 10001, '2026.10', ?, 19, '2.16 Kayıp ya da çalıntı bildirimi', ?, ?, ?)""",
                turnId, vectorRank, chunkId, (int) (chunkId % 1000), distance, rerankScore, promptPosition);
    }

    public int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private long ragTurn(long conversationId, String answer, boolean notFound, String errorCode) {
        int turnNo = nextTurnNo(conversationId);
        return jdbc.queryForObject("""
                insert into conversation_turns (conversation_id, turn_no, question, search_query, query_rewrite_status,
                    generation_mode, retrieval_strategy, candidates, top_k, chat_model, answer, not_found_answer,
                    latency_ms, error_code, created_at, completed_at)
                values (?, ?, 'q', 'q', 'DISABLED', 'RAG', 'RERANK', 20, 5, 'm', ?, ?, 10, ?, now(), now())
                returning id""", Long.class, conversationId, turnNo, answer, notFound, errorCode);
    }

    private int nextTurnNo(long conversationId) {
        return jdbc.queryForObject(
                "select coalesce(max(turn_no), 0) + 1 from conversation_turns where conversation_id = ?",
                Integer.class, conversationId);
    }
}
