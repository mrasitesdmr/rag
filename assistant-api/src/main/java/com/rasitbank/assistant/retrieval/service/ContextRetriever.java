package com.rasitbank.assistant.retrieval.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.client.RerankClient;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.retrieval.client.SearchHit;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Soruya göre prompt'a girecek parçaları seçer: yapılandırılan stratejiye göre vector sırasının ilkleri ya da
 * reranker puanına göre en iyileri; reranker başarısız olursa vector sırasına döner. Rerank sonucunu reranker durum
 * takipçisine bildirir.
 */
@Service
public class ContextRetriever {

    private static final Logger log = LoggerFactory.getLogger(ContextRetriever.class);

    private static final Comparator<RetrievedChunk> BY_RERANK_SCORE_THEN_VECTOR_RANK =
            Comparator.comparing((RetrievedChunk chunk) -> Objects.requireNonNull(chunk.rerankScore()),
                            Comparator.reverseOrder())
                    .thenComparingInt(RetrievedChunk::vectorRank);

    private final RetrievalClient retrievalClient;
    private final RerankClient rerankClient;
    private final RerankerStatusTracker rerankerStatusTracker;
    private final AssistantProperties.Retrieval settings;

    public ContextRetriever(RetrievalClient retrievalClient, RerankClient rerankClient,
            RerankerStatusTracker rerankerStatusTracker, AssistantProperties properties) {
        this.retrievalClient = retrievalClient;
        this.rerankClient = rerankClient;
        this.rerankerStatusTracker = rerankerStatusTracker;
        this.settings = properties.retrieval();
    }

    public RetrievedContext retrieveContext(String query) {
        RetrievalStrategy strategy = settings.configuredStrategy();
        int topK = settings.topK();
        int candidates = settings.candidates();
        int searchLimit = strategy == RetrievalStrategy.TOP_K ? topK : candidates;

        long searchStart = System.nanoTime();
        List<SearchHit> hits = retrievalClient.search(query, searchLimit);
        long retrievalLatencyMs = elapsedMs(searchStart);
        List<RetrievedChunk> candidateChunks = inVectorOrder(hits);

        if (strategy == RetrievalStrategy.TOP_K || candidateChunks.isEmpty()) {
            return new RetrievedContext(strategy, candidates, topK, candidateChunks, firstInVectorOrder(candidateChunks, topK),
                    retrievalLatencyMs, null);
        }
        return rerank(query, candidateChunks, candidates, topK, retrievalLatencyMs);
    }

    private RetrievedContext rerank(String query, List<RetrievedChunk> candidateChunks, int candidates, int topK,
            long retrievalLatencyMs) {
        List<String> documents = candidateChunks.stream()
                .map(chunk -> chunk.hit().heading() + "\n\n" + chunk.hit().content())
                .toList();
        long rerankStart = System.nanoTime();
        try {
            List<Double> scores = rerankClient.rerankChunks(query, documents);
            long rerankLatencyMs = elapsedMs(rerankStart);
            rerankerStatusTracker.recordAvailable();
            List<RetrievedChunk> scoredChunks = new ArrayList<>(candidateChunks.size());
            for (int i = 0; i < candidateChunks.size(); i++) {
                RetrievedChunk chunk = candidateChunks.get(i);
                scoredChunks.add(new RetrievedChunk(chunk.hit(), chunk.vectorRank(), scores.get(i)));
            }
            List<RetrievedChunk> contextChunks = scoredChunks.stream()
                    .sorted(BY_RERANK_SCORE_THEN_VECTOR_RANK)
                    .limit(topK)
                    .toList();
            log.info("Reranked candidates={} kept={} durationMs={}", candidateChunks.size(), contextChunks.size(),
                    rerankLatencyMs);
            return new RetrievedContext(RetrievalStrategy.RERANK, candidates, topK, scoredChunks, contextChunks,
                    retrievalLatencyMs, rerankLatencyMs);
        } catch (ServiceUnavailableException exception) {
            long rerankLatencyMs = elapsedMs(rerankStart);
            rerankerStatusTracker.recordUnavailable();
            log.warn("Reranker unavailable, falling back to vector order candidates={} topK={} reason={}",
                    candidateChunks.size(), topK, exception.getMessage());
            return new RetrievedContext(RetrievalStrategy.RERANK_FALLBACK, candidates, topK, candidateChunks,
                    firstInVectorOrder(candidateChunks, topK), retrievalLatencyMs, rerankLatencyMs);
        }
    }

    private static List<RetrievedChunk> inVectorOrder(List<SearchHit> hits) {
        List<RetrievedChunk> chunks = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            chunks.add(new RetrievedChunk(hits.get(i), i + 1, null));
        }
        return chunks;
    }

    private static List<RetrievedChunk> firstInVectorOrder(List<RetrievedChunk> chunks, int topK) {
        return chunks.stream().limit(topK).toList();
    }

    private static long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
