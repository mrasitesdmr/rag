package com.rasitbank.retrieval.workspace.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Kaynakların, parçaların ve içe alma işlerinin bağlı olduğu çalışma alanıdır; embedding modeli bu düzeyde tektir.
 */
@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    @Column(name = "embedding_dimensions", nullable = false)
    private int embeddingDimensions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Workspace() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String embeddingModelLabel() {
        return embeddingModel + "@" + embeddingDimensions;
    }
}
