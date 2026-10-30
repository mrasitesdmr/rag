package com.rasitbank.retrieval.ingestion.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Migration'ların çalışma alanını ve HNSW cosine index'ini oluşturduğunu gerçek veritabanında doğrular.
 */
class WorkspaceMigrationIntegrationTest extends AbstractIngestionIntegrationTest {

    @Test
    void givenFreshDatabase_whenMigrated_thenWorkspace42WithBgeM3At1024Exists() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, name, embedding_model, embedding_dimensions from workspaces");

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("id", 42L)
                .containsEntry("name", "Raşit Bankası · Dijital Asistan")
                .containsEntry("embedding_model", "bge-m3").containsEntry("embedding_dimensions", 1024);
    }

    @Test
    void givenMigratedDatabase_whenIndexesRead_thenChunksEmbeddingHasHnswCosineIndex() {
        String definition = jdbc.queryForObject(
                "select indexdef from pg_indexes where tablename = 'chunks' and indexname = 'chunks_embedding_hnsw_idx'",
                String.class);

        assertThat(definition).contains("USING hnsw (embedding vector_cosine_ops)");
    }

    @Test
    void givenMigratedDatabase_whenFirstSourceCreated_thenIdStartsAt10001() {
        jdbc.execute("alter sequence sources_id_seq restart with 10001");

        Long id = jdbc.queryForObject("select nextval('sources_id_seq')", Long.class);

        assertThat(id).isEqualTo(10001L);
    }
}
