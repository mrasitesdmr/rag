package com.rasitbank.retrieval.search.domain.dto;

import java.util.List;

/**
 * Vektör aramasının sonucunu, en yakın parça önce olacak şekilde istemciye taşır.
 */
public record SearchResponse(List<SearchHitResponse> hits) {

    public SearchResponse {
        hits = List.copyOf(hits);
    }

    public static SearchResponse empty() {
        return new SearchResponse(List.of());
    }
}
