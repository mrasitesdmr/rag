package com.rasitbank.retrieval.source.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.source.domain.dto.SourceResponse;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import com.rasitbank.retrieval.source.domain.entity.SourceVersion;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Kaynağın ekran alanlarının güncel sürümden, sürüm yoksa son işten geldiğini ve ortalama token sayısının yarım yukarı
 * yuvarlandığını doğrular.
 */
class SourceResponseTest {

    private static final Instant SYNCED = Instant.parse("2026-10-29T13:15:44Z");

    private static Source source() {
        Source source = Source.create(42, "Rehber", SourceKind.PDF, Instant.parse("2026-10-29T13:00:00Z"));
        ReflectionTestUtils.setField(source, "id", 10001L);
        return source;
    }

    private static IngestionJob job(long id, String version, String fileName, long size) {
        IngestionJob job = IngestionJob.queued(42, 10001, version, fileName, size, Instant.parse("2026-10-29T13:00:00Z"));
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    private static SourceVersion activeVersion() {
        return SourceVersion.of(10001, 1, "2026.10", "surum.pdf", 412873, 70, 11, 154, SYNCED);
    }

    @ParameterizedTest
    @CsvSource({"10.5,11", "10.49,10", "10.0,10", "282.6,283", "0.4,0"})
    void givenAverage_whenOf_thenRoundsHalfUp(double average, int expected) {
        // Act
        SourceResponse response = SourceResponse.of(source(), activeVersion(), average, job(1, "2026.10", "x.pdf", 1));

        // Assert
        assertThat(response.averageTokenCount()).isEqualTo(expected);
    }

    @Test
    void givenActiveVersion_whenOf_thenFileAndCountFieldsComeFromVersionNotJob() {
        // Act
        SourceResponse response = SourceResponse.of(source(), activeVersion(), 283.0,
                job(2, "2026.11", "is-dosyasi.pdf", 5));

        // Assert
        assertThat(response.fileName()).isEqualTo("surum.pdf");
        assertThat(response.fileSizeBytes()).isEqualTo(412873);
        assertThat(response.version()).isEqualTo("2026.10");
        assertThat(response.pageCount()).isEqualTo(70);
        assertThat(response.chapterCount()).isEqualTo(11);
        assertThat(response.chunkCount()).isEqualTo(154);
        assertThat(response.syncedAt()).isEqualTo(SYNCED);
        assertThat(response.latestJob().jobId()).isEqualTo(2);
        assertThat(response.sourceId()).isEqualTo(10001);
        assertThat(response.kind()).isEqualTo(SourceKind.PDF);
    }

    @Test
    void givenSourceWhoseOnlyJobFailed_whenOf_thenFileFieldsFromJobAndCountsNull() {
        // Arrange
        IngestionJob failed = job(5, "3.1", "basarisiz.pdf", 777);
        failed.start(Instant.parse("2026-10-29T13:01:00Z"));
        failed.fail("EMBEDDING_UNAVAILABLE", Instant.parse("2026-10-29T13:02:00Z"));

        // Act
        SourceResponse response = SourceResponse.of(source(), null, null, failed);

        // Assert
        assertThat(response.fileName()).isEqualTo("basarisiz.pdf");
        assertThat(response.fileSizeBytes()).isEqualTo(777);
        assertThat(response.version()).isEqualTo("3.1");
        assertThat(response.pageCount()).isNull();
        assertThat(response.chapterCount()).isNull();
        assertThat(response.chunkCount()).isNull();
        assertThat(response.averageTokenCount()).isNull();
        assertThat(response.syncedAt()).isNull();
        assertThat(response.latestJob().stage()).isEqualTo(IngestionStage.FAILED);
    }

    @Test
    void givenActiveVersionWithoutChunkAverage_whenOf_thenAverageNull() {
        // Act
        SourceResponse response = SourceResponse.of(source(), activeVersion(), null, job(1, "2026.10", "x.pdf", 1));

        // Assert
        assertThat(response.averageTokenCount()).isNull();
    }
}
