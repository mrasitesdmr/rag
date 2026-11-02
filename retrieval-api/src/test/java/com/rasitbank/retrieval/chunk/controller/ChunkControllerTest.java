package com.rasitbank.retrieval.chunk.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.chunk.domain.dto.ChunkResponse;
import com.rasitbank.retrieval.chunk.domain.dto.ChunkSummaryResponse;
import com.rasitbank.retrieval.chunk.service.ChunkService;
import com.rasitbank.retrieval.exception.GlobalExceptionHandler;
import com.rasitbank.retrieval.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Parça listesi ve tek parça uçlarının parametre doğrulamasını, gövde şeklini ve hata yanıtlarını doğrular.
 */
@WebMvcTest(controllers = ChunkController.class)
@Import(GlobalExceptionHandler.class)
class ChunkControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ChunkService chunkService;

    @Test
    void givenQueryOf201Chars_whenListChunks_thenReturns400OnQuery() {
        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/chunks?query=" + "a".repeat(201)).exchange();

        // Assert
        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("query");
    }

    @Test
    void givenQueryOf200Chars_whenListChunks_thenReturns200() {
        // Arrange
        String query = "a".repeat(200);
        given(chunkService.listChunks(42, 10001, query, 0, 20))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/chunks?query=" + query).exchange();

        // Assert
        assertThat(result).hasStatusOk();
    }

    @Test
    void givenPage1Size12AndQuery_whenListChunks_thenServiceGetsParamsAndBodyHasPageMetadata() {
        // Arrange
        ChunkSummaryResponse chunk = new ChunkSummaryResponse(151, 12, 19, "Başlık", "Metin", 297);
        given(chunkService.listChunks(42, 10001, "asgari ödeme", 1, 12))
                .willReturn(new PageImpl<>(List.of(chunk), PageRequest.of(1, 12), 25));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/chunks?query=asgari ödeme&page=1&size=12")
                .exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content[0]").asMap().containsOnlyKeys("chunkId", "chunkNo",
                "pageNumber", "heading", "content", "tokenCount");
        assertThat(result).bodyJson().extractingPath("$.content[0].chunkId").isEqualTo(151);
        assertThat(result).bodyJson().extractingPath("$.content[0].chunkNo").isEqualTo(12);
        assertThat(result).bodyJson().extractingPath("$.content[0].pageNumber").isEqualTo(19);
        assertThat(result).bodyJson().extractingPath("$.content[0].tokenCount").isEqualTo(297);
        assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(12);
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(25);
        assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
    }

    @Test
    void givenNoParams_whenListChunks_thenDefaultsNoQueryPage0Size20() {
        // Arrange
        given(chunkService.listChunks(org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(10001L),
                isNull(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(20)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/chunks").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(20);
    }

    @Test
    void givenSize101AndPageMinus1_whenListChunks_thenReturns400OnBothFields() {
        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/chunks?size=101&page=-1").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[*].field").asList()
                .containsExactlyInAnyOrder("size", "page");
    }

    @Test
    void givenUnknownSource_whenListChunks_thenReturns404SourceNotFound() {
        // Arrange
        given(chunkService.listChunks(42, 99, null, 0, 20)).willThrow(new NotFoundException("SOURCE_NOT_FOUND", "yok"));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/99/chunks").exchange();

        // Assert
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenChunk_whenGetChunk_thenReturnsContractFieldsWithEmbeddingPreview() {
        // Arrange
        given(chunkService.getChunk(42, 151)).willReturn(new ChunkResponse(151, 31, 19, "Başlık", "Metin", 297,
                "bge-m3@1024", 1024, new float[] {0.5f, 0.25f}, 10001, "Rehber", "2026.10"));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/chunks/151").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$").asMap().containsOnlyKeys("chunkId", "chunkNo",
                "pageNumber", "heading", "content", "tokenCount", "embeddingModel", "embeddingDimensions",
                "embeddingPreview", "sourceId", "sourceName", "version");
        assertThat(result).bodyJson().extractingPath("$.embeddingPreview").asList().containsExactly(0.5, 0.25);
        assertThat(result).bodyJson().extractingPath("$.embeddingDimensions").isEqualTo(1024);
        assertThat(result).bodyJson().extractingPath("$.sourceName").isEqualTo("Rehber");
    }

    @Test
    void givenUnknownChunk_whenGetChunk_thenReturns404ChunkNotFound() {
        // Arrange
        given(chunkService.getChunk(42, 9)).willThrow(new NotFoundException("CHUNK_NOT_FOUND", "yok"));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/chunks/9").exchange();

        // Assert
        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("CHUNK_NOT_FOUND");
    }

    @Test
    void givenInvalidTexts_whenFindChunkMatches_thenValidationFailed() {
        // Act
        var empty = mvc.post().uri("/api/workspaces/42/chunk-matches").contentType("application/json")
                .content("{\"texts\":[]}").exchange();
        var blank = mvc.post().uri("/api/workspaces/42/chunk-matches").contentType("application/json")
                .content("{\"texts\":[\"ok\",\"  \"]}").exchange();

        // Assert
        assertThat(empty).hasStatus(400);
        assertThat(empty).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(empty).bodyJson().extractingPath("$.violations[0].field").isEqualTo("texts");
        assertThat(empty).bodyJson().extractingPath("$.violations[0].message").isEqualTo("size must be between 1 and 500");
        assertThat(blank).hasStatus(400);
        assertThat(blank).bodyJson().extractingPath("$.violations[0].field").isEqualTo("texts[1]");
        assertThat(blank).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must not be blank");
    }

    @Test
    void givenMoreThan500Texts_whenFindChunkMatches_thenValidationFailed() {
        // Arrange
        String texts = String.join(",", java.util.Collections.nCopies(501, "\"a\""));

        // Act
        var result = mvc.post().uri("/api/workspaces/42/chunk-matches").contentType("application/json")
                .content("{\"texts\":[" + texts + "]}").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("texts");
    }

    @Test
    void givenUnknownWorkspace_whenFindChunkMatches_thenReturns404WorkspaceNotFound() {
        // Arrange
        given(chunkService.findChunkMatches(7, List.of("metin")))
                .willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "Workspace not found"));

        // Act
        var result = mvc.post().uri("/api/workspaces/7/chunk-matches").contentType("application/json")
                .content("{\"texts\":[\"metin\"]}").exchange();

        // Assert
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenValidTexts_whenFindChunkMatches_thenReturnsMatchesBody() {
        // Arrange
        given(chunkService.findChunkMatches(42, List.of("metin"))).willReturn(new com.rasitbank.retrieval.chunk.domain.dto.ChunkMatchesResponse(
                List.of(new com.rasitbank.retrieval.chunk.domain.dto.ChunkTextMatchResponse("metin", List.of(
                        new com.rasitbank.retrieval.chunk.domain.dto.MatchedChunkResponse(151, 10001, "Rehber", "2026.10", 19))))));

        // Act
        var result = mvc.post().uri("/api/workspaces/42/chunk-matches").contentType("application/json")
                .content("{\"texts\":[\"metin\"]}").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.matches[0].chunks[0].chunkId").isEqualTo(151);
        assertThat(result).bodyJson().extractingPath("$.matches[0].chunks[0].pageNumber").isEqualTo(19);
        assertThat(result).bodyJson().extractingPath("$.matches[0].text").isEqualTo("metin");
    }
}
