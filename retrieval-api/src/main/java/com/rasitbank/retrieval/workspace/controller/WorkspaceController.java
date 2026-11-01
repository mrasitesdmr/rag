package com.rasitbank.retrieval.workspace.controller;

import com.rasitbank.retrieval.workspace.domain.dto.WorkspaceResponse;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Çalışma alanını embedding modeli, parçalama ve index ayarlarıyla Bilgi kaynakları ekranına sunar.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public WorkspaceResponse getWorkspaceResponse(@PathVariable long workspaceId) {
        return workspaceService.getWorkspaceResponse(workspaceId);
    }
}
