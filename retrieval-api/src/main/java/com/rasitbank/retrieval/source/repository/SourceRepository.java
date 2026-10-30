package com.rasitbank.retrieval.source.repository;

import com.rasitbank.retrieval.source.domain.entity.Source;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Bilgi kaynaklarını okur ve yazar.
 */
public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByWorkspaceIdAndName(long workspaceId, String name);
}
