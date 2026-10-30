package com.rasitbank.retrieval.ingestion.repository;

import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJobFile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İçe alma işlerine yüklenen dosyaların baytlarını okur ve yazar.
 */
public interface IngestionJobFileRepository extends JpaRepository<IngestionJobFile, Long> {
}
