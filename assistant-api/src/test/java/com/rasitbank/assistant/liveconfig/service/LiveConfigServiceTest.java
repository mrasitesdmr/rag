package com.rasitbank.assistant.liveconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.liveconfig.domain.dto.KnowledgeBaseSource;
import com.rasitbank.assistant.liveconfig.domain.dto.KnowledgeBaseStatus;
import com.rasitbank.assistant.liveconfig.domain.dto.LiveConfigResponse;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.retrieval.client.SourcePageResponse;
import com.rasitbank.assistant.retrieval.client.SourceResponse;
import com.rasitbank.assistant.retrieval.client.WorkspaceResponse;
import com.rasitbank.assistant.retrieval.service.RerankerStatus;
import com.rasitbank.assistant.retrieval.service.RerankerStatusTracker;
import com.rasitbank.assistant.support.TestProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Canlı yapılandırma servisinin ayarlardaki değerleri, retrieval-api'den okunan embedding ve aranabilir belgeleri
 * doğru birleştirdiğini, retrieval-api kapalıyken de düşmediğini doğrular.
 */
@ExtendWith(MockitoExtension.class)
class LiveConfigServiceTest {

    private static final WorkspaceResponse WORKSPACE = new WorkspaceResponse(42, "Raşit Bankası · Dijital Asistan",
            "bge-m3", 1024);
    private static final SourceResponse GUIDE = new SourceResponse(10001, "Raşit Bankası Ürün ve Hizmet Rehberi",
            "2026.10", 61);

    @Mock
    private RetrievalClient retrievalClient;

    private LiveConfigService service;

    @BeforeEach
    void setUp() {
        service = serviceFor(TestProperties.defaults());
    }

    private LiveConfigService serviceFor(com.rasitbank.assistant.config.AssistantProperties properties) {
        return new LiveConfigService(properties, "qwen3.5:9b", retrievalClient,
                new RerankerStatusTracker(properties));
    }

    private static SourcePageResponse page(int page, int totalPages, long totalElements, SourceResponse... sources) {
        return new SourcePageResponse(List.of(sources), page, 100, totalElements, totalPages);
    }

    @Test
    void givenDefaultsAndOneSearchableSource_whenGetLiveConfig_thenConfiguredValues() {
        // Arrange
        given(retrievalClient.getWorkspace()).willReturn(WORKSPACE);
        given(retrievalClient.listSources(0, 100)).willReturn(page(0, 1, 1, GUIDE));

        // Act
        LiveConfigResponse config = service.getLiveConfig();

        // Assert
        assertThat(config.chatModel()).isEqualTo("qwen3.5:9b");
        assertThat(config.embeddingModel()).isEqualTo("bge-m3");
        assertThat(config.embeddingDimensions()).isEqualTo(1024);
        assertThat(config.generationMode()).isEqualTo(GenerationMode.RAG);
        assertThat(config.retrievalStrategy()).isEqualTo(RetrievalStrategy.RERANK);
        assertThat(config.candidates()).isEqualTo(20);
        assertThat(config.topK()).isEqualTo(5);
        assertThat(config.queryRewriteEnabled()).isTrue();
        assertThat(config.rerankerStatus()).isEqualTo(RerankerStatus.UNKNOWN);
        assertThat(config.knowledgeBase().status()).isEqualTo(KnowledgeBaseStatus.READY);
        assertThat(config.knowledgeBase().searchableSourceCount()).isEqualTo(1);
        assertThat(config.knowledgeBase().sources())
                .containsExactly(new KnowledgeBaseSource(10001, "Raşit Bankası Ürün ve Hizmet Rehberi", "2026.10"));
    }

    @Test
    void givenSourceWithoutCurrentVersion_whenGetLiveConfig_thenNotCounted() {
        // Arrange
        given(retrievalClient.getWorkspace()).willReturn(WORKSPACE);
        given(retrievalClient.listSources(0, 100)).willReturn(page(0, 1, 3, GUIDE,
                new SourceResponse(10002, "İşleniyor", "v1", null), new SourceResponse(10003, "İkinci", "v2", 4)));

        // Act
        LiveConfigResponse config = service.getLiveConfig();

        // Assert
        assertThat(config.knowledgeBase().searchableSourceCount()).isEqualTo(2);
        assertThat(config.knowledgeBase().sources()).extracting(KnowledgeBaseSource::sourceId)
                .containsExactly(10001L, 10003L);
    }

    @Test
    void givenNoSources_whenGetLiveConfig_thenEmpty() {
        // Arrange
        given(retrievalClient.getWorkspace()).willReturn(WORKSPACE);
        given(retrievalClient.listSources(0, 100)).willReturn(page(0, 0, 0));

        // Act
        LiveConfigResponse config = service.getLiveConfig();

        // Assert
        assertThat(config.knowledgeBase().status()).isEqualTo(KnowledgeBaseStatus.EMPTY);
        assertThat(config.knowledgeBase().searchableSourceCount()).isZero();
        assertThat(config.knowledgeBase().sources()).isEmpty();
        assertThat(config.embeddingModel()).isEqualTo("bge-m3");
    }

