package com.rasitbank.assistant.liveconfig.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.liveconfig.domain.dto.KnowledgeBaseResponse;
import com.rasitbank.assistant.liveconfig.domain.dto.KnowledgeBaseSource;
import com.rasitbank.assistant.liveconfig.domain.dto.LiveConfigResponse;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.retrieval.client.SourcePageResponse;
import com.rasitbank.assistant.retrieval.client.SourceResponse;
import com.rasitbank.assistant.retrieval.client.WorkspaceResponse;
import com.rasitbank.assistant.retrieval.service.RerankerStatusTracker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Asistanın etkin yapılandırmasını toplar: ayarlardaki değerler, reranker'ın son ölçülen durumu ve retrieval-api'den
 * okunan embedding modeli ile aranabilir belgeler. retrieval-api okunamazsa yapılandırmanın geri kalanı yine döner.
 */
@Service
public class LiveConfigService {

    static final int SOURCE_PAGE_SIZE = 100;

    private final AssistantProperties properties;
    private final String chatModelName;
    private final RetrievalClient retrievalClient;
    private final RerankerStatusTracker rerankerStatusTracker;

    public LiveConfigService(AssistantProperties properties,
            @Value("${spring.ai.ollama.chat.model}") String chatModelName, RetrievalClient retrievalClient,
            RerankerStatusTracker rerankerStatusTracker) {
        this.properties = properties;
        this.chatModelName = chatModelName;
        this.retrievalClient = retrievalClient;
        this.rerankerStatusTracker = rerankerStatusTracker;
    }

    public LiveConfigResponse getLiveConfig() {
        AssistantProperties.Retrieval retrieval = properties.retrieval();
        @Nullable WorkspaceResponse workspace;
        KnowledgeBaseResponse knowledgeBase;
        try {
            WorkspaceResponse readWorkspace = retrievalClient.getWorkspace();
            knowledgeBase = KnowledgeBaseResponse.of(listSearchableSources());
            workspace = readWorkspace;
        } catch (ServiceUnavailableException exception) {
            workspace = null;
            knowledgeBase = KnowledgeBaseResponse.unavailable();
        }
        return new LiveConfigResponse(
                chatModelName,
                workspace == null ? null : workspace.embeddingModel(),
                workspace == null ? null : workspace.embeddingDimensions(),
                properties.generation().configuredMode(),
                retrieval.configuredStrategy(),
                retrieval.candidates(),
                retrieval.topK(),
                properties.queryRewrite().enabled(),
                rerankerStatusTracker.currentStatus(),
                knowledgeBase);
    }

    private List<KnowledgeBaseSource> listSearchableSources() {
        List<KnowledgeBaseSource> sources = new ArrayList<>();
        int page = 0;
        SourcePageResponse sourcePage;
        do {
            sourcePage = retrievalClient.listSources(page, SOURCE_PAGE_SIZE);
            for (SourceResponse source : sourcePage.content()) {
                if (source.chunkCount() != null) {
                    sources.add(new KnowledgeBaseSource(source.sourceId(), source.name(), source.version()));
                }
            }
            page++;
        } while (page < sourcePage.totalPages());
        sources.sort(Comparator.comparingLong(KnowledgeBaseSource::sourceId));
        return sources;
    }
}
