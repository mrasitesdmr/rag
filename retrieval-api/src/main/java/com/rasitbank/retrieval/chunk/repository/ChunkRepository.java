package com.rasitbank.retrieval.chunk.repository;

import com.rasitbank.retrieval.chunk.domain.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Parçaları ve embedding'lerini toplu olarak yazar ve okur.
 */
public interface ChunkRepository extends JpaRepository<Chunk, Long> {
}
