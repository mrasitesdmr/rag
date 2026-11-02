package com.rasitbank.assistant.liveconfig.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.exception.GlobalExceptionHandler;
import com.rasitbank.assistant.liveconfig.domain.dto.KnowledgeBaseResponse;
import com.rasitbank.assistant.liveconfig.domain.dto.KnowledgeBaseSource;
import com.rasitbank.assistant.liveconfig.domain.dto.KnowledgeBaseStatus;
import com.rasitbank.assistant.liveconfig.domain.dto.LiveConfigResponse;
import com.rasitbank.assistant.liveconfig.service.LiveConfigService;
import com.rasitbank.assistant.retrieval.service.RerankerStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Canlı yapılandırma ucunun JSON alan adlarını ve salt okunur olduğunu (POST yok) doğrular.
 */
@WebMvcTest(LiveConfigController.class)
@Import(GlobalExceptionHandler.class)
class LiveConfigControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private LiveConfigService liveConfigService;

    @Test
    void givenLiveConfig_whenGet_thenJsonFields() {
        // Arrange
        given(liveConfigService.getLiveConfig()).willReturn(new LiveConfigResponse("qwen3.5:9b", "bge-m3", 1024,
                GenerationMode.RAG, RetrievalStrategy.RERANK, 20, 5, true, RerankerStatus.UP,
                new KnowledgeBaseResponse(KnowledgeBaseStatus.READY, 1, List.of(new KnowledgeBaseSource(10001,
                        "Raşit Bankası Ürün ve Hizmet Rehberi", "2026.10")))));

        // Act
        var result = mvc.get().uri("/api/assistant/live-config").exchange();

        // Assert
        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.chatModel").isEqualTo("qwen3.5:9b");
        assertThat(result).bodyJson().extractingPath("$.embeddingModel").isEqualTo("bge-m3");
        assertThat(result).bodyJson().extractingPath("$.embeddingDimensions").isEqualTo(1024);
        assertThat(result).bodyJson().extractingPath("$.generationMode").isEqualTo("RAG");
        assertThat(result).bodyJson().extractingPath("$.retrievalStrategy").isEqualTo("RERANK");
        assertThat(result).bodyJson().extractingPath("$.candidates").isEqualTo(20);
        assertThat(result).bodyJson().extractingPath("$.topK").isEqualTo(5);
        assertThat(result).bodyJson().extractingPath("$.queryRewriteEnabled").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.rerankerStatus").isEqualTo("UP");
        assertThat(result).bodyJson().extractingPath("$.knowledgeBase.status").isEqualTo("READY");
        assertThat(result).bodyJson().extractingPath("$.knowledgeBase.searchableSourceCount").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.knowledgeBase.sources[0].sourceId").isEqualTo(10001);
        assertThat(result).bodyJson().extractingPath("$.knowledgeBase.sources[0].name")
                .isEqualTo("Raşit Bankası Ürün ve Hizmet Rehberi");
        assertThat(result).bodyJson().extractingPath("$.knowledgeBase.sources[0].version").isEqualTo("2026.10");
    }

    @Test
    void givenUnavailableKnowledgeBase_whenGet_thenNullEmbeddingFieldsAreExplicitNulls() {
        // Arrange
        given(liveConfigService.getLiveConfig()).willReturn(new LiveConfigResponse("qwen3.5:9b", null, null,
                GenerationMode.LLM_ONLY, RetrievalStrategy.RERANK, 20, 5, true, RerankerStatus.NOT_USED,
                KnowledgeBaseResponse.unavailable()));

        // Act
        var result = mvc.get().uri("/api/assistant/live-config").exchange();

        // Assert
        assertThat(result).bodyJson().extractingPath("$.generationMode").isEqualTo("LLM_ONLY");
        assertThat(result).bodyJson().extractingPath("$.rerankerStatus").isEqualTo("NOT_USED");
        assertThat(result).bodyJson().extractingPath("$.knowledgeBase.status").isEqualTo("UNAVAILABLE");
        assertThat(result).bodyJson().extractingPath("$.knowledgeBase.searchableSourceCount").isNull();
        assertThat(result).bodyJson().extractingPath("$.embeddingModel").isNull();
        assertThat(result).bodyJson().extractingPath("$.knowledgeBase.sources").isEqualTo(List.of());
    }

    @Test
    void givenLiveConfig_whenPost_thenMethodNotAllowed() {
        // Act
        var result = mvc.post().uri("/api/assistant/live-config").exchange();

        // Assert
        assertThat(result).hasStatus(405);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("METHOD_NOT_ALLOWED");
    }
}
