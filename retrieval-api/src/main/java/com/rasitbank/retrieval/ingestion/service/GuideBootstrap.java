package com.rasitbank.retrieval.ingestion.service;

import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobRepository;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Bilgi bankasında rehberin tamamlanmış sürümü yoksa yapılandırılmış PDF'ten içe alma işini başlatır.
 */
@Service
public class GuideBootstrap {

    public static final long WORKSPACE_ID = 42L;

    private static final Logger log = LoggerFactory.getLogger(GuideBootstrap.class);

    private final IngestionProperties.Bootstrap properties;
    private final SourceRepository sourceRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final IngestionService ingestionService;

    public GuideBootstrap(IngestionProperties ingestionProperties, SourceRepository sourceRepository,
            IngestionJobRepository ingestionJobRepository, IngestionService ingestionService) {
        this.properties = ingestionProperties.bootstrap();
        this.sourceRepository = sourceRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.ingestionService = ingestionService;
    }

    public GuideBootstrapResult bootstrapGuide() {
        if (!properties.enabled()) {
            return logged(GuideBootstrapResult.DISABLED, null, null);
        }
        Optional<Source> existingSource = sourceRepository.findByWorkspaceIdAndName(WORKSPACE_ID,
                properties.sourceName());
        if (existingSource.isPresent()) {
            Source source = existingSource.get();
            if (source.hasActiveVersion()) {
                return logged(GuideBootstrapResult.ALREADY_INGESTED, source.getId(), null);
            }
            if (ingestionJobRepository.existsBySourceIdAndStageIn(source.getId(), IngestionStage.activeStages())) {
                return logged(GuideBootstrapResult.JOB_ACTIVE, source.getId(), null);
            }
        }
        byte[] content = readFile(properties.file());
        if (content == null) {
            return logged(GuideBootstrapResult.FILE_MISSING, existingSource.map(Source::getId).orElse(null), null);
        }
        IngestionJobResponse job = ingestionService.queueIngestion(WORKSPACE_ID, properties.sourceName(),
                properties.version(), properties.file().getFileName().toString(), content);
        return logged(GuideBootstrapResult.JOB_QUEUED, job.sourceId(), job.jobId());
    }

    private static byte @Nullable [] readFile(Path file) {
        try {
            byte[] content = Files.readAllBytes(file);
            if (content.length == 0) {
                log.error("Guide bootstrap file not readable path={} error={}", file, "EmptyFile");
                return null;
            }
            return content;
        } catch (IOException | SecurityException exception) {
            log.error("Guide bootstrap file not readable path={} error={}", file, exception.getClass().getSimpleName());
            return null;
        }
    }

    private static GuideBootstrapResult logged(GuideBootstrapResult result, @Nullable Long sourceId,
            @Nullable Long jobId) {
        log.info("Guide bootstrap result={} sourceId={} jobId={}", result, sourceId, jobId);
        return result;
    }
}
