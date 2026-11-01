package com.rasitbank.retrieval.search.repository;

import com.rasitbank.retrieval.embedding.client.EmbeddingClient;
import com.rasitbank.retrieval.fixture.SearchFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Gerçek pgvector veritabanı ve sahte embedding istemcisiyle arama entegrasyon testlerinin ortak kurulumudur.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractSearchIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:0.8.1-pg18");

    static {
        POSTGRES.start();
    }

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    EmbeddingClient embeddingClient;

    @BeforeEach
    void cleanDatabase() {
        SearchFixtures.clean(jdbc);
    }
}
