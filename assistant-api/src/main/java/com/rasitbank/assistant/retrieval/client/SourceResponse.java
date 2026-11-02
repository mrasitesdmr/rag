package com.rasitbank.assistant.retrieval.client;

import org.jspecify.annotations.Nullable;

/**
 * retrieval-api'nin kaynak listesindeki bir kaynağın canlı yapılandırmanın ihtiyaç duyduğu alanlarıdır; parça sayısı
 * boşsa kaynağın güncel sürümü yoktur ve aranmaz.
 */
public record SourceResponse(long sourceId, String name, String version, @Nullable Integer chunkCount) {
}
