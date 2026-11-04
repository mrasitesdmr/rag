package com.rasitbank.assistant.support;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.retrieval.client.SearchHit;
import com.rasitbank.assistant.retrieval.service.RetrievedChunk;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Testlerde retrieval sonucunu (adaylar ve prompt'a giren parçalar) elle belli sıralarla kurar.
 */
public final class ContextFixtures {

    private ContextFixtures() {
    }

    /** 20 aday vector sırasıyla; her adayın puanı 10 - vectorRank / 10; bağlam verilen vector sıraları. */
    public static RetrievedContext rerankContext(int... contextVectorRanks) {
        List<SearchHit> hits = SearchHitFixtures.hits(20);
        List<RetrievedChunk> candidates = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            candidates.add(new RetrievedChunk(hits.get(i), i + 1, 10.0 - (i + 1) / 10.0));
        }
        List<RetrievedChunk> context = new ArrayList<>();
        for (int rank : contextVectorRanks) {
            context.add(candidates.get(rank - 1));
        }
        return new RetrievedContext(RetrievalStrategy.RERANK, 20, 5, candidates, context, 84L, 712L);
    }

    /** 20 aday vector sırasıyla; en iyi adayın puanı verilen değer, sonrakiler 0,1 azalır; bağlam ilk 5 aday. */
    public static RetrievedContext rerankContextWithTopScore(double topScore) {
        List<SearchHit> hits = SearchHitFixtures.hits(20);
        List<RetrievedChunk> candidates = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            candidates.add(new RetrievedChunk(hits.get(i), i + 1, topScore - i / 10.0));
        }
        return new RetrievedContext(RetrievalStrategy.RERANK, 20, 5, candidates, candidates.subList(0, 5), 84L,
                712L);
    }

    /** İlk 5 vector sırası bağlamda, puansız (TOP_K). */
    public static RetrievedContext topKContext() {
        List<RetrievedChunk> candidates = new ArrayList<>();
        List<SearchHit> hits = SearchHitFixtures.hits(5);
        for (int i = 0; i < hits.size(); i++) {
            candidates.add(new RetrievedChunk(hits.get(i), i + 1, null));
        }
        return new RetrievedContext(RetrievalStrategy.TOP_K, 20, 5, candidates, candidates, 84L, null);
    }

    public static RetrievedContext emptyContext(@Nullable RetrievalStrategy strategy) {
        return new RetrievedContext(strategy == null ? RetrievalStrategy.RERANK : strategy, 20, 5, List.of(),
                List.of(), 12L, null);
    }
}
