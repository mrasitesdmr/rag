package com.rasitbank.retrieval.source.repository;

import com.rasitbank.retrieval.source.domain.entity.SourceVersion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Kaynakların içe alınmış sürümlerini okur ve yazar.
 */
public interface SourceVersionRepository extends JpaRepository<SourceVersion, Long> {
}
