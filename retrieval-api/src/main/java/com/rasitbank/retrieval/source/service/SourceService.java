package com.rasitbank.retrieval.source.service;

import com.rasitbank.retrieval.source.domain.dto.SourceResponse;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışma alanının bilgi kaynaklarını sabit sırayla (kimliğe göre) sayfa sayfa listeler.
 */
@Service
public class SourceService {

    private final WorkspaceService workspaceService;
    private final SourceRepository sourceRepository;

    public SourceService(WorkspaceService workspaceService, SourceRepository sourceRepository) {
        this.workspaceService = workspaceService;
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public Page<SourceResponse> listSources(long workspaceId, int page, int size) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        return sourceRepository.findByWorkspaceIdOrderByIdAsc(workspaceId, PageRequest.of(page, size))
                .map(SourceResponse::from);
    }
}
