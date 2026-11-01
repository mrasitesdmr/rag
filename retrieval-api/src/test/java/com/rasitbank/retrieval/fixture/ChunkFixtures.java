package com.rasitbank.retrieval.fixture;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Okuma uçlarının testleri için başlığı, metni, sayfası ve token sayısı elle verilmiş parça ile iş dosyası yazar.
 */
public final class ChunkFixtures {

    private ChunkFixtures() {
    }

    public static void insertChunk(JdbcTemplate jdbc, long chunkId, long sourceId, long versionId, int chunkNo,
            int pageNumber, String heading, String content, int tokenCount, float[] embedding) {
        jdbc.update("""
                insert into chunks (id, workspace_id, source_id, source_version_id, chunk_no, page_number, heading,
                    content, token_count, embedding, embedding_model, created_at)
                values (?, 42, ?, ?, ?, ?, ?, ?, ?, cast(? as vector), ?, ?)
                """, chunkId, sourceId, versionId, chunkNo, pageNumber, heading, content, tokenCount,
                SearchFixtures.literal(embedding), SearchFixtures.MODEL, Timestamp.from(Instant.now()));
    }

    public static void insertChunk(JdbcTemplate jdbc, long chunkId, long sourceId, long versionId, int chunkNo,
            String heading, String content) {
        insertChunk(jdbc, chunkId, sourceId, versionId, chunkNo, 1, heading, content, 10, SearchFixtures.e0());
    }

    public static long jobIdOfVersion(JdbcTemplate jdbc, long versionId) {
        Long jobId = jdbc.queryForObject("select ingestion_job_id from source_versions where id = ?", Long.class,
                versionId);
        return jobId == null ? -1 : jobId;
    }

    public static long insertFailedJob(JdbcTemplate jdbc, long sourceId, String version, String fileName,
            long fileSize, byte[] fileContent) {
        Long jobId = jdbc.queryForObject("""
                insert into ingestion_jobs (workspace_id, source_id, version, file_name, file_size_bytes, stage,
                    failed_stage, progress, embedded_chunks, error_code, created_at)
                values (42, ?, ?, ?, ?, 'FAILED', 'EMBEDDING', 0.1, 0, 'EMBEDDING_UNAVAILABLE', ?) returning id
                """, Long.class, sourceId, version, fileName, fileSize, Timestamp.from(Instant.now()));
        jdbc.update("insert into ingestion_job_files (ingestion_job_id, content) values (?, ?)", jobId, fileContent);
        return jobId == null ? -1 : jobId;
    }

    public static void insertFile(JdbcTemplate jdbc, long jobId, byte[] content) {
        jdbc.update("insert into ingestion_job_files (ingestion_job_id, content) values (?, ?)", jobId, content);
    }
}
