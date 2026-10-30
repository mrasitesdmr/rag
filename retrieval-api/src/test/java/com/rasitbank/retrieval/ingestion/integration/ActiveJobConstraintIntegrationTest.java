package com.rasitbank.retrieval.ingestion.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Bir kaynağın aynı anda tek aktif işi olabileceğini veritabanı kısıtıyla doğrular.
 */
class ActiveJobConstraintIntegrationTest extends AbstractIngestionIntegrationTest {

    private static IngestionJob job(long sourceId) {
        return IngestionJob.queued(42, sourceId, "2026.10", "r.pdf", 10, Instant.now());
    }

    @Test
    void givenActiveJobForSource_whenSecondActiveJobSaved_thenUniqueIndexRejects() {
        long sourceId = sourceRepository.save(Source.create(42, "Kaynak A", SourceKind.PDF, Instant.now())).getId();
        ingestionJobRepository.saveAndFlush(job(sourceId));

        assertThatThrownBy(() -> ingestionJobRepository.saveAndFlush(job(sourceId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenFailedJobForSource_whenNewQueuedJobSaved_thenAccepted() {
        long sourceId = sourceRepository.save(Source.create(42, "Kaynak B", SourceKind.PDF, Instant.now())).getId();
        IngestionJob first = job(sourceId);
        first.fail("PDF_UNREADABLE", Instant.now());
        ingestionJobRepository.saveAndFlush(first);

        ingestionJobRepository.saveAndFlush(job(sourceId));

        assertThat(count("ingestion_jobs")).isEqualTo(2);
    }
}
