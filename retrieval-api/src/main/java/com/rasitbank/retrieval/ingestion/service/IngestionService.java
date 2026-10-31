package com.rasitbank.retrieval.ingestion.service;

import com.rasitbank.retrieval.config.EmbeddingProperties;
import com.rasitbank.retrieval.exception.ConflictException;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJobFile;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobFileRepository;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobRepository;
import com.rasitbank.retrieval.source.domain.dto.SourceUpload;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Yüklenen belgeden kaynak ve ilk içe alma işini oluşturur, başarısız içe almayı yeniden başlatır, işlerin durumunu
 * sunar ve servis yeniden başladığında yarıda kalan işleri kapatır.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private static final String INGESTION_INTERRUPTED = "INGESTION_INTERRUPTED";
    private static final String SOURCE_NAME_TAKEN = "SOURCE_NAME_TAKEN";
    private static final String INGESTION_IN_PROGRESS = "INGESTION_IN_PROGRESS";

    private final WorkspaceService workspaceService;
    private final SourceRepository sourceRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final IngestionJobFileRepository ingestionJobFileRepository;
    private final IngestionJobRunner ingestionJobRunner;
    private final EmbeddingProperties embeddingProperties;
    private final Clock clock;

    public IngestionService(WorkspaceService workspaceService, SourceRepository sourceRepository,
            IngestionJobRepository ingestionJobRepository, IngestionJobFileRepository ingestionJobFileRepository,
            IngestionJobRunner ingestionJobRunner, EmbeddingProperties embeddingProperties, Clock clock) {
        this.workspaceService = workspaceService;
        this.sourceRepository = sourceRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.ingestionJobFileRepository = ingestionJobFileRepository;
        this.ingestionJobRunner = ingestionJobRunner;
        this.embeddingProperties = embeddingProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public IngestionJobResponse getIngestionJob(long workspaceId, long jobId) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        return ingestionJobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .map(IngestionJobResponse::from)
                .orElseThrow(() -> new NotFoundException("INGESTION_JOB_NOT_FOUND",
                        "Ingestion job not found: " + jobId));
    }

    @Transactional
    public IngestionJobResponse createSource(long workspaceId, SourceUpload upload) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        workspaceService.ensureEmbeddingModelMatches(workspaceId, embeddingProperties.modelLabel());
        if (sourceRepository.findByLowerName(workspaceId, upload.name()).isPresent()) {
            throw sourceNameTaken();
        }
        Instant now = Instant.now(clock);
        Source source;
        try {
            source = sourceRepository.saveAndFlush(Source.create(workspaceId, upload.name(), SourceKind.PDF, now));
        } catch (DataIntegrityViolationException exception) {
            throw sourceNameTaken();
        }
        log.info("Source created sourceId={} workspaceId={}", source.getId(), workspaceId);
        IngestionJob job = queueJob(IngestionJob.queued(workspaceId, source.getId(), upload.version(),
                upload.fileName(), upload.content().length, now), upload.content());
        log.info("Ingestion job queued jobId={} sourceId={} fileSizeBytes={}", job.getId(), source.getId(),
                upload.content().length);
        return IngestionJobResponse.from(job);
    }

    @Transactional
    public IngestionJobResponse retryIngestion(long workspaceId, long sourceId) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        Source source = sourceRepository.findByIdAndWorkspaceId(sourceId, workspaceId)
                .orElseThrow(() -> new NotFoundException("SOURCE_NOT_FOUND", "Source not found: " + sourceId));
        workspaceService.ensureEmbeddingModelMatches(workspaceId, embeddingProperties.modelLabel());
        if (source.hasActiveVersion()) {
            throw new ConflictException("SOURCE_ALREADY_INGESTED", "Source already has a completed version: "
                    + sourceId);
        }
        if (ingestionJobRepository.existsBySourceIdAndStageIn(sourceId, IngestionStage.activeStages())) {
            throw ingestionInProgress(sourceId);
        }
        IngestionJob previousJob = ingestionJobRepository.findFirstBySourceIdOrderByIdDesc(sourceId)
                .orElseThrow(() -> new IllegalStateException("Source has no ingestion job: " + sourceId));
        byte[] content = ingestionJobFileRepository.findById(previousJob.getId())
                .orElseThrow(() -> new IllegalStateException("Ingestion job has no file: " + previousJob.getId()))
                .getContent();
        IngestionJob job;
        try {
            job = queueJob(IngestionJob.queued(workspaceId, sourceId, previousJob.getVersion(),
                    previousJob.getFileName(), previousJob.getFileSizeBytes(), Instant.now(clock)), content);
        } catch (DataIntegrityViolationException exception) {
            throw ingestionInProgress(sourceId);
        }
        log.info("Ingestion retry queued jobId={} sourceId={} previousJobId={}", job.getId(), sourceId,
                previousJob.getId());
        return IngestionJobResponse.from(job);
    }

    @Transactional
    public int failInterruptedJobs() {
        List<IngestionJob> interruptedJobs = ingestionJobRepository.findByStageIn(IngestionStage.activeStages());
        Instant now = Instant.now(clock);
        interruptedJobs.forEach(job -> job.fail(INGESTION_INTERRUPTED, now));
        log.info("Interrupted ingestion jobs marked failed count={}", interruptedJobs.size());
        return interruptedJobs.size();
    }

    private IngestionJob queueJob(IngestionJob queuedJob, byte[] content) {
        IngestionJob job = ingestionJobRepository.saveAndFlush(queuedJob);
        ingestionJobFileRepository.save(IngestionJobFile.of(job.getId(), content));
        submitAfterCommit(job.getId(), content);
        return job;
    }

    private static ConflictException sourceNameTaken() {
        return new ConflictException(SOURCE_NAME_TAKEN, "A source with this name already exists");
    }

    private static ConflictException ingestionInProgress(long sourceId) {
        return new ConflictException(INGESTION_IN_PROGRESS, "Source has an active ingestion job: " + sourceId);
    }

    private void submitAfterCommit(long jobId, byte[] content) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ingestionJobRunner.submit(jobId, content);
            }
        });
    }
}
