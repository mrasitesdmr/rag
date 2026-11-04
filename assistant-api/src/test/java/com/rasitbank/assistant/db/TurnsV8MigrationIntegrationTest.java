package com.rasitbank.assistant.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.rasitbank.assistant.support.PostgresIntegrationTest;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * V7 durumundaki tur kayıtlarının V8 ile doğru doldurulduğunu ve V8'in tutarlılık kısıtlarının geçersiz
 * birleşimleri reddedip geçerlileri kabul ettiğini ayrı bir veritabanında sınar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TurnsV8MigrationIntegrationTest extends PostgresIntegrationTest {

    private static final String DATABASE = "v8_migration_test";

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private JdbcTemplate admin;
    private String migrationUrl;
    private JdbcTemplate jdbc;

    @BeforeEach
    void createFreshDatabase() {
        admin = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        admin.execute("drop database if exists " + DATABASE + " with (force)");
        admin.execute("create database " + DATABASE);
        migrationUrl = url.substring(0, url.lastIndexOf('/') + 1) + DATABASE
                + (url.contains("?") ? url.substring(url.indexOf('?')) : "");
        jdbc = new JdbcTemplate(new DriverManagerDataSource(migrationUrl, username, password));
    }

    private void migrateTo(String target) {
        Flyway.configure().dataSource(migrationUrl, username, password).locations("classpath:db/migration")
                .target(target).load().migrate();
    }

    private long conversation() {
        return jdbc.queryForObject(
                "insert into conversations (channel, created_at) values ('PREVIEW', now()) returning id", Long.class);
    }

    private long v7Turn(String mode, String strategy, boolean notFound, String errorCode) {
        boolean none = "NONE".equals(strategy);
        return jdbc.queryForObject("""
                insert into conversation_turns (conversation_id, turn_no, question, search_query, query_rewrite_status,
                    generation_mode, retrieval_strategy, candidates, top_k, chat_model, answer, not_found_answer,
                    latency_ms, error_code, created_at, completed_at)
                values (?, 1, 'q', ?, 'DISABLED', ?, ?, ?, ?, 'm', 'a', ?, 10, ?, now(), now()) returning id""",
                Long.class, conversation(), none ? null : "q", mode, strategy, none ? null : 20, none ? null : 5,
                notFound, errorCode);
    }

    private void v7Chunk(long turnId) {
        jdbc.update("""
                insert into turn_chunks (turn_id, vector_rank, chunk_id, source_id, version, chunk_no, page_number,
                    heading, distance, rerank_score, prompt_position)
                values (?, 1, 101, 10001, '2026.10', 1, 19, 'h', 0.3, 1.0, 1)""", turnId);
    }

    private String answerSourceOf(long turnId) {
        return jdbc.queryForObject("select answer_source from conversation_turns where id = ?", String.class, turnId);
    }

    private Throwable insertV8(String mode, String strategy, boolean notFound, String answerSource, String label,
            Integer labelLatency, String cited) {
        boolean none = "NONE".equals(strategy);
        return catchThrowable(() -> jdbc.update("""
                insert into conversation_turns (conversation_id, turn_no, question, search_query, query_rewrite_status,
                    generation_mode, retrieval_strategy, candidates, top_k, chat_model, answer, not_found_answer,
                    latency_ms, created_at, completed_at, answer_source, message_label, message_label_latency_ms, cited)
                values (?, 1, 'q', ?, 'DISABLED', ?, ?, ?, ?, 'm', 'a', ?, 10, now(), now(), ?, ?, ?, ?::int[])""",
                conversation(), none ? null : "q", mode, strategy, none ? null : 20, none ? null : 5, notFound,
                answerSource, label, labelLatency, cited));
    }

    @Test
    void givenV7Turns_whenV8Migrated_thenAnswerSourceBackfilled() {
        // Arrange
        migrateTo("7");
        long emptyKnowledgeBase = v7Turn("RAG", "RERANK", true, null);
        long notFoundWithChunks = v7Turn("RAG", "RERANK", true, null);
        v7Chunk(notFoundWithChunks);
        long answered = v7Turn("RAG", "RERANK", false, null);
        v7Chunk(answered);
        long llmOnly = v7Turn("LLM_ONLY", "NONE", false, null);
        long failedNotFound = v7Turn("RAG", "RERANK", true, "CHAT_MODEL_UNAVAILABLE");

        // Act
        migrateTo("8");

        // Assert
        assertThat(answerSourceOf(emptyKnowledgeBase)).isEqualTo("EMPTY_KNOWLEDGE_BASE");
        assertThat(answerSourceOf(notFoundWithChunks)).isEqualTo("MODEL");
        assertThat(answerSourceOf(answered)).isEqualTo("MODEL");
        assertThat(answerSourceOf(llmOnly)).isEqualTo("MODEL");
        assertThat(answerSourceOf(failedNotFound)).isEqualTo("MODEL");
        Map<String, Object> labels = jdbc.queryForMap(
                "select count(message_label) as labels, count(message_label_latency_ms) as latencies "
                        + "from conversation_turns");
        assertThat(labels.get("labels")).isEqualTo(0L);
        assertThat(labels.get("latencies")).isEqualTo(0L);
    }

    @Test
    void givenV8Schema_whenInsertingValidCombinations_thenAccepted() {
        // Arrange
        migrateTo("8");

        // Act
        List<Throwable> results = java.util.Arrays.asList(
                insertV8("RAG", "NONE", false, "SMALL_TALK", "GREETING", null, "{}"),
                insertV8("RAG", "RERANK", false, "SMALL_TALK", "THANKS", 640, "{}"),
                insertV8("RAG", "RERANK", true, "BELOW_RERANK_THRESHOLD", "OTHER", 700, "{}"),
                insertV8("RAG", "RERANK", true, "EMPTY_KNOWLEDGE_BASE", null, null, "{}"),
                insertV8("LLM_ONLY", "NONE", false, "MODEL", null, null, "{}"));

        // Assert
        assertThat(results).containsOnlyNulls();
    }

    @Test
    void givenV8Schema_whenInsertingInvalidCombinations_thenEachRejectedByItsConstraint() {
        // Arrange
        migrateTo("8");

        // Act
        Throwable noneWithoutReason = insertV8("RAG", "NONE", false, "MODEL", null, null, "{}");
        Throwable smallTalkOther = insertV8("RAG", "NONE", false, "SMALL_TALK", "OTHER", null, "{}");
        Throwable smallTalkWithCitation = insertV8("RAG", "NONE", false, "SMALL_TALK", "GREETING", null, "{1}");
        Throwable belowThresholdTopK = insertV8("RAG", "TOP_K", true, "BELOW_RERANK_THRESHOLD", "OTHER", null, "{}");
        Throwable belowThresholdAnswered = insertV8("RAG", "RERANK", false, "BELOW_RERANK_THRESHOLD", "OTHER", null,
                "{}");
        Throwable latencyWithoutLabel = insertV8("RAG", "RERANK", true, "EMPTY_KNOWLEDGE_BASE", null, 5, "{}");
        Throwable unknownLabel = insertV8("RAG", "RERANK", true, "EMPTY_KNOWLEDGE_BASE", "FOO", null, "{}");
        Throwable negativeLatency = insertV8("RAG", "RERANK", true, "BELOW_RERANK_THRESHOLD", "OTHER", -1, "{}");
        Throwable unknownSource = insertV8("RAG", "RERANK", true, "WHATEVER", null, null, "{}");

        // Assert
        assertThat(noneWithoutReason).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_conversation_turns_none_reason");
        assertThat(smallTalkOther).hasMessageContaining("ck_conversation_turns_small_talk");
        assertThat(smallTalkWithCitation).hasMessageContaining("ck_conversation_turns_small_talk");
        assertThat(belowThresholdTopK).hasMessageContaining("ck_conversation_turns_below_threshold");
        assertThat(belowThresholdAnswered).hasMessageContaining("ck_conversation_turns_below_threshold");
        assertThat(latencyWithoutLabel).hasMessageContaining("ck_conversation_turns_label_latency_has_label");
        assertThat(unknownLabel).hasMessageContaining("ck_conversation_turns_message_label");
        assertThat(negativeLatency).hasMessageContaining("ck_conversation_turns_message_label_latency");
        assertThat(unknownSource).hasMessageContaining("ck_conversation_turns_answer_source");
    }

    @Test
    void givenV8Schema_whenLlmOnlyWithRetrievalStrategy_thenRejected() {
        // Arrange
        migrateTo("8");

        // Act
        Throwable thrown = catchThrowable(() -> jdbc.update("""
                insert into conversation_turns (conversation_id, turn_no, question, search_query, query_rewrite_status,
                    generation_mode, retrieval_strategy, candidates, top_k, chat_model, answer, not_found_answer,
                    latency_ms, created_at, completed_at)
                values (?, 1, 'q', 'q', 'DISABLED', 'LLM_ONLY', 'RERANK', 20, 5, 'm', 'a', false, 10, now(), now())""",
                conversation()));

        // Assert
        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
    }
}
