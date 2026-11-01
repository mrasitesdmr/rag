package com.rasitbank.retrieval.chunk.repository;

import com.rasitbank.retrieval.chunk.domain.entity.Chunk;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Parçaları ve embedding'lerini toplu olarak yazar; kaynak sürümünün parçalarını filtreleyip sayfa sayfa okur.
 */
public interface ChunkRepository extends JpaRepository<Chunk, Long> {

    String ACTIVE_VERSION_CHUNKS_CONDITION = """
            source_version_id = :sourceVersionId
              AND (CAST(:query AS text) IS NULL
                   OR strpos(regexp_replace(lower(translate(heading || ' ' || content, 'Iİ', 'ıi')), '\\s+', ' ', 'g'),
                             CAST(:query AS text)) > 0)
            """;

    Optional<Chunk> findByIdAndWorkspaceId(long id, long workspaceId);

    @Query("select c.sourceVersionId as sourceVersionId, avg(c.tokenCount) as averageTokenCount"
            + " from Chunk c where c.sourceVersionId in :versionIds group by c.sourceVersionId")
    List<VersionTokenAverage> averageTokenCountsByVersionIds(@Param("versionIds") Collection<Long> versionIds);

    @Query(nativeQuery = true,
            value = "SELECT * FROM chunks WHERE " + ACTIVE_VERSION_CHUNKS_CONDITION + " ORDER BY chunk_no",
            countQuery = "SELECT count(*) FROM chunks WHERE " + ACTIVE_VERSION_CHUNKS_CONDITION)
    Page<Chunk> findActiveVersionChunks(@Param("sourceVersionId") long sourceVersionId,
            @Param("query") @Nullable String normalizedQuery, Pageable pageable);
}
