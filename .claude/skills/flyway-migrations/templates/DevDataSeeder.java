package com.rasitbank.retrieval.config;

import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.workspace.domain.entity.Workspace;
import com.rasitbank.retrieval.workspace.repository.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Sadece dev profilinde, boş veritabanına örnek kaynak kaydı ekler; tek çalışma alanı (42) migration ile gelir.
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final long WORKSPACE_ID = 42L;

    private final WorkspaceRepository workspaceRepository;
    private final SourceRepository sourceRepository;
    private final Clock clock;

    public DevDataSeeder(WorkspaceRepository workspaceRepository, SourceRepository sourceRepository, Clock clock) {
        this.workspaceRepository = workspaceRepository;
        this.sourceRepository = sourceRepository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (sourceRepository.count() > 0) {
            log.info("Dev data already present, skipping seed");
            return;
        }

        Instant now = Instant.now(clock);
        Workspace workspace = workspaceRepository.findById(WORKSPACE_ID)
                .orElseThrow(() -> new IllegalStateException("Seed workspace missing: " + WORKSPACE_ID));
        sourceRepository.save(Source.ingesting(workspace, sourceRepository.nextSourceId(),
                "Raşit Bankası Ürün ve Hizmet Rehberi", SourceKind.PDF, now));

        log.info("Dev data seeded sources={}", 1);
    }
}
