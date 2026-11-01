package com.rasitbank.retrieval.search.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.rasitbank.retrieval.exception.ConflictException;
import com.rasitbank.retrieval.exception.GlobalExceptionHandler;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.rasitbank.retrieval.search.domain.dto.SearchHitResponse;
import com.rasitbank.retrieval.search.domain.dto.SearchResponse;
import com.rasitbank.retrieval.search.service.SearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Arama ucunun gövde doğrulamasını, hata gövdelerini ve yanıt şeklini sözleşmeye göre doğrular.
 */
@WebMvcTest(controllers = SearchController.class)
@Import(GlobalExceptionHandler.class)
class SearchControllerTest {

    private static final String URI = "/api/workspaces/42/search";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SearchService searchService;

    private MockMvcTester.MockMvcRequestBuilder post(String body) {
        return mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void stubEmptyResult() {
        given(searchService.searchChunks(anyLong(), anyString(), anyInt())).willReturn(SearchResponse.empty());
    }

    @Test
    void givenTwoHits_whenSearchChunks_thenReturns200WithAllHitFieldsInOrder() {
        // Arrange
        given(searchService.searchChunks(42, "kart", 5)).willReturn(new SearchResponse(List.of(
                new SearchHitResponse(29, 3, 12, "2.16 Kayıp ya da çalıntı bildirimi", "Kartınızı kapatın.", 10001,
                        "Rehber", "2026.10", 0.298),
                new SearchHitResponse(36, 4, 13, "SSS", "Aidat.", 10002, "El Kitabı", "v1", 0.35))));

        // Act
        var result = post("{\"query\":\"kart\",\"topK\":5}").exchange();

        // Assert
        assertThat(result).hasStatus(200).hasContentType("application/json");
        assertThat(result).bodyJson().extractingPath("$.hits[0].chunkId").isEqualTo(29);
        assertThat(result).bodyJson().extractingPath("$.hits[0].chunkNo").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.hits[0].pageNumber").isEqualTo(12);
        assertThat(result).bodyJson().extractingPath("$.hits[0].heading")
                .isEqualTo("2.16 Kayıp ya da çalıntı bildirimi");
        assertThat(result).bodyJson().extractingPath("$.hits[0].content").isEqualTo("Kartınızı kapatın.");
        assertThat(result).bodyJson().extractingPath("$.hits[0].sourceId").isEqualTo(10001);
        assertThat(result).bodyJson().extractingPath("$.hits[0].sourceName").isEqualTo("Rehber");
        assertThat(result).bodyJson().extractingPath("$.hits[0].version").isEqualTo("2026.10");
        assertThat(result).bodyJson().extractingPath("$.hits[0].distance").isEqualTo(0.298);
        assertThat(result).bodyJson().extractingPath("$.hits[1].chunkId").isEqualTo(36);
        assertThat(result).bodyJson().doesNotHavePath("$.hits[0].embedding");
    }

    @Test
    void givenTopKZero_whenSearchChunks_thenReturns400ValidationFailedOnTopK() {
        // Act
        var result = post("{\"query\":\"kart\",\"topK\":0}").exchange();

        // Assert
        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("topK");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must be between 1 and 50");
        then(searchService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 51, 1000})
    void givenTopKOutsideRange_whenSearchChunks_thenReturns400OnTopK(int topK) {
        // Act
        var result = post("{\"query\":\"kart\",\"topK\":" + topK + "}").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("topK");
        then(searchService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    void givenTopKAtBoundary_whenSearchChunks_thenReturns200(int topK) {
        // Arrange
        stubEmptyResult();

        // Act
        var result = post("{\"query\":\"kart\",\"topK\":" + topK + "}").exchange();

        // Assert
        assertThat(result).hasStatus(200);
        then(searchService).should().searchChunks(42, "kart", topK);
    }

    @Test
    void givenMissingTopK_whenSearchChunks_thenReturns400MustNotBeNull() {
        // Act
        var result = post("{\"query\":\"kart\"}").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("topK");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"topK\":5}", "{\"query\":\"\",\"topK\":5}", "{\"query\":\"   \",\"topK\":5}"})
    void givenBlankQuery_whenSearchChunks_thenReturns400OnQuery(String body) {
        // Act
        var result = post(body).exchange();

        // Assert
        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("query");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must not be blank");
        then(searchService).shouldHaveNoInteractions();
    }

    @Test
    void givenQueryOf1001Chars_whenSearchChunks_thenReturns400OnQuery() {
        // Act
        var result = post("{\"query\":\"" + "a".repeat(1001) + "\",\"topK\":5}").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("query");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message")
                .isEqualTo("size must be at most 1000 characters");
        then(searchService).shouldHaveNoInteractions();
    }

    @Test
    void givenQueryOf1000TurkishChars_whenSearchChunks_thenReturns200() {
        // Arrange
        stubEmptyResult();
        String query = "ş".repeat(1000);

        // Act
        var result = post("{\"query\":\"" + query + "\",\"topK\":5}").exchange();

        // Assert
        assertThat(result).hasStatus(200);
        then(searchService).should().searchChunks(42, query, 5);
    }

    @Test
    void givenBlankQueryAndTopK51_whenSearchChunks_thenReturnsBothViolations() {
        // Act
        var result = post("{\"query\":\"  \",\"topK\":51}").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations").asList().hasSize(2);
        assertThat(result).bodyJson().extractingPath("$.violations[*].field").asList()
                .containsExactlyInAnyOrder("query", "topK");
    }

    @Test
    void givenTextPlainBody_whenSearchChunks_thenReturns415() {
        // Act
        var result = mvc.post().uri(URI).contentType(MediaType.TEXT_PLAIN).content("kart").exchange();

        // Assert
        assertThat(result).hasStatus(415);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void givenMalformedJson_whenSearchChunks_thenReturns400BadRequest() {
        // Act
        var result = post("{\"query\":").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void givenTopKAsText_whenSearchChunks_thenReturns400BadRequest() {
        // Act
        var result = post("{\"query\":\"a\",\"topK\":\"beş\"}").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void givenUnknownExtraField_whenSearchChunks_thenIgnoresItAndReturns200() {
        // Arrange
        stubEmptyResult();

        // Act
        var result = post("{\"query\":\"kart\",\"topK\":5,\"mode\":\"VECTOR\"}").exchange();

        // Assert
        assertThat(result).hasStatus(200);
    }

    @Test
    void givenWorkspaceNotFound_whenSearchChunks_thenReturns404() {
        // Arrange
        given(searchService.searchChunks(42, "kart", 5))
                .willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "none"));

        // Act
        var result = post("{\"query\":\"kart\",\"topK\":5}").exchange();

        // Assert
        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenModelMismatch_whenSearchChunks_thenReturns409() {
        // Arrange
        given(searchService.searchChunks(42, "kart", 5))
                .willThrow(new ConflictException("EMBEDDING_MODEL_MISMATCH", "differs"));

        // Act
        var result = post("{\"query\":\"kart\",\"topK\":5}").exchange();

        // Assert
        assertThat(result).hasStatus(409);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("EMBEDDING_MODEL_MISMATCH");
    }

    @Test
    void givenEmbeddingUnavailable_whenSearchChunks_thenReturns503() {
        // Arrange
        given(searchService.searchChunks(42, "kart", 5))
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE", "down"));

        // Act
        var result = post("{\"query\":\"kart\",\"topK\":5}").exchange();

        // Assert
        assertThat(result).hasStatus(503).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("EMBEDDING_UNAVAILABLE");
    }
}
