package com.rasitbank.assistant.support;

import com.rasitbank.assistant.retrieval.client.SearchHit;
import java.util.ArrayList;
import java.util.List;

/**
 * Testlerde retrieval-api'nin döndüğü aday parçaları elle belli değerlerle üretir.
 */
public final class SearchHitFixtures {

    public static final long SOURCE_ID = 10001L;
    public static final String SOURCE_NAME = "Raşit Bankası Ürün ve Hizmet Rehberi";
    public static final String VERSION = "2026.10";

    private SearchHitFixtures() {
    }

    /** Vector sırası 1..count: chunkId 100+sıra, başlık "H"+chunkId, içerik "C"+chunkId, mesafe 0.30 + 0.01 × sıra. */
    public static List<SearchHit> hits(int count) {
        List<SearchHit> hits = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            long chunkId = 100L + rank;
            hits.add(new SearchHit(chunkId, rank, 10 + rank, "H" + chunkId, "C" + chunkId, SOURCE_ID, SOURCE_NAME,
                    VERSION, 0.30 + 0.01 * rank));
        }
        return hits;
    }
}
