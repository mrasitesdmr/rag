package com.rasitbank.retrieval.source.controller;

import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.service.IngestionService;
import com.rasitbank.retrieval.pagination.PageParameters;
import com.rasitbank.retrieval.pagination.PageResponse;
import com.rasitbank.retrieval.source.domain.dto.SourceResponse;
import com.rasitbank.retrieval.source.domain.dto.SourceUpload;
import com.rasitbank.retrieval.source.service.SourceService;
import com.rasitbank.retrieval.source.service.SourceUploadValidator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Çalışma alanının kaynaklarını listeler, yüklenen PDF'ten kaynak oluşturur ve başarısız ilk içe almayı yeniden başlatır.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/sources")
public class SourceController {

    private final SourceService sourceService;
    private final SourceUploadValidator sourceUploadValidator;
    private final IngestionService ingestionService;

    public SourceController(SourceService sourceService, SourceUploadValidator sourceUploadValidator,
            IngestionService ingestionService) {
        this.sourceService = sourceService;
        this.sourceUploadValidator = sourceUploadValidator;
        this.ingestionService = ingestionService;
    }

    @GetMapping
    public PageResponse<SourceResponse> listSources(@PathVariable long workspaceId,
            @RequestParam(name = "page", defaultValue = "0")
            @Min(value = 0, message = PageParameters.PAGE_MESSAGE) int page,
            @RequestParam(name = "size", defaultValue = "20")
            @Min(value = 1, message = PageParameters.SIZE_MESSAGE)
            @Max(value = PageParameters.MAX_SIZE, message = PageParameters.SIZE_MESSAGE) int size) {
        return PageResponse.from(sourceService.listSources(workspaceId, page, size));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionJobResponse> createSource(@PathVariable long workspaceId,
            @RequestPart(name = "file", required = false) @Nullable MultipartFile file,
            @RequestPart(name = "name", required = false) @Nullable String name,
            @RequestPart(name = "version", required = false) @Nullable String version) {
        SourceUpload upload = sourceUploadValidator.validate(file, name, version);
        return accepted(ingestionService.createSource(workspaceId, upload));
    }

    @PostMapping("/{sourceId}/ingestion-jobs")
    public ResponseEntity<IngestionJobResponse> retryIngestion(@PathVariable long workspaceId,
            @PathVariable long sourceId) {
        return accepted(ingestionService.retryIngestion(workspaceId, sourceId));
    }

    private static ResponseEntity<IngestionJobResponse> accepted(IngestionJobResponse job) {
        return ResponseEntity.accepted().location(URI.create(job.statusUrl())).body(job);
    }
}
