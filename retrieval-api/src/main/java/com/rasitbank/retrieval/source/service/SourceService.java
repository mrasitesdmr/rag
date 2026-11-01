package com.rasitbank.retrieval.source.service;

import com.rasitbank.retrieval.chunk.repository.ChunkRepository;
import com.rasitbank.retrieval.chunk.repository.VersionTokenAverage;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobFileRepository;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobRepository;
import com.rasitbank.retrieval.source.domain.dto.SourceFile;
import com.rasitbank.retrieval.source.domain.dto.SourceResponse;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceVersion;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.source.repository.SourceVersionRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışma alanının bilgi kaynaklarını güncel sürümleri, parça sayıları ve son içe alma işleriyle okur; kaynağın
 * saklanan PDF'ini sunar.
 */
@Service
public class SourceService {

    private static final Logger log = LoggerFactory.getLogger(SourceService.class);

    private final WorkspaceService workspaceService;
    private final SourceRepository sourceRepository;
    private final SourceVersionRepository sourceVersionRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final IngestionJobFileRepository ingestionJobFileRepository;
    private final ChunkRepository chunkRepository;

    public SourceService(WorkspaceService workspaceService, SourceRepository sourceRepository,
            SourceVersionRepository sourceVersionRepository, IngestionJobRepository ingestionJobRepository,
            IngestionJobFileRepository ingestionJobFileRepository, ChunkRepository chunkRepository) {
        this.workspaceService = workspaceService;
        this.sourceRepository = sourceRepository;
        this.sourceVersionRepository = sourceVersionRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.ingestionJobFileRepository = ingestionJobFileRepository;
        this.chunkRepository = chunkRepository;
    }

    @Transactional(readOnly = true)
    public Page<SourceResponse> listSources(long workspaceId, int page, int size) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        Page<Source> sources = sourceRepository.findByWorkspaceIdOrderByIdAsc(workspaceId, PageRequest.of(page, size));
        return new PageImpl<>(toResponses(sources.getContent()), sources.getPageable(), sources.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SourceResponse getSource(long workspaceId, long sourceId) {
        return toResponses(List.of(getExistingSource(workspaceId, sourceId))).getFirst();
    }

    @Transactional(readOnly = true)
    public SourceFile getSourceFile(long workspaceId, long sourceId) {
        Source source = getExistingSource(workspaceId, sourceId);
        IngestionJob job = findFileJob(source);
        byte[] content = ingestionJobFileRepository.findById(job.getId())
                .orElseThrow(() -> new IllegalStateException("Ingestion job has no file: " + job.getId()))
                .getContent();
        log.info("Source file served sourceId={} jobId={} fileSizeBytes={}", sourceId, job.getId(), content.length);
        return new SourceFile(job.getFileName(), content);
    }

    private Source getExistingSource(long workspaceId, long sourceId) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        return sourceRepository.findByIdAndWorkspaceId(sourceId, workspaceId)
                .orElseThrow(() -> new NotFoundException("SOURCE_NOT_FOUND", "Source not found: " + sourceId));
    }

    private IngestionJob findFileJob(Source source) {
        Long activeVersionId = source.getActiveVersionId();
        if (activeVersionId != null) {
            long jobId = sourceVersionRepository.findById(activeVersionId)
                    .orElseThrow(() -> new IllegalStateException("Active version not found: " + activeVersionId))
                    .getIngestionJobId();
            return ingestionJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalStateException("Ingestion job not found: " + jobId));
        }
        return ingestionJobRepository.findFirstBySourceIdOrderByIdDesc(source.getId())
                .orElseThrow(() -> new IllegalStateException("Source has no ingestion job: " + source.getId()));
    }

    private List<SourceResponse> toResponses(List<Source> sources) {
        if (sources.isEmpty()) {
            return List.of();
        }
        List<Long> sourceIds = sources.stream().map(Source::getId).toList();
        List<Long> activeVersionIds = sources.stream().map(Source::getActiveVersionId).filter(Objects::nonNull)
                .toList();
        Map<Long, SourceVersion> activeVersions = activeVersionIds.isEmpty() ? Map.of()
                : sourceVersionRepository.findAllById(activeVersionIds).stream()
                        .collect(Collectors.toMap(SourceVersion::getId, Function.identity()));
        Map<Long, Double> averageTokenCounts = activeVersionIds.isEmpty() ? Map.of()
                : chunkRepository.averageTokenCountsByVersionIds(activeVersionIds).stream()
                        .collect(Collectors.toMap(VersionTokenAverage::getSourceVersionId,
                                VersionTokenAverage::getAverageTokenCount));
        Map<Long, IngestionJob> latestJobs = ingestionJobRepository.findLatestBySourceIds(sourceIds).stream()
                .collect(Collectors.toMap(IngestionJob::getSourceId, Function.identity()));
        return sources.stream().map(source -> toResponse(source, activeVersions, averageTokenCounts, latestJobs))
                .toList();
    }

    private static SourceResponse toResponse(Source source, Map<Long, SourceVersion> activeVersions,
            Map<Long, Double> averageTokenCounts, Map<Long, IngestionJob> latestJobs) {
        IngestionJob latestJob = latestJobs.get(source.getId());
        if (latestJob == null) {
            throw new IllegalStateException("Source has no ingestion job: " + source.getId());
        }
        Long activeVersionId = source.getActiveVersionId();
        SourceVersion activeVersion = activeVersionId == null ? null : activeVersions.get(activeVersionId);
        Double averageTokenCount = activeVersionId == null ? null : averageTokenCounts.get(activeVersionId);
        return SourceResponse.of(source, activeVersion, averageTokenCount, latestJob);
    }
}
