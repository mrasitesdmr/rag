package com.rasitbank.retrieval.workspace.domain.dto;

import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.workspace.domain.entity.Workspace;

/**
 * Çalışma alanını, embedding modelini ve güncel parçalama ile vektör index ayarlarını istemciye taşır.
 */
public record WorkspaceResponse(
        long workspaceId,
        String name,
        String embeddingModel,
        int embeddingDimensions,
        int targetChunkChars,
        int maxChunkChars,
        int overlapChars,
        String vectorIndexType,
        String distanceMetric) {

    public static final String VECTOR_INDEX_TYPE = "HNSW";
    public static final String DISTANCE_METRIC = "COSINE";

    public static WorkspaceResponse of(Workspace workspace, IngestionProperties ingestion) {
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getEmbeddingModel(),
                workspace.getEmbeddingDimensions(), ingestion.targetChunkChars(), ingestion.maxChunkChars(),
                ingestion.overlapChars(), VECTOR_INDEX_TYPE, DISTANCE_METRIC);
    }
}
