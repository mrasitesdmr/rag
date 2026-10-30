package com.rasitbank.retrieval.workspace.service;

import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.workspace.domain.entity.Workspace;
import com.rasitbank.retrieval.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışma alanını okur ve istekteki çalışma alanının var olduğunu doğrular.
 */
@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public Workspace getWorkspace(long workspaceId) {
        return workspaceRepository.findById(workspaceId).orElseThrow(() -> workspaceNotFound(workspaceId));
    }

    public void ensureWorkspaceExists(long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw workspaceNotFound(workspaceId);
        }
    }

    private static NotFoundException workspaceNotFound(long workspaceId) {
        return new NotFoundException("WORKSPACE_NOT_FOUND", "Workspace not found: " + workspaceId);
    }
}
