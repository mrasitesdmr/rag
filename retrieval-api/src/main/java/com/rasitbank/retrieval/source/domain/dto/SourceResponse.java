package com.rasitbank.retrieval.source.domain.dto;

import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import java.time.Instant;

/**
 * Kaynak listesindeki bir bilgi kaynağını istemciye taşır.
 */
public record SourceResponse(long sourceId, long workspaceId, String name, SourceKind kind, Instant createdAt) {

    public static SourceResponse from(Source source) {
        return new SourceResponse(source.getId(), source.getWorkspaceId(), source.getName(), source.getKind(),
                source.getCreatedAt());
    }
}
