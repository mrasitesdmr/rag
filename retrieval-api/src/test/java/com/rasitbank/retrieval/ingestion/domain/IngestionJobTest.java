package com.rasitbank.retrieval.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * İçe alma işinin aşama geçişlerini, ilerleme oranlarını ve hata kaydını doğrular.
 */
class IngestionJobTest {

    private static final Instant T0 = Instant.parse("2026-10-30T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-30T10:05:00Z");

    private static IngestionJob queued() {
        return IngestionJob.queued(42, 10001, "2026.10", "rehber.pdf", 1000, T0);
    }

    @Test
    void givenJobAdvancingThroughStages_whenProgressRead_thenMatchesStageValues() {
        IngestionJob job = queued();
        assertThat(job.getStage()).isEqualTo(IngestionStage.QUEUED);
        assertThat(job.getProgress()).isEqualTo(0.0);

        job.start(T0);
        assertThat(job.getStage()).isEqualTo(IngestionStage.PARSING);
        assertThat(job.getProgress()).isCloseTo(0.05, within(1e-6));
        assertThat(job.getStartedAt()).isEqualTo(T0);

        job.moveTo(IngestionStage.CHUNKING);
        assertThat(job.getProgress()).isCloseTo(0.1, within(1e-6));
        job.recordTotalChunks(150);
        job.moveTo(IngestionStage.EMBEDDING);
        assertThat(job.getProgress()).isCloseTo(0.1, within(1e-6));

        job.recordEmbeddingProgress(96);
        assertThat(job.getProgress()).isCloseTo(0.612, within(1e-6));
        assertThat(job.getEmbeddedChunks()).isEqualTo(96);
        assertThat(job.getTotalChunks()).isEqualTo(150);
        job.recordEmbeddingProgress(150);
        assertThat(job.getProgress()).isCloseTo(0.9, within(1e-6));

        job.moveTo(IngestionStage.WRITING);
        assertThat(job.getProgress()).isCloseTo(0.9, within(1e-6));
        job.complete(T1);
        assertThat(job.getStage()).isEqualTo(IngestionStage.DONE);
        assertThat(job.getProgress()).isCloseTo(1.0, within(1e-6));
        assertThat(job.getFinishedAt()).isEqualTo(T1);
        assertThat(job.getErrorCode()).isNull();
    }

    @Test
    void givenDoneOrFailedJob_whenMoveTo_thenThrowsIllegalState() {
        IngestionJob done = queued();
        done.start(T0);
        done.moveTo(IngestionStage.CHUNKING);
        done.recordTotalChunks(1);
        done.moveTo(IngestionStage.EMBEDDING);
        done.moveTo(IngestionStage.WRITING);
        done.complete(T1);
        IngestionJob failed = queued();
        failed.fail("PDF_UNREADABLE", T1);

        assertThatThrownBy(() -> done.moveTo(IngestionStage.CHUNKING)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failed.moveTo(IngestionStage.CHUNKING)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failed.start(T1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> done.fail("X", T1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenJobInEmbedding_whenFail_thenFailedWithFailedStageEmbeddingAndProgressKept() {
        IngestionJob job = queued();
        job.start(T0);
        job.moveTo(IngestionStage.CHUNKING);
        job.recordTotalChunks(150);
        job.moveTo(IngestionStage.EMBEDDING);
        job.recordEmbeddingProgress(96);

        job.fail("EMBEDDING_UNAVAILABLE", T1);

        assertThat(job.getStage()).isEqualTo(IngestionStage.FAILED);
        assertThat(job.getFailedStage()).isEqualTo(IngestionStage.EMBEDDING);
        assertThat(job.getErrorCode()).isEqualTo("EMBEDDING_UNAVAILABLE");
        assertThat(job.getFinishedAt()).isEqualTo(T1);
        assertThat(job.getProgress()).isCloseTo(0.612, within(1e-6));
        assertThat(job.getEmbeddedChunks()).isEqualTo(96);
    }

    @Test
    void givenJobSkippingAStage_whenMoveTo_thenThrowsIllegalState() {
        IngestionJob job = queued();
        job.start(T0);

        assertThatThrownBy(() -> job.moveTo(IngestionStage.EMBEDDING)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenEmbeddedCountAboveTotal_whenRecordEmbeddingProgress_thenThrowsIllegalArgument() {
        IngestionJob job = queued();
        job.start(T0);
        job.moveTo(IngestionStage.CHUNKING);
        job.recordTotalChunks(10);
        job.moveTo(IngestionStage.EMBEDDING);

        assertThatThrownBy(() -> job.recordEmbeddingProgress(11)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenZeroAndNegativeTotals_whenRecordTotalChunks_thenZeroAcceptedNegativeRejected() {
        IngestionJob job = queued();
        job.start(T0);
        job.moveTo(IngestionStage.CHUNKING);

        job.recordTotalChunks(0);

        assertThat(job.getTotalChunks()).isZero();
        assertThatThrownBy(() -> job.recordTotalChunks(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenTotalOfTen_whenRecordEmbeddingProgressZeroAndTen_thenBoundariesAccepted() {
        IngestionJob job = queued();
        job.start(T0);
        job.moveTo(IngestionStage.CHUNKING);
        job.recordTotalChunks(10);
        job.moveTo(IngestionStage.EMBEDDING);

        job.recordEmbeddingProgress(0);
        assertThat(job.getProgress()).isCloseTo(0.1, within(1e-6));
        job.recordEmbeddingProgress(10);
        assertThat(job.getProgress()).isCloseTo(0.9, within(1e-6));
        assertThatThrownBy(() -> job.recordEmbeddingProgress(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenFailedJob_whenRecordingOrCompleting_thenThrowsIllegalState() {
        IngestionJob failed = queued();
        failed.fail("PDF_UNREADABLE", T1);

        assertThatThrownBy(() -> failed.recordTotalChunks(3)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failed.recordEmbeddingProgress(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failed.complete(T1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenJobStillInEmbedding_whenComplete_thenThrowsIllegalState() {
        IngestionJob job = queued();
        job.start(T0);
        job.moveTo(IngestionStage.CHUNKING);
        job.recordTotalChunks(3);
        job.moveTo(IngestionStage.EMBEDDING);

        assertThatThrownBy(() -> job.complete(T1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenQueuedJob_whenCreated_thenCreatedAtAndFieldsKept() {
        IngestionJob job = queued();

        assertThat(job.getCreatedAt()).isEqualTo(T0);
        assertThat(job.getWorkspaceId()).isEqualTo(42);
        assertThat(job.getSourceId()).isEqualTo(10001);
        assertThat(job.getVersion()).isEqualTo("2026.10");
        assertThat(job.getFileName()).isEqualTo("rehber.pdf");
        assertThat(job.getFileSizeBytes()).isEqualTo(1000);
    }
}
