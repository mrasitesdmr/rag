package com.rasitbank.retrieval.search.service;

import com.rasitbank.retrieval.embedding.client.EmbeddingClient;
import com.rasitbank.retrieval.search.domain.dto.SearchHitResponse;
import com.rasitbank.retrieval.search.domain.dto.SearchResponse;
import com.rasitbank.retrieval.search.repository.ChunkSearchRepository;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sorguyu embedding modeliyle vektöre çevirip çalışma alanındaki kaynakların güncel sürümlerinde en yakın parçaları bulur.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final WorkspaceService workspaceService;
    private final SourceRepository sourceRepository;
    private final EmbeddingClient embeddingClient;
    private final ChunkSearchRepository chunkSearchRepository;

    public SearchService(WorkspaceService workspaceService, SourceRepository sourceRepository,
            EmbeddingClient embeddingClient, ChunkSearchRepository chunkSearchRepository) {
        this.workspaceService = workspaceService;
        this.sourceRepository = sourceRepository;
        this.embeddingClient = embeddingClient;
        this.chunkSearchRepository = chunkSearchRepository;
    }

    public SearchResponse searchChunks(long workspaceId, String query, int topK) {
        long startNanos = System.nanoTime();
        String strippedQuery = query.strip();
        String embeddingModel = embeddingClient.modelLabel();
        workspaceService.ensureWorkspaceExists(workspaceId);
        workspaceService.ensureEmbeddingModelMatches(workspaceId, embeddingModel);
        if (!sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull(workspaceId)) {
            logCompleted(workspaceId, strippedQuery, topK, 0, 0, 0, startNanos);
            return SearchResponse.empty();
        }

        long embeddingStartNanos = System.nanoTime();
        float[] queryEmbedding = embeddingClient.embed(List.of(strippedQuery)).getFirst();
        long embeddingMs = elapsedMs(embeddingStartNanos);

        long sqlStartNanos = System.nanoTime();
        List<SearchHitResponse> hits = chunkSearchRepository.findNearest(workspaceId, queryEmbedding, embeddingModel,
                topK);
        long sqlMs = elapsedMs(sqlStartNanos);

        logCompleted(workspaceId, strippedQuery, topK, hits.size(), embeddingMs, sqlMs, startNanos);
        return new SearchResponse(hits);
    }

    private static void logCompleted(long workspaceId, String strippedQuery, int topK, int hitCount, long embeddingMs,
            long sqlMs, long startNanos) {
        log.info("Search completed workspaceId={} queryLength={} topK={} hitCount={} embeddingMs={} sqlMs={} durationMs={}",
                workspaceId, strippedQuery.length(), topK, hitCount, embeddingMs, sqlMs, elapsedMs(startNanos));
    }

    private static long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
