package com.rasitbank.assistant.retrieval.client;

import java.util.List;

/**
 * retrieval-api'nin kaynak listesinin bir sayfasıdır.
 */
public record SourcePageResponse(List<SourceResponse> content, int page, int size, long totalElements,
        int totalPages) {

    public SourcePageResponse {
        content = List.copyOf(content);
    }
}
