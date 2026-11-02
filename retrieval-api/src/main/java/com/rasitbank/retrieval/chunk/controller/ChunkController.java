package com.rasitbank.retrieval.chunk.controller;

import com.rasitbank.retrieval.chunk.domain.dto.ChunkMatchesResponse;
import com.rasitbank.retrieval.chunk.domain.dto.ChunkResponse;
import com.rasitbank.retrieval.chunk.domain.dto.ChunkSummaryResponse;
import com.rasitbank.retrieval.chunk.domain.dto.FindChunkMatchesRequest;
import com.rasitbank.retrieval.chunk.service.ChunkService;
import com.rasitbank.retrieval.pagination.PageParameters;
import com.rasitbank.retrieval.pagination.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kaynağın güncel sürümünün parçalarını süzülmüş ve sayfalı olarak, tek parçayı parça penceresi için sunar ve verilen
 * metinleri içeren güncel parçaları bulur.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
public class ChunkController {

    private static final String QUERY_SIZE_MESSAGE = "size must be at most 200 characters";

    private final ChunkService chunkService;

    public ChunkController(ChunkService chunkService) {
        this.chunkService = chunkService;
    }

    @GetMapping("/sources/{sourceId}/chunks")
    public PageResponse<ChunkSummaryResponse> listChunks(@PathVariable long workspaceId, @PathVariable long sourceId,
            @RequestParam(name = "query", required = false)
            @Size(max = ChunkService.QUERY_MAX_LENGTH, message = QUERY_SIZE_MESSAGE) @Nullable String query,
            @RequestParam(name = "page", defaultValue = "0")
            @Min(value = 0, message = PageParameters.PAGE_MESSAGE) int page,
            @RequestParam(name = "size", defaultValue = "20")
            @Min(value = 1, message = PageParameters.SIZE_MESSAGE)
            @Max(value = PageParameters.MAX_SIZE, message = PageParameters.SIZE_MESSAGE) int size) {
        return PageResponse.from(chunkService.listChunks(workspaceId, sourceId, query, page, size));
    }

    @GetMapping("/chunks/{chunkId}")
    public ChunkResponse getChunk(@PathVariable long workspaceId, @PathVariable long chunkId) {
        return chunkService.getChunk(workspaceId, chunkId);
    }

    @PostMapping(path = "/chunk-matches", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChunkMatchesResponse findChunkMatches(@PathVariable long workspaceId,
            @Valid @RequestBody FindChunkMatchesRequest request) {
        List<String> texts = Objects.requireNonNull(request.texts()).stream().map(Objects::requireNonNull).toList();
        return chunkService.findChunkMatches(workspaceId, texts);
    }
}
