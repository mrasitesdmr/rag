package com.rasitbank.retrieval.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.rasitbank.retrieval.embedding.client.EmbeddingClient;
import com.rasitbank.retrieval.exception.ConflictException;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.rasitbank.retrieval.search.domain.dto.SearchHitResponse;
import com.rasitbank.retrieval.search.domain.dto.SearchResponse;
import com.rasitbank.retrieval.search.repository.ChunkSearchRepository;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Arama servisinin kontrol sırasını, model çağrısını, repository'ye giden değerleri ve logunu sahte bağımlılıklarla doğrular.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SearchServiceTest {

    private static final String MODEL = "bge-m3@1024";

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private ChunkSearchRepository chunkSearchRepository;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(workspaceService, sourceRepository, embeddingClient, chunkSearchRepository);
        org.mockito.Mockito.lenient().when(embeddingClient.modelLabel()).thenReturn(MODEL);
    }

    private static SearchHitResponse hit(long chunkId) {
        return new SearchHitResponse(chunkId, 0, 1, "Başlık", "içerik", 10001, "Rehber", "2026.10", 0.25);
    }

    @Test
    void givenQueryWithSurroundingSpaces_whenSearchChunks_thenEmbedsStrippedQueryKeepingCaseAndTurkishLetters() {
        // Arrange
        given(sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull(42)).willReturn(true);
        given(embeddingClient.embed(anyList())).willReturn(List.<float[]>of(new float[] {1f}));
        given(chunkSearchRepository.findNearest(anyLong(), any(), anyString(), anyInt())).willReturn(List.of());

        // Act
        searchService.searchChunks(42, "  Kartımı KAYBETTİM  ", 5);

        // Assert
        then(embeddingClient).should().embed(List.of("Kartımı KAYBETTİM"));
    }

    @Test
    void givenNoSourceWithActiveVersion_whenSearchChunks_thenReturnsNoHitsWithoutCallingEmbeddingModel() {
        // Arrange
        given(sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull(42)).willReturn(false);

        // Act
        SearchResponse response = searchService.searchChunks(42, "soru", 5);

        // Assert
        assertThat(response.hits()).isEmpty();
        then(embeddingClient).should(never()).embed(anyList());
        then(chunkSearchRepository).shouldHaveNoInteractions();
    }

    @Test
    void givenConfiguredModelDiffersFromWorkspace_whenSearchChunks_thenThrowsEmbeddingModelMismatchWithoutEmbedding() {
        // Arrange
        willThrow(new ConflictException("EMBEDDING_MODEL_MISMATCH", "differs")).given(workspaceService)
                .ensureEmbeddingModelMatches(42, MODEL);

        // Act
        Throwable thrown = catchThrowable(() -> searchService.searchChunks(42, "soru", 5));

        // Assert
        assertThat(thrown).isInstanceOf(ConflictException.class).extracting("errorCode")
                .isEqualTo("EMBEDDING_MODEL_MISMATCH");
        then(embeddingClient).should(never()).embed(anyList());
        then(chunkSearchRepository).shouldHaveNoInteractions();
    }

    @Test
    void givenUnknownWorkspace_whenSearchChunks_thenThrowsWorkspaceNotFound() {
        // Arrange
        willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "none")).given(workspaceService)
                .ensureWorkspaceExists(7);

        // Act
        Throwable thrown = catchThrowable(() -> searchService.searchChunks(7, "soru", 5));

        // Assert
        assertThat(thrown).isInstanceOf(NotFoundException.class).extracting("errorCode")
                .isEqualTo("WORKSPACE_NOT_FOUND");
        then(embeddingClient).should(never()).embed(anyList());
        then(chunkSearchRepository).shouldHaveNoInteractions();
    }

    @Test
    void givenEmbeddingUnavailable_whenSearchChunks_thenThrowsEmbeddingUnavailableAndDoesNotQuery() {
        // Arrange
        given(sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull(42)).willReturn(true);
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE", "down"));

        // Act
        Throwable thrown = catchThrowable(() -> searchService.searchChunks(42, "soru", 5));

        // Assert
        assertThat(thrown).isInstanceOf(ServiceUnavailableException.class).extracting("errorCode")
                .isEqualTo("EMBEDDING_UNAVAILABLE");
        then(chunkSearchRepository).shouldHaveNoInteractions();
    }

    @Test
    void givenTopK5_whenSearchChunks_thenQueriesWithQueryVectorModelLabelAndTopK() {
        // Arrange
        float[] vector = {0.5f, 0.25f};
        given(sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull(42)).willReturn(true);
        given(embeddingClient.embed(List.of("soru"))).willReturn(List.<float[]>of(vector));
        given(chunkSearchRepository.findNearest(42, vector, MODEL, 5)).willReturn(List.of(hit(3), hit(1), hit(2)));

        // Act
        SearchResponse response = searchService.searchChunks(42, "soru", 5);

        // Assert
        assertThat(response.hits()).extracting(SearchHitResponse::chunkId).containsExactly(3L, 1L, 2L);
    }

    @Test
    void givenThreeHits_whenSearchChunks_thenLogsLengthTopKAndHitCountButNotQuery(CapturedOutput output) {
        // Arrange
        given(sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull(42)).willReturn(true);
        given(embeddingClient.embed(anyList())).willReturn(List.<float[]>of(new float[] {1f}));
        given(chunkSearchRepository.findNearest(anyLong(), any(), anyString(), anyInt()))
                .willReturn(List.of(hit(1), hit(2), hit(3)));

        // Act
        searchService.searchChunks(42, "Kartımı kaybettim, ne yapmalıyım?", 5);

        // Assert
        assertThat(output.getOut()).contains("Search completed workspaceId=42 queryLength=33 topK=5 hitCount=3");
        assertThat(output.getOut()).doesNotContain("Kartımı");
        java.util.regex.Matcher durations = java.util.regex.Pattern
                .compile("embeddingMs=(\\d+) sqlMs=(\\d+) durationMs=(\\d+)").matcher(output.getOut());
        assertThat(durations.find()).isTrue();
        assertThat(Long.parseLong(durations.group(3))).isBetween(0L, 5_000L);
    }

    @Test
    void givenEmptyKnowledgeBase_whenSearchChunks_thenLogsZeroHitsAndZeroModelTime(CapturedOutput output) {
        // Arrange
        given(sourceRepository.existsByWorkspaceIdAndActiveVersionIdIsNotNull(42)).willReturn(false);

        // Act
        searchService.searchChunks(42, "  soru  ", 5);

        // Assert
        assertThat(output.getOut())
                .contains("Search completed workspaceId=42 queryLength=4 topK=5 hitCount=0 embeddingMs=0 sqlMs=0");
    }
}
