package com.rasitbank.retrieval.fixture;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Arama testleri için SQL ile kaynak, iş, sürüm ve elle verilmiş vektörlü parça yazar; vektörler birim eksenlerden kurulur.
 */
public final class SearchFixtures {

    public static final String MODEL = "bge-m3@1024";
    private static final int DIMENSIONS = 1024;

    private SearchFixtures() {
    }

    public static float[] e0() {
        return vector(1f, 0f);
    }

    public static float[] e1() {
        return vector(0f, 1f);
    }

    public static float[] mixed() {
        return vector(0.8f, 0.6f);
    }

    public static float[] vector(float first, float second) {
        float[] vector = new float[DIMENSIONS];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }

    public static String literal(float[] vector) {
        StringBuilder text = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            text.append(index > 0 ? "," : "").append(vector[index]);
        }
        return text.append(']').toString();
    }

    public static void clean(JdbcTemplate jdbc) {
        jdbc.execute("update sources set active_version_id = null");
        jdbc.execute("delete from chunks");
        jdbc.execute("delete from source_versions");
        jdbc.execute("delete from ingestion_job_files");
        jdbc.execute("delete from ingestion_jobs");
        jdbc.execute("delete from sources");
    }

    public static void insertSource(JdbcTemplate jdbc, long sourceId, String name) {
        jdbc.update("insert into sources (id, workspace_id, name, kind, created_at) values (?, 42, ?, 'PDF', ?)",
                sourceId, name, Timestamp.from(Instant.now()));
    }

    public static long insertVersion(JdbcTemplate jdbc, long sourceId, String version, boolean active) {
        Long jobId = jdbc.queryForObject("""
                insert into ingestion_jobs (workspace_id, source_id, version, file_name, file_size_bytes, stage,
                    progress, embedded_chunks, created_at)
                values (42, ?, ?, 'f.pdf', 1, 'DONE', 1, 1, ?) returning id
                """, Long.class, sourceId, version, Timestamp.from(Instant.now()));
        Long versionId = jdbc.queryForObject("""
                insert into source_versions (source_id, ingestion_job_id, version, file_name, file_size_bytes,
                    page_count, chapter_count, chunk_count, synced_at, created_at)
                values (?, ?, ?, 'f.pdf', 1, 1, 1, 1, ?, ?) returning id
                """, Long.class, sourceId, jobId, version, Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()));
        if (active) {
            jdbc.update("update sources set active_version_id = ? where id = ?", versionId, sourceId);
        }
        return versionId;
    }

    public static void insertChunk(JdbcTemplate jdbc, long chunkId, long sourceId, long versionId, int chunkNo,
            float[] embedding) {
        insertChunk(jdbc, chunkId, sourceId, versionId, chunkNo, embedding, MODEL);
    }

    public static void insertChunk(JdbcTemplate jdbc, long chunkId, long sourceId, long versionId, int chunkNo,
            float[] embedding, String embeddingModel) {
        jdbc.update("""
                insert into chunks (id, workspace_id, source_id, source_version_id, chunk_no, page_number, heading,
                    content, token_count, embedding, embedding_model, created_at)
                values (?, 42, ?, ?, ?, ?, ?, ?, 5, cast(? as vector), ?, ?)
                """, chunkId, sourceId, versionId, chunkNo, chunkNo + 3, "Başlık " + chunkId, "İçerik " + chunkId,
                literal(embedding), embeddingModel, Timestamp.from(Instant.now()));
    }
}
