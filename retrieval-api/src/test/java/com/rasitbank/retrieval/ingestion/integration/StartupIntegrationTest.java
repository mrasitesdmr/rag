package com.rasitbank.retrieval.ingestion.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Temiz veritabanıyla açılan servisin hiçbir kaynak, sürüm, parça ya da iş oluşturmadığını doğrular.
 */
@Testcontainers
@SpringBootTest
class StartupIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("pgvector/pgvector:0.8.1-pg18");

    @Autowired
    private JdbcTemplate jdbc;

    private long count(String table) {
        Long count = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0 : count;
    }

    @Test
    void givenFreshDatabase_whenApplicationStarted_thenNoSourceVersionChunkOrJobExists() {
        assertThat(count("sources")).isZero();
        assertThat(count("source_versions")).isZero();
        assertThat(count("chunks")).isZero();
        assertThat(count("ingestion_jobs")).isZero();
        assertThat(count("workspaces")).isEqualTo(1);
    }
}
