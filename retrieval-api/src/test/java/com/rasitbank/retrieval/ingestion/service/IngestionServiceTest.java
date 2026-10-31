package com.rasitbank.retrieval.ingestion.service;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.embeddingProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.retrieval.exception.ConflictException;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJobFile;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobFileRepository;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobRepository;
import com.rasitbank.retrieval.source.domain.dto.SourceUpload;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * İş okuma, belge ekleme ve yeniden deneme servisinin kontrol sırasını, çakışma kodlarını ve commit sonrası başlatmayı
 * doğrular.
 */
class IngestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    private static final byte[] CONTENT = {1, 2, 3, 4};

    private final WorkspaceService workspaceService = mock(WorkspaceService.class);
    private final SourceRepository sourceRepository = mock(SourceRepository.class);
    private final IngestionJobRepository jobRepository = mock(IngestionJobRepository.class);
    private final IngestionJobFileRepository fileRepository = mock(IngestionJobFileRepository.class);
    private final IngestionJobRunner runner = mock(IngestionJobRunner.class);
    private final IngestionService service = new IngestionService(workspaceService, sourceRepository, jobRepository,
            fileRepository, runner, embeddingProperties("bge-m3", 1024, Duration.ZERO, 2),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void startTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private static SourceUpload upload() {
        return new SourceUpload("Rehber", "2026.10", "rehber.pdf", CONTENT);
    }

    private static Source sourceWithId(long id) {
        Source source = Source.create(42, "Rehber", SourceKind.PDF, NOW);
        ReflectionTestUtils.setField(source, "id", id);
        return source;
    }

    private static IngestionJob jobWithId(IngestionJob job, long id) {
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    private static void runAfterCommit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void givenExistingJob_whenGetIngestionJob_thenReturnsResponseWithStatusUrl() {
        IngestionJob job = jobWithId(IngestionJob.queued(42, 10001, "2026.10", "r.pdf", 10, Instant.EPOCH), 5);
        given(jobRepository.findByIdAndWorkspaceId(5, 42)).willReturn(Optional.of(job));

        IngestionJobResponse response = service.getIngestionJob(42, 5);

        assertThat(response.jobId()).isEqualTo(5);
        assertThat(response.statusUrl()).isEqualTo("/api/workspaces/42/ingestion-jobs/5");
        assertThat(response.stage().name()).isEqualTo("QUEUED");
    }

    @Test
    void givenUnknownJob_whenGetIngestionJob_thenThrowsIngestionJobNotFound() {
        given(jobRepository.findByIdAndWorkspaceId(99, 42)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.getIngestionJob(42, 99));

        assertThat(thrown).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("INGESTION_JOB_NOT_FOUND");
    }

    @Test
    void givenUnknownWorkspace_whenGetIngestionJob_thenThrowsWorkspaceNotFoundWithoutReadingJobs() {
        willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok")).given(workspaceService)
                .ensureWorkspaceExists(43);

        Throwable thrown = catchThrowable(() -> service.getIngestionJob(43, 1));

        assertThat(thrown).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
        verifyNoInteractions(jobRepository);
    }

    @Test
    void givenUnknownWorkspace_whenCreateSource_thenThrowsAndWritesNothing() {
        willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok")).given(workspaceService)
                .ensureWorkspaceExists(43);

        Throwable thrown = catchThrowable(() -> service.createSource(43, upload()));

        assertThat(thrown).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
        verifyNoInteractions(sourceRepository, jobRepository, fileRepository, runner);
    }

    @Test
    void givenValidUpload_whenCreateSource_thenSavesSourceJobAndFileAndStartsRunnerOnlyAfterCommit() {
        // Arrange
        given(sourceRepository.findByLowerName(42, "Rehber")).willReturn(Optional.empty());
        given(sourceRepository.saveAndFlush(any(Source.class))).willReturn(sourceWithId(10001));
        given(jobRepository.saveAndFlush(any(IngestionJob.class)))
                .willAnswer(invocation -> jobWithId(invocation.getArgument(0), 7));

        // Act
        IngestionJobResponse response = service.createSource(42, upload());

        // Assert
        assertThat(response.jobId()).isEqualTo(7);
        assertThat(response.sourceId()).isEqualTo(10001);
        assertThat(response.version()).isEqualTo("2026.10");
        assertThat(response.fileName()).isEqualTo("rehber.pdf");
        assertThat(response.stage().name()).isEqualTo("QUEUED");
        assertThat(response.statusUrl()).isEqualTo("/api/workspaces/42/ingestion-jobs/7");
        ArgumentCaptor<IngestionJobFile> file = ArgumentCaptor.captor();
        then(fileRepository).should().save(file.capture());
        assertThat(file.getValue().getIngestionJobId()).isEqualTo(7);
        assertThat(file.getValue().getContent()).isEqualTo(CONTENT);
        then(runner).shouldHaveNoInteractions();
        runAfterCommit();
        then(runner).should().submit(7, CONTENT);
    }

    @Test
    void givenSourceNameAlreadyExists_whenCreateSource_thenThrowsSourceNameTakenAndWritesNothing() {
        given(sourceRepository.findByLowerName(42, "Rehber")).willReturn(Optional.of(sourceWithId(10001)));

        Throwable thrown = catchThrowable(() -> service.createSource(42, upload()));

        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("SOURCE_NAME_TAKEN");
        then(sourceRepository).should(never()).saveAndFlush(any());
        verifyNoInteractions(jobRepository, fileRepository, runner);
    }

    @Test
    void givenUniqueIndexRejectsSource_whenCreateSource_thenThrowsSourceNameTaken() {
        // Arrange
        given(sourceRepository.findByLowerName(42, "Rehber")).willReturn(Optional.empty());
        given(sourceRepository.saveAndFlush(any(Source.class))).willThrow(new DataIntegrityViolationException("dup"));

        // Act
        Throwable thrown = catchThrowable(() -> service.createSource(42, upload()));

        // Assert
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("SOURCE_NAME_TAKEN");
        runAfterCommit();
        verifyNoInteractions(runner, jobRepository);
    }

    @Test
    void givenConfiguredModelDiffers_whenCreateSource_thenThrowsEmbeddingModelMismatchAndSavesNothing() {
        // Arrange
        willThrow(new ConflictException("EMBEDDING_MODEL_MISMATCH", "differs")).given(workspaceService)
                .ensureEmbeddingModelMatches(42, "bge-m3@1024");

        // Act
        Throwable thrown = catchThrowable(() -> service.createSource(42, upload()));

        // Assert
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("EMBEDDING_MODEL_MISMATCH");
        verifyNoInteractions(sourceRepository, jobRepository, fileRepository, runner);
    }

    @Test
    void givenUnknownWorkspace_whenRetryIngestion_thenThrowsWorkspaceNotFoundAndWritesNothing() {
        // Arrange
        willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok")).given(workspaceService)
                .ensureWorkspaceExists(43);

        // Act
        Throwable thrown = catchThrowable(() -> service.retryIngestion(43, 7));

        // Assert
        assertThat(thrown).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
        verifyNoInteractions(sourceRepository, jobRepository, fileRepository, runner);
    }

    @Test
    void givenUnknownSource_whenRetryIngestion_thenThrowsSourceNotFound() {
        given(sourceRepository.findByIdAndWorkspaceId(7, 42)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.retryIngestion(42, 7));

        assertThat(thrown).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("SOURCE_NOT_FOUND");
        verifyNoInteractions(jobRepository, runner);
    }

    @Test
    void givenSourceWithActiveVersion_whenRetryIngestion_thenThrowsSourceAlreadyIngested() {
        Source source = sourceWithId(7);
        source.activate(1L);
        given(sourceRepository.findByIdAndWorkspaceId(7, 42)).willReturn(Optional.of(source));

        Throwable thrown = catchThrowable(() -> service.retryIngestion(42, 7));

        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("SOURCE_ALREADY_INGESTED");
        then(jobRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void givenActiveJob_whenRetryIngestion_thenThrowsIngestionInProgress() {
        given(sourceRepository.findByIdAndWorkspaceId(7, 42)).willReturn(Optional.of(sourceWithId(7)));
        given(jobRepository.existsBySourceIdAndStageIn(eq(7L), anyCollection())).willReturn(true);

        Throwable thrown = catchThrowable(() -> service.retryIngestion(42, 7));

        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("INGESTION_IN_PROGRESS");
        then(jobRepository).should(never()).saveAndFlush(any());
        verifyNoInteractions(runner);
    }

    @Test
    void givenConfiguredModelDiffers_whenRetryIngestion_thenThrowsEmbeddingModelMismatchAndSavesNothing() {
        given(sourceRepository.findByIdAndWorkspaceId(7, 42)).willReturn(Optional.of(sourceWithId(7)));
        willThrow(new ConflictException("EMBEDDING_MODEL_MISMATCH", "differs")).given(workspaceService)
                .ensureEmbeddingModelMatches(42, "bge-m3@1024");

        Throwable thrown = catchThrowable(() -> service.retryIngestion(42, 7));

        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("EMBEDDING_MODEL_MISMATCH");
        then(jobRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void givenFailedFirstJob_whenRetryIngestion_thenNewJobCopiesVersionFileNameAndBytesAndStartsAfterCommit() {
        // Arrange
        given(sourceRepository.findByIdAndWorkspaceId(7, 42)).willReturn(Optional.of(sourceWithId(7)));
        IngestionJob previous = jobWithId(IngestionJob.queued(42, 7, "2026.10", "rehber.pdf", 4, Instant.EPOCH), 3);
        given(jobRepository.findFirstBySourceIdOrderByIdDesc(7)).willReturn(Optional.of(previous));
        given(fileRepository.findById(3L)).willReturn(Optional.of(IngestionJobFile.of(3, CONTENT)));
        given(jobRepository.saveAndFlush(any(IngestionJob.class)))
                .willAnswer(invocation -> jobWithId(invocation.getArgument(0), 8));

        // Act
        IngestionJobResponse response = service.retryIngestion(42, 7);

        // Assert
        assertThat(response.jobId()).isEqualTo(8);
        assertThat(response.sourceId()).isEqualTo(7);
        assertThat(response.version()).isEqualTo("2026.10");
        assertThat(response.fileName()).isEqualTo("rehber.pdf");
        ArgumentCaptor<IngestionJobFile> file = ArgumentCaptor.captor();
        then(fileRepository).should().save(file.capture());
        assertThat(file.getValue().getIngestionJobId()).isEqualTo(8);
        assertThat(file.getValue().getContent()).isEqualTo(CONTENT);
        then(runner).shouldHaveNoInteractions();
        runAfterCommit();
        then(runner).should().submit(8, CONTENT);
    }

    @Test
    void givenActiveJobIndexRejectsNewJob_whenRetryIngestion_thenThrowsIngestionInProgress() {
        // Arrange
        given(sourceRepository.findByIdAndWorkspaceId(7, 42)).willReturn(Optional.of(sourceWithId(7)));
        IngestionJob previous = jobWithId(IngestionJob.queued(42, 7, "2026.10", "rehber.pdf", 4, Instant.EPOCH), 3);
        given(jobRepository.findFirstBySourceIdOrderByIdDesc(7)).willReturn(Optional.of(previous));
        given(fileRepository.findById(3L)).willReturn(Optional.of(IngestionJobFile.of(3, CONTENT)));
        given(jobRepository.saveAndFlush(any(IngestionJob.class))).willThrow(new DataIntegrityViolationException("dup"));

        // Act
        Throwable thrown = catchThrowable(() -> service.retryIngestion(42, 7));

        // Assert
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("INGESTION_IN_PROGRESS");
        runAfterCommit();
        verifyNoInteractions(runner);
    }
}
