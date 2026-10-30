package com.rasitbank.retrieval.ingestion.repository;

import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İçe alma işlerini okur ve yazar.
 */
public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {

    Optional<IngestionJob> findByIdAndWorkspaceId(long id, long workspaceId);

    List<IngestionJob> findByStageIn(Collection<IngestionStage> stages);

    boolean existsBySourceIdAndStageIn(long sourceId, Collection<IngestionStage> stages);
}
