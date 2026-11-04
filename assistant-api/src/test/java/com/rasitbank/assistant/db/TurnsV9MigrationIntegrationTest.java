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
 * V9'un "harf ya da rakamsız mesaj" turunu yalnızca bulamadım + arama yok + etiket yok birleşimiyle kabul edip
 * diğer birleşimleri reddettiğini, eski birleşimlerin geçerliliğini koruduğunu ayrı bir veritabanında sınar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TurnsV9MigrationIntegrationTest extends PostgresIntegrationTest {

    private static final String DATABASE = "v9_migration_test";

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

    private Throwable insertTurn(String strategy, boolean notFound, String answerSource, String label,
            Integer labelLatency) {
        boolean none = "NONE".equals(strategy);
        return catchThrowable(() -> jdbc.update("""
                insert into conversation_turns (conversation_id, turn_no, question, search_query, query_rewrite_status,
                    generation_mode, retrieval_strategy, candidates, top_k, chat_model, answer, not_found_answer,
                    latency_ms, created_at, completed_at, answer_source, message_label, message_label_latency_ms, cited)
                values (?, 1, 'q', ?, 'DISABLED', 'RAG', ?, ?, ?, 'm', 'a', ?, 10, now(), now(), ?, ?, ?, '{}')""",
                conversation(), none ? null : "q", strategy, none ? null : 20, none ? null : 5, notFound,
                answerSource, label, labelLatency));
    }

    @Test
    void givenV9_whenInsertNoWordsTurns_thenOnlyNotFoundNoneWithoutLabelAccepted() {
        // Arrange
        migrateTo("9");

        // Act
        Throwable valid = insertTurn("NONE", true, "NO_WORDS", null, null);
        Throwable withRerank = insertTurn("RERANK", true, "NO_WORDS", null, null);
        Throwable notFoundFalse = insertTurn("NONE", false, "NO_WORDS", null, null);
        Throwable withLabel = insertTurn("NONE", true, "NO_WORDS", "OTHER", 5);
        Throwable withCitation = catchThrowable(() -> jdbc.update("""
                insert into conversation_turns (conversation_id, turn_no, question, query_rewrite_status,
                    generation_mode, retrieval_strategy, chat_model, answer, not_found_answer, latency_ms, created_at,
                    completed_at, answer_source, cited)
                values (?, 1, 'q', 'DISABLED', 'RAG', 'NONE', 'm', 'a', true, 10, now(), now(), 'NO_WORDS', '{1}')""",
                conversation()));
        Throwable noneWithModelSource = insertTurn("NONE", false, "MODEL", null, null);

        // Assert
        assertThat(valid).isNull();
        assertThat(withRerank).hasMessageContaining("ck_conversation_turns_no_words");
        assertThat(notFoundFalse).hasMessageContaining("ck_conversation_turns_no_words");
        assertThat(withLabel).hasMessageContaining("ck_conversation_turns_no_words");
        assertThat(withCitation).hasMessageContaining("ck_conversation_turns_no_words");
        assertThat(noneWithModelSource).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_conversation_turns_none_reason");
    }

    @Test
    void givenV9_whenInsertingOlderValidCombinations_thenStillAccepted() {
        // Arrange
        migrateTo("9");

        // Act
        Throwable smallTalk = insertTurn("NONE", false, "SMALL_TALK", "GREETING", 300);
        Throwable belowThreshold = insertTurn("RERANK", true, "BELOW_RERANK_THRESHOLD", "OTHER", 700);
        Throwable emptyKnowledgeBase = insertTurn("RERANK", true, "EMPTY_KNOWLEDGE_BASE", null, null);
        Throwable unknownSource = insertTurn("RERANK", true, "WHATEVER", null, null);

        // Assert
        assertThat(smallTalk).isNull();
        assertThat(belowThreshold).isNull();
        assertThat(emptyKnowledgeBase).isNull();
        assertThat(unknownSource).hasMessageContaining("ck_conversation_turns_answer_source");
    }
}
