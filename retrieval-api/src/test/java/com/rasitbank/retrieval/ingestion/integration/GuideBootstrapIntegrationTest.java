package com.rasitbank.retrieval.ingestion.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.rasitbank.retrieval.ingestion.service.GuideBootstrapResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Rehberin gerçek hattan (PDFBox, parçalama, sahte embedding, gerçek pgvector) geçerek içe alındığını doğrular.
 */
@ExtendWith(OutputCaptureExtension.class)
class GuideBootstrapIntegrationTest extends AbstractIngestionIntegrationTest {

    private long runBootstrapToDone() {
        GuideBootstrapResult result = bootstrapFor(GUIDE).bootstrapGuide();
        assertThat(result).isEqualTo(GuideBootstrapResult.JOB_QUEUED);
        long jobId = singleJobId();
        awaitJobStage(jobId, "DONE");
        return jobId;
    }

    @Test
    void givenFreshDatabase_whenBootstrapGuide_thenJobDoneSource10001Version202610AndChunksWith1024Dims()
            throws IOException {
        long jobId = runBootstrapToDone();

        assertThat(jdbc.queryForList("select id, name, kind from sources")).singleElement().satisfies(source -> {
            assertThat(source).containsEntry("id", 10001L).containsEntry("kind", "PDF")
                    .containsEntry("name", "Raşit Bankası Ürün ve Hizmet Rehberi");
        });
        byte[] pdf = Files.readAllBytes(GUIDE);
        Map<String, Object> version = jdbc.queryForMap("select * from source_versions");
        assertThat(version).containsEntry("version", "2026.10").containsEntry("page_count", 70)
                .containsEntry("chapter_count", 11).containsEntry("file_size_bytes", (long) pdf.length)
                .containsEntry("file_name", "Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf")
                .containsEntry("ingestion_job_id", jobId);
        assertThat(version.get("chunk_count")).isEqualTo((int) count("chunks"));
        Map<String, Object> job = jdbc.queryForMap("select stage, progress, embedded_chunks, total_chunks, finished_at"
                + " from ingestion_jobs where id = ?", jobId);
        assertThat(job).containsEntry("stage", "DONE").containsEntry("progress", 1.0);
        assertThat(job.get("embedded_chunks")).isEqualTo(job.get("total_chunks")).isEqualTo(version.get("chunk_count"));
        assertThat(((Timestamp) version.get("synced_at")).toInstant())
                .isEqualTo(((Timestamp) job.get("finished_at")).toInstant());
        byte[] stored = jdbc.queryForObject("select content from ingestion_job_files where ingestion_job_id = ?",
                byte[].class, jobId);
        assertThat(stored).isEqualTo(pdf);
        assertThat(jdbc.queryForObject("select active_version_id = ? from sources where id = 10001", Boolean.class,
                version.get("id"))).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from chunks where vector_dims(embedding) <> 1024",
                Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(distinct embedding_model) from chunks", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select min(embedding_model) from chunks", String.class))
                .isEqualTo("bge-m3@1024");
        assertThat(jdbc.queryForObject("select min(page_number) from chunks", Integer.class)).isGreaterThanOrEqualTo(5);
        assertThat(jdbc.queryForObject("select max(page_number) from chunks", Integer.class)).isLessThanOrEqualTo(70);
        assertThat(jdbc.queryForObject("select count(*) from chunks where heading = '' or content = ''", Long.class))
                .isZero();
        assertThat(jdbc.queryForObject("select max(char_length(content)) from chunks", Integer.class))
                .isLessThanOrEqualTo(2000);
        assertThat(jdbc.queryForObject("select min(chunk_no) from chunks", Integer.class)).isZero();
    }

    @Test
    void givenGuideAlreadyIngested_whenBootstrapAgain_thenAlreadyIngestedAndNoNewRows() {
        runBootstrapToDone();
        long chunks = count("chunks");

        GuideBootstrapResult second = bootstrapFor(GUIDE).bootstrapGuide();

        assertThat(second).isEqualTo(GuideBootstrapResult.ALREADY_INGESTED);
        assertThat(count("sources")).isEqualTo(1);
        assertThat(count("ingestion_jobs")).isEqualTo(1);
        assertThat(count("chunks")).isEqualTo(chunks);
        assertThat(count("source_versions")).isEqualTo(1);
    }

    @Test
    void givenEmbeddingUnavailable_whenBootstrapGuide_thenJobFailedAtEmbeddingAndNothingWritten() {
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE", "down"));

        assertThat(bootstrapFor(GUIDE).bootstrapGuide()).isEqualTo(GuideBootstrapResult.JOB_QUEUED);
        long jobId = singleJobId();
        awaitJobStage(jobId, "FAILED");

        Map<String, Object> job = jdbc.queryForMap(
                "select failed_stage, error_code, finished_at from ingestion_jobs where id = ?", jobId);
        assertThat(job).containsEntry("failed_stage", "EMBEDDING").containsEntry("error_code", "EMBEDDING_UNAVAILABLE");
        assertThat(job.get("finished_at")).isNotNull();
        assertThat(count("chunks")).isZero();
        assertThat(count("source_versions")).isZero();
        assertThat(jdbc.queryForObject("select active_version_id from sources", Long.class)).isNull();
        assertThat(jdbc.queryForObject("select id from sources", Long.class)).isEqualTo(10001L);
    }

    @Test
    void givenEmbeddingReturnsWrongDimension_whenBootstrapGuide_thenJobFailedWithDimensionMismatchAndNothingWritten() {
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_DIMENSION_MISMATCH", "512"));

        bootstrapFor(GUIDE).bootstrapGuide();
        long jobId = singleJobId();
        awaitJobStage(jobId, "FAILED");

        assertThat(jdbc.queryForObject("select error_code from ingestion_jobs where id = ?", String.class, jobId))
                .isEqualTo("EMBEDDING_DIMENSION_MISMATCH");
        assertThat(count("chunks")).isZero();
        assertThat(count("source_versions")).isZero();
    }

    @Test
    void givenPreviousBootstrapFailed_whenBootstrapAgain_thenSameSourceNewJobDone() {
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE", "down"));
        bootstrapFor(GUIDE).bootstrapGuide();
        awaitJobStage(singleJobId(), "FAILED");
        org.mockito.Mockito.reset(embeddingClient);
        given(embeddingClient.embed(anyList())).willAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream().map(input -> new float[1024]).toList();
        });

        runBootstrapToDone();

        assertThat(count("sources")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select id from sources", Long.class)).isEqualTo(10001L);
        List<String> stages = jdbc.queryForList("select stage from ingestion_jobs order by id", String.class);
        assertThat(stages).containsExactly("FAILED", "DONE");
        assertThat(jdbc.queryForObject("select count(distinct source_version_id) from chunks", Long.class))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from source_versions", Long.class)).isEqualTo(1L);
    }

    @Test
    void givenBootstrapFileMissing_whenBootstrapGuide_thenFileMissingAndNothingWritten() {
        GuideBootstrapResult result = bootstrapFor(Path.of("/yok/rehber.pdf")).bootstrapGuide();

        assertThat(result).isEqualTo(GuideBootstrapResult.FILE_MISSING);
        assertThat(count("sources")).isZero();
        assertThat(count("ingestion_jobs")).isZero();
    }

    @Test
    void givenGuideIngested_whenLogsCaptured_thenStageAndCountsLoggedButNoChunkText(CapturedOutput output) {
        runBootstrapToDone();

        assertThat(output.getAll()).contains("Document chunked jobId=").contains("Ingestion job finished jobId=")
                .contains("Ingestion job queued jobId=");
        assertThat(output.getAll()).doesNotContain("Raşit Banka Kartı, vadesiz Türk lirası hesabınıza bağlı")
                .doesNotContain("2.16 Kayıp ya da çalıntı bildirimi").doesNotContain("Kayıp ya da çalıntı");
    }
}
