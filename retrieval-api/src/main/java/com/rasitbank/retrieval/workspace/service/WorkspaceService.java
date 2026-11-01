package com.rasitbank.retrieval.workspace.service;

import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.exception.ConflictException;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.workspace.domain.dto.WorkspaceResponse;
import com.rasitbank.retrieval.workspace.domain.entity.Workspace;
import com.rasitbank.retrieval.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışma alanını ayarlarıyla birlikte okur; istekteki çalışma alanının var olduğunu ve embedding modelinin ayardakiyle aynı olduğunu doğrular.
 */
@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final IngestionProperties ingestionProperties;

    public WorkspaceService(WorkspaceRepository workspaceRepository, IngestionProperties ingestionProperties) {
        this.workspaceRepository = workspaceRepository;
        this.ingestionProperties = ingestionProperties;
    }

    public Workspace getWorkspace(long workspaceId) {
        return workspaceRepository.findById(workspaceId).orElseThrow(() -> workspaceNotFound(workspaceId));
    }

    public WorkspaceResponse getWorkspaceResponse(long workspaceId) {
        return WorkspaceResponse.of(getWorkspace(workspaceId), ingestionProperties);
    }

    public void ensureWorkspaceExists(long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw workspaceNotFound(workspaceId);
        }
    }

    public void ensureEmbeddingModelMatches(long workspaceId, String configuredModelLabel) {
        Workspace workspace = getWorkspace(workspaceId);
        if (!workspace.embeddingModelLabel().equals(configuredModelLabel)) {
            throw new ConflictException("EMBEDDING_MODEL_MISMATCH", "Configured embedding model "
                    + configuredModelLabel + " is not the workspace's " + workspace.embeddingModelLabel());
        }
    }

    private static NotFoundException workspaceNotFound(long workspaceId) {
        return new NotFoundException("WORKSPACE_NOT_FOUND", "Workspace not found: " + workspaceId);
    }
}
