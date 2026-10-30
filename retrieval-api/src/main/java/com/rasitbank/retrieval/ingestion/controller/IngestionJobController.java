package com.rasitbank.retrieval.ingestion.controller;

import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.service.IngestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * İçe alma işinin aşamasını ve ilerlemesini polling için sunar.
 */
@RestController
public class IngestionJobController {

    private final IngestionService ingestionService;

    public IngestionJobController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("/api/workspaces/{workspaceId}/ingestion-jobs/{jobId}")
    public IngestionJobResponse getIngestionJob(@PathVariable long workspaceId, @PathVariable long jobId) {
        return ingestionService.getIngestionJob(workspaceId, jobId);
    }
}
