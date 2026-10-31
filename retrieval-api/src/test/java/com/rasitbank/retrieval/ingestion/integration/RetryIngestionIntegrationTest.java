package com.rasitbank.retrieval.ingestion.integration;

import static com.rasitbank.retrieval.fixture.PdfFixtures.validPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.exception.ConflictException;
import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJobFile;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobFileRepository;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Başarısız ya da yarıda kalan ilk içe almanın aynı kaynak üzerinde saklanan PDF'ten yeniden denenmesini doğrular.
 */
class RetryIngestionIntegrationTest extends AbstractIngestionIntegrationTest {

    @Autowired
    private IngestionJobFileRepository fileRepository;

    @Test
    void givenFirstJobFailedEmbeddingUnavailable_whenRetryIngestion_thenSameSourceNewJobDone() {
        // Arrange
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE", "down"));
        IngestionJobResponse first = upload("Rehber", "2026.10", "rehber.pdf", validPdf());
        awaitJobStage(first.jobId(), "FAILED");
        stubEmbeddingSuccess();

        // Act
        IngestionJobResponse second = ingestionService.retryIngestion(42, first.sourceId());
        awaitJobStage(second.jobId(), "DONE");

        // Assert
        assertThat(second.jobId()).isNotEqualTo(first.jobId());
        assertThat(second.sourceId()).isEqualTo(first.sourceId());
        assertThat(count("sources")).isEqualTo(1);
        assertThat(jdbc.queryForList("select stage from ingestion_jobs order by id", String.class))
                .containsExactly("FAILED", "DONE");
        Map<String, Object> firstRow = jdbc.queryForMap("select version, file_name, file_size_bytes"
                + " from ingestion_jobs where id = ?", first.jobId());
        Map<String, Object> secondRow = jdbc.queryForMap("select version, file_name, file_size_bytes"
                + " from ingestion_jobs where id = ?", second.jobId());
        assertThat(secondRow).isEqualTo(firstRow).containsEntry("version", "2026.10");
        assertThat(jdbc.queryForObject("select content from ingestion_job_files where ingestion_job_id = ?",
                byte[].class, second.jobId())).isEqualTo(validPdf());
        assertThat(jdbc.queryForObject("select count(distinct source_version_id) from chunks", Long.class))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("select ingestion_job_id from source_versions", Long.class))
                .isEqualTo(second.jobId());
        assertThat(jdbc.queryForObject("select count(*) from chunks", Long.class)).isPositive();
    }

    @Test
    void givenJobInterrupted_whenRetryIngestion_thenNewJobDone() {
        // Arrange
        Source source = sourceRepository.save(Source.create(42, "Yarıda kalan", SourceKind.PDF, Instant.now()));
        IngestionJob job = IngestionJob.queued(42, source.getId(), "1", "yarida.pdf", validPdf().length, Instant.now());
        job.start(Instant.now());
        job.moveTo(IngestionStage.CHUNKING);
        job.recordTotalChunks(3);
        job.moveTo(IngestionStage.EMBEDDING);
        long jobId = ingestionJobRepository.saveAndFlush(job).getId();
        fileRepository.save(IngestionJobFile.of(jobId, validPdf()));
        ingestionService.failInterruptedJobs();

        // Act
        IngestionJobResponse retried = ingestionService.retryIngestion(42, source.getId());
        awaitJobStage(retried.jobId(), "DONE");

        // Assert
        assertThat(jdbc.queryForObject("select error_code from ingestion_jobs where id = ?", String.class, jobId))
                .isEqualTo("INGESTION_INTERRUPTED");
        assertThat(jdbc.queryForObject("select stage from ingestion_jobs where id = ?", String.class,
                retried.jobId())).isEqualTo("DONE");
        assertThat(count("sources")).isEqualTo(1);
    }

    @Test
    void givenDoneSource_whenRetryIngestion_thenSourceAlreadyIngestedAndNoNewJob() {
        // Arrange
        IngestionJobResponse done = uploadValidPdf("Tamamlanan");
        awaitJobStage(done.jobId(), "DONE");

        // Act
        Throwable thrown = catchThrowable(() -> ingestionService.retryIngestion(42, done.sourceId()));

        // Assert
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("SOURCE_ALREADY_INGESTED");
        assertThat(count("ingestion_jobs")).isEqualTo(1);
    }
}
