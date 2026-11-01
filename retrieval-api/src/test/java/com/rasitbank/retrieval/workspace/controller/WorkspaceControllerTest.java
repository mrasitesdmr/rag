package com.rasitbank.retrieval.workspace.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.exception.GlobalExceptionHandler;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.workspace.domain.dto.WorkspaceResponse;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Çalışma alanı ucunun sözleşmedeki alan kümesini ve 404 hata yanıtını doğrular.
 */
@WebMvcTest(controllers = WorkspaceController.class)
@Import(GlobalExceptionHandler.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private WorkspaceService workspaceService;

    @Test
    void givenWorkspace42_whenGetWorkspace_thenReturnsModelChunkingAndIndex() {
        // Arrange
        given(workspaceService.getWorkspaceResponse(42)).willReturn(new WorkspaceResponse(42,
                "Raşit Bankası · Dijital Asistan", "bge-m3", 1024, 1600, 2000, 200, "HNSW", "COSINE"));

        // Act
        var result = mvc.get().uri("/api/workspaces/42").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$").asMap().containsOnlyKeys("workspaceId", "name",
                "embeddingModel", "embeddingDimensions", "targetChunkChars", "maxChunkChars", "overlapChars",
                "vectorIndexType", "distanceMetric");
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Raşit Bankası · Dijital Asistan");
        assertThat(result).bodyJson().extractingPath("$.embeddingModel").isEqualTo("bge-m3");
        assertThat(result).bodyJson().extractingPath("$.embeddingDimensions").isEqualTo(1024);
        assertThat(result).bodyJson().extractingPath("$.overlapChars").isEqualTo(200);
        assertThat(result).bodyJson().extractingPath("$.vectorIndexType").isEqualTo("HNSW");
        assertThat(result).bodyJson().extractingPath("$.distanceMetric").isEqualTo("COSINE");
    }

    @Test
    void givenWorkspace43_whenGetWorkspace_thenReturns404WorkspaceNotFound() {
        // Arrange
        given(workspaceService.getWorkspaceResponse(43)).willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok"));

        // Act
        var result = mvc.get().uri("/api/workspaces/43").exchange();

        // Assert
        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
    }
}
