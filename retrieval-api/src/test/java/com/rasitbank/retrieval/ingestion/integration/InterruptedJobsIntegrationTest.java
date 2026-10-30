package com.rasitbank.retrieval.ingestion.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Servis yeniden başlarken yarıda kalan işlerin INGESTION_INTERRUPTED ile kapatıldığını doğrular.
 */
class InterruptedJobsIntegrationTest extends AbstractIngestionIntegrationTest {

    @Test
    void givenJobsInEmbeddingAndDone_whenFailInterruptedJobs_thenActiveOneFailedInterruptedAndDoneUntouched() {
        long sourceA = sourceRepository.save(Source.create(42, "Kaynak A", SourceKind.PDF, Instant.now())).getId();
        long sourceB = sourceRepository.save(Source.create(42, "Kaynak B", SourceKind.PDF, Instant.now())).getId();
        IngestionJob embedding = IngestionJob.queued(42, sourceA, "1", "a.pdf", 10, Instant.now());
        embedding.start(Instant.now());
        embedding.moveTo(IngestionStage.CHUNKING);
        embedding.recordTotalChunks(10);
        embedding.moveTo(IngestionStage.EMBEDDING);
        IngestionJob done = IngestionJob.queued(42, sourceB, "1", "b.pdf", 10, Instant.now());
        done.start(Instant.now());
        done.moveTo(IngestionStage.CHUNKING);
        done.recordTotalChunks(1);
        done.moveTo(IngestionStage.EMBEDDING);
        done.moveTo(IngestionStage.WRITING);
        done.complete(Instant.now());
        long embeddingId = ingestionJobRepository.saveAndFlush(embedding).getId();
        long doneId = ingestionJobRepository.saveAndFlush(done).getId();

        int count = ingestionService.failInterruptedJobs();

        assertThat(count).isEqualTo(1);
        Map<String, Object> failed = jdbc.queryForMap(
                "select stage, failed_stage, error_code, finished_at from ingestion_jobs where id = ?", embeddingId);
        assertThat(failed).containsEntry("stage", "FAILED").containsEntry("failed_stage", "EMBEDDING")
                .containsEntry("error_code", "INGESTION_INTERRUPTED");
        assertThat(failed.get("finished_at")).isNotNull();
        Map<String, Object> untouched = jdbc.queryForMap(
                "select stage, failed_stage, error_code from ingestion_jobs where id = ?", doneId);
        assertThat(untouched).containsEntry("stage", "DONE").containsEntry("failed_stage", null)
                .containsEntry("error_code", null);
    }

    @Test
    void givenNoActiveJobs_whenFailInterruptedJobs_thenReturnsZero() {
        assertThat(ingestionService.failInterruptedJobs()).isZero();
    }
}
