package com.rasitbank.retrieval.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobFileRepository;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobRepository;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

/**
 * İş okuma ve kuyruğa alma servisinin çalışma alanı ve iş varlık kontrollerini doğrular.
 */
class IngestionServiceTest {

    private final WorkspaceService workspaceService = mock(WorkspaceService.class);
    private final SourceRepository sourceRepository = mock(SourceRepository.class);
    private final IngestionJobRepository jobRepository = mock(IngestionJobRepository.class);
    private final IngestionJobFileRepository fileRepository = mock(IngestionJobFileRepository.class);
    private final IngestionJobRunner runner = mock(IngestionJobRunner.class);
    private final IngestionService service = new IngestionService(workspaceService, sourceRepository, jobRepository,
            fileRepository, runner, Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void givenExistingJob_whenGetIngestionJob_thenReturnsResponseWithStatusUrl() {
        IngestionJob job = IngestionJob.queued(42, 10001, "2026.10", "r.pdf", 10, Instant.EPOCH);
        org.springframework.test.util.ReflectionTestUtils.setField(job, "id", 5L);
        given(jobRepository.findByIdAndWorkspaceId(5, 42)).willReturn(Optional.of(job));

        IngestionJobResponse response = service.getIngestionJob(42, 5);

        assertThat(response.jobId()).isEqualTo(5);
        assertThat(response.statusUrl()).isEqualTo("/api/workspaces/42/ingestion-jobs/5");
        assertThat(response.stage().name()).isEqualTo("QUEUED");
    }

    @Test
    void givenUnknownJob_whenGetIngestionJob_thenThrowsIngestionJobNotFound() {
        given(jobRepository.findByIdAndWorkspaceId(99, 42)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getIngestionJob(42, 99)).isInstanceOfSatisfying(NotFoundException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("INGESTION_JOB_NOT_FOUND"));
    }

    @Test
    void givenUnknownWorkspace_whenGetIngestionJob_thenThrowsWorkspaceNotFoundWithoutReadingJobs() {
        willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok")).given(workspaceService)
                .ensureWorkspaceExists(43);

        assertThatThrownBy(() -> service.getIngestionJob(43, 1)).isInstanceOfSatisfying(NotFoundException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("WORKSPACE_NOT_FOUND"));
        verifyNoInteractions(jobRepository);
    }

    @Test
    void givenUnknownWorkspace_whenQueueIngestion_thenThrowsAndWritesNothing() {
        willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok")).given(workspaceService)
                .ensureWorkspaceExists(43);

        assertThatThrownBy(() -> service.queueIngestion(43, "Rehber", "1", "r.pdf", new byte[] {1}))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(sourceRepository, jobRepository, fileRepository, runner);
    }
}
