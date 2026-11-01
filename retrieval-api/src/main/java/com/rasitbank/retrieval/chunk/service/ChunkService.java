package com.rasitbank.retrieval.chunk.service;

import com.rasitbank.retrieval.chunk.domain.dto.ChunkResponse;
import com.rasitbank.retrieval.chunk.domain.dto.ChunkSummaryResponse;
import com.rasitbank.retrieval.chunk.domain.entity.Chunk;
import com.rasitbank.retrieval.chunk.repository.ChunkRepository;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceVersion;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.source.repository.SourceVersionRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kaynağın güncel sürümünün parçalarını başlık ya da metne göre süzüp sayfa sayfa listeler ve parça penceresi için tek
 * parçayı sunar.
 */
@Service
public class ChunkService {

    public static final int EMBEDDING_PREVIEW_SIZE = ChunkResponse.EMBEDDING_PREVIEW_SIZE;
    public static final int QUERY_MAX_LENGTH = 200;

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final WorkspaceService workspaceService;
    private final SourceRepository sourceRepository;
    private final SourceVersionRepository sourceVersionRepository;
    private final ChunkRepository chunkRepository;

    public ChunkService(WorkspaceService workspaceService, SourceRepository sourceRepository,
            SourceVersionRepository sourceVersionRepository, ChunkRepository chunkRepository) {
        this.workspaceService = workspaceService;
        this.sourceRepository = sourceRepository;
        this.sourceVersionRepository = sourceVersionRepository;
        this.chunkRepository = chunkRepository;
    }

    @Transactional(readOnly = true)
    public Page<ChunkSummaryResponse> listChunks(long workspaceId, long sourceId, @Nullable String query, int page,
            int size) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        Source source = sourceRepository.findByIdAndWorkspaceId(sourceId, workspaceId)
                .orElseThrow(() -> new NotFoundException("SOURCE_NOT_FOUND", "Source not found: " + sourceId));
        PageRequest pageRequest = PageRequest.of(page, size);
        Long activeVersionId = source.getActiveVersionId();
        if (activeVersionId == null) {
            return Page.empty(pageRequest);
        }
        return chunkRepository.findActiveVersionChunks(activeVersionId, normalizeQuery(query), pageRequest)
                .map(ChunkSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ChunkResponse getChunk(long workspaceId, long chunkId) {
        workspaceService.ensureWorkspaceExists(workspaceId);
        Chunk chunk = chunkRepository.findByIdAndWorkspaceId(chunkId, workspaceId)
                .orElseThrow(() -> new NotFoundException("CHUNK_NOT_FOUND", "Chunk not found: " + chunkId));
        Source source = sourceRepository.findById(chunk.getSourceId())
                .orElseThrow(() -> new IllegalStateException("Source of chunk not found: " + chunkId));
        SourceVersion sourceVersion = sourceVersionRepository.findById(chunk.getSourceVersionId())
                .orElseThrow(() -> new IllegalStateException("Source version of chunk not found: " + chunkId));
        return ChunkResponse.of(chunk, source, sourceVersion);
    }

    public static @Nullable String normalizeQuery(@Nullable String query) {
        if (query == null) {
            return null;
        }
        String stripped = query.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        return WHITESPACE.matcher(stripped.toLowerCase(TURKISH)).replaceAll(" ");
    }
}
