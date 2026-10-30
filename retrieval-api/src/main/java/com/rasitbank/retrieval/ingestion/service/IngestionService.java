package com.rasitbank.retrieval.ingestion.service;

import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJobFile;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobFileRepository;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobRepository;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * İçe alma işlerini kuyruğa alır, durumlarını sunar ve servis yeniden başladığında yarıda kalanları kapatır.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private static final String INGESTION_INTERRUPTED = "INGESTION_INTERRUPTED";

    private final WorkspaceService workspaceService;
    private final SourceRepository sourceRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final IngestionJobFileRepository ingestionJobFileRepository;
    private final IngestionJobRunner ingestionJobRunner;
    private final Clock clock;

    public IngestionService(WorkspaceService workspaceService, SourceRepository sourceRepository,
            IngestionJobRepository ingestionJobRepository, IngestionJobFileRepository ingestionJobFileRepository,
            IngestionJobRunner ingestionJobRunner, Clock clock) {
        this.workspaceService = workspaceService;
        this.sourceRepository = sourceRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.ingestionJobFileRepository = ingestionJobFileRepository;
        this.ingestionJobRunner = ingestionJobRunner;
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
    public IngestionJobResponse queueIngestion(long workspaceId, String sourceName, String version, String fileName,
            byte[] content) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        Instant now = Instant.now(clock);
        Source source = sourceRepository.findByWorkspaceIdAndName(workspaceId, sourceName)
                .orElseGet(() -> sourceRepository.save(Source.create(workspaceId, sourceName, SourceKind.PDF, now)));
        IngestionJob job = ingestionJobRepository.saveAndFlush(
                IngestionJob.queued(workspaceId, source.getId(), version, fileName, content.length, now));
        long jobId = job.getId();
        ingestionJobFileRepository.save(IngestionJobFile.of(jobId, content));
        log.info("Ingestion job queued jobId={} sourceId={} version={} fileSizeBytes={}", jobId, source.getId(),
                version, content.length);
        submitAfterCommit(jobId, content);
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

    private void submitAfterCommit(long jobId, byte[] content) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ingestionJobRunner.submit(jobId, content);
            }
        });
    }
}
