package com.rasitbank.retrieval.ingestion.repository;

import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * İçe alma işlerini okur ve yazar.
 */
public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {

    Optional<IngestionJob> findByIdAndWorkspaceId(long id, long workspaceId);

    List<IngestionJob> findByStageIn(Collection<IngestionStage> stages);

    boolean existsBySourceIdAndStageIn(long sourceId, Collection<IngestionStage> stages);

    Optional<IngestionJob> findFirstBySourceIdOrderByIdDesc(long sourceId);

    Page<IngestionJob> findBySourceIdOrderByIdDesc(long sourceId, Pageable pageable);

    @Query("select j from IngestionJob j where j.sourceId in :sourceIds"
            + " and j.id = (select max(j2.id) from IngestionJob j2 where j2.sourceId = j.sourceId)")
    List<IngestionJob> findLatestBySourceIds(@Param("sourceIds") Collection<Long> sourceIds);
}
