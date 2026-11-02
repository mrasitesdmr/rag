package com.rasitbank.assistant.liveconfig.domain.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Canlı yapılandırmadaki bilgi bankası bölümüdür: durum, aranabilir belge sayısı (retrieval-api okunamadıysa boş) ve
 * aranabilir belgeler.
 */
public record KnowledgeBaseResponse(KnowledgeBaseStatus status, @Nullable Integer searchableSourceCount,
        List<KnowledgeBaseSource> sources) {

    public KnowledgeBaseResponse {
        sources = List.copyOf(sources);
    }

    public static KnowledgeBaseResponse unavailable() {
        return new KnowledgeBaseResponse(KnowledgeBaseStatus.UNAVAILABLE, null, List.of());
    }

    public static KnowledgeBaseResponse of(List<KnowledgeBaseSource> sources) {
        KnowledgeBaseStatus status = sources.isEmpty() ? KnowledgeBaseStatus.EMPTY : KnowledgeBaseStatus.READY;
        return new KnowledgeBaseResponse(status, sources.size(), sources);
    }
}
