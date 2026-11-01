package com.rasitbank.retrieval.search.controller;

import com.rasitbank.retrieval.search.domain.dto.SearchChunksRequest;
import com.rasitbank.retrieval.search.domain.dto.SearchResponse;
import com.rasitbank.retrieval.search.service.SearchService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Çalışma alanındaki kaynakların güncel sürümlerinde vektör araması yapan ucu sunar.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public SearchResponse searchChunks(@PathVariable long workspaceId,
            @Valid @RequestBody SearchChunksRequest request) {
        return searchService.searchChunks(workspaceId, Objects.requireNonNull(request.query()),
                Objects.requireNonNull(request.topK()));
    }
}
