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

    String CHUNKS_CONTAINING_TEXTS_SQL = """
            SELECT t.ordinality AS "ordinality", c.id AS "chunkId", c.source_id AS "sourceId",
                   s.name AS "sourceName", v.version AS "version", c.page_number AS "pageNumber"
            FROM unnest(CAST(:texts AS text[])) WITH ORDINALITY AS t(match_text, ordinality)
            JOIN chunks c
              ON strpos(regexp_replace(lower(translate(c.content, 'Iİ', 'ıi')), '\\s+', ' ', 'g'), t.match_text) > 0
            JOIN sources s ON s.id = c.source_id AND s.active_version_id = c.source_version_id
            JOIN source_versions v ON v.id = c.source_version_id
            WHERE s.workspace_id = :workspaceId
            ORDER BY t.ordinality, c.id
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

    @Query(nativeQuery = true, value = CHUNKS_CONTAINING_TEXTS_SQL)
    List<ChunkTextMatchRow> findChunksContainingTexts(@Param("workspaceId") long workspaceId,
            @Param("texts") String[] normalizedTexts);
}