    @Test
    void givenRetrievalUnavailable_whenGetLiveConfig_thenUnavailableWithoutFailing() {
        // Arrange
        given(retrievalClient.getWorkspace())
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        // Act
        LiveConfigResponse config = service.getLiveConfig();

        // Assert
        assertThat(config.knowledgeBase().status()).isEqualTo(KnowledgeBaseStatus.UNAVAILABLE);
        assertThat(config.knowledgeBase().searchableSourceCount()).isNull();
        assertThat(config.knowledgeBase().sources()).isEmpty();
        assertThat(config.embeddingModel()).isNull();
        assertThat(config.embeddingDimensions()).isNull();
        assertThat(config.chatModel()).isEqualTo("qwen3.5:9b");
        then(retrievalClient).should(never()).listSources(0, 100);
    }

    @Test
    void givenSourceListFailsAfterWorkspaceRead_whenGetLiveConfig_thenEmbeddingAlsoNull() {
        // Arrange
        given(retrievalClient.getWorkspace()).willReturn(WORKSPACE);
        given(retrievalClient.listSources(0, 100))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        // Act
        LiveConfigResponse config = service.getLiveConfig();

        // Assert
        assertThat(config.knowledgeBase().status()).isEqualTo(KnowledgeBaseStatus.UNAVAILABLE);
        assertThat(config.embeddingModel()).isNull();
    }

    @Test
    void givenTwoPages_whenGetLiveConfig_thenReadsBothWithSize100() {
        // Arrange
        given(retrievalClient.getWorkspace()).willReturn(WORKSPACE);
        given(retrievalClient.listSources(0, 100)).willReturn(page(0, 2, 102, GUIDE));
        given(retrievalClient.listSources(1, 100)).willReturn(page(1, 2, 102,
                new SourceResponse(10005, "Ek", "v1", 3), new SourceResponse(10004, "Ek 2", "v1", 2)));

        // Act
        LiveConfigResponse config = service.getLiveConfig();

        // Assert
        then(retrievalClient).should().listSources(0, 100);
        then(retrievalClient).should().listSources(1, 100);
        then(retrievalClient).should(never()).listSources(2, 100);
        assertThat(config.knowledgeBase().searchableSourceCount()).isEqualTo(3);
        assertThat(config.knowledgeBase().sources()).extracting(KnowledgeBaseSource::sourceId)
                .containsExactly(10001L, 10004L, 10005L);
    }

    @Test
    void givenLlmOnlyMode_whenGetLiveConfig_thenModeLlmOnlyAndRerankerNotUsedButStrategyConfigured() {
        // Arrange
        LiveConfigService llmService = serviceFor(TestProperties.withMode("llm", "rerank"));
        given(retrievalClient.getWorkspace()).willReturn(WORKSPACE);
        given(retrievalClient.listSources(0, 100)).willReturn(page(0, 1, 1, GUIDE));

        // Act
        LiveConfigResponse config = llmService.getLiveConfig();

        // Assert
        assertThat(config.generationMode()).isEqualTo(GenerationMode.LLM_ONLY);
        assertThat(config.retrievalStrategy()).isEqualTo(RetrievalStrategy.RERANK);
        assertThat(config.rerankerStatus()).isEqualTo(RerankerStatus.NOT_USED);
    }

    @Test
    void givenTopKStrategy_whenGetLiveConfig_thenTopKAndRerankerNotUsed() {
        // Arrange
        LiveConfigService topKService = serviceFor(TestProperties.withStrategy("top5"));
        given(retrievalClient.getWorkspace()).willReturn(WORKSPACE);
        given(retrievalClient.listSources(0, 100)).willReturn(page(0, 1, 1, GUIDE));

        // Act
        LiveConfigResponse config = topKService.getLiveConfig();

        // Assert
        assertThat(config.retrievalStrategy()).isEqualTo(RetrievalStrategy.TOP_K);
        assertThat(config.rerankerStatus()).isEqualTo(RerankerStatus.NOT_USED);
    }

    @Test
    void givenRerankerMeasuredDown_whenGetLiveConfig_thenRerankerStatusDown() {
        // Arrange
        com.rasitbank.assistant.config.AssistantProperties properties = TestProperties.defaults();
        RerankerStatusTracker tracker = new RerankerStatusTracker(properties);
        tracker.recordUnavailable();
        LiveConfigService downService = new LiveConfigService(properties, "qwen3.5:9b", retrievalClient, tracker);
        given(retrievalClient.getWorkspace()).willReturn(WORKSPACE);
        given(retrievalClient.listSources(0, 100)).willReturn(page(0, 1, 1, GUIDE));

        // Act
        LiveConfigResponse config = downService.getLiveConfig();

        // Assert
        assertThat(config.rerankerStatus()).isEqualTo(RerankerStatus.DOWN);
    }

    @Test
    void givenGetLiveConfig_whenRetrievalCalled_thenNoTransactionActive() {
        // Arrange
        boolean[] transactionSeen = new boolean[2];
        given(retrievalClient.getWorkspace()).willAnswer(invocation -> {
            transactionSeen[0] = TransactionSynchronizationManager.isActualTransactionActive();
            return WORKSPACE;
        });
        given(retrievalClient.listSources(0, 100)).willAnswer(invocation -> {
            transactionSeen[1] = TransactionSynchronizationManager.isActualTransactionActive();
            return page(0, 1, 1, GUIDE);
        });

        // Act
        service.getLiveConfig();

        // Assert
        assertThat(transactionSeen).containsExactly(false, false);
    }
}
