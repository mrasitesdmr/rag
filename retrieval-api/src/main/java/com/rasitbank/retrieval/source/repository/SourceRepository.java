package com.rasitbank.retrieval.source.repository;

import com.rasitbank.retrieval.source.domain.entity.Source;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Bilgi kaynaklarını okur ve yazar.
 */
public interface SourceRepository extends JpaRepository<Source, Long> {

    @Query("select s from Source s where s.workspaceId = :workspaceId and lower(s.name) = lower(:name)")
    Optional<Source> findByLowerName(@Param("workspaceId") long workspaceId, @Param("name") String name);

    Optional<Source> findByIdAndWorkspaceId(long id, long workspaceId);

    Page<Source> findByWorkspaceIdOrderByIdAsc(long workspaceId, Pageable pageable);

    boolean existsByWorkspaceIdAndActiveVersionIdIsNotNull(long workspaceId);
}
