package com.rasitbank.retrieval.ingestion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bir içe alma işine yüklenen dosyanın baytlarıdır; iş satırı okunurken yüklenmesin diye ayrı tabloda durur.
 */
@Entity
@Table(name = "ingestion_job_files")
public class IngestionJobFile {

    @Id
    @Column(name = "ingestion_job_id")
    private Long ingestionJobId;

    @Column(nullable = false)
    private byte[] content;

    protected IngestionJobFile() {
    }

    private IngestionJobFile(long ingestionJobId, byte[] content) {
        this.ingestionJobId = ingestionJobId;
        this.content = content.clone();
    }

    public static IngestionJobFile of(long ingestionJobId, byte[] content) {
        return new IngestionJobFile(ingestionJobId, content);
    }

    public Long getIngestionJobId() {
        return ingestionJobId;
    }

    public byte[] getContent() {
        return content.clone();
    }
}
