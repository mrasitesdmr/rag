package com.rasitbank.retrieval.source.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.rasitbank.retrieval.fixture.ChunkFixtures;
import com.rasitbank.retrieval.fixture.PdfFixtures;
import com.rasitbank.retrieval.fixture.SearchFixtures;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Gerçek rehber içe alındıktan sonra çalışma alanı, kaynak listesi, tek kaynak ve içe alma işi uçlarının ekranın
 * ihtiyaç duyduğu değerleri verdiğini doğrular.
 */
class SourceReadIntegrationTest extends AbstractReadIntegrationTest {

    private static final String GUIDE_NAME = "Raşit Bankası Ürün ve Hizmet Rehberi";
    private static final String GUIDE_FILE = "Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf";

    private JsonNode uploadGuideAndWait() {
        IngestionJobResponse job = upload(GUIDE_NAME, "2026.10", GUIDE_FILE, guideBytes());
        awaitJobStage(job.jobId(), "DONE");
        return getJson("/api/workspaces/42/sources").get("content").get(0);
    }

    @Test
    void givenWorkspace42_whenGetWorkspace_thenReturnsModelChunkingAndIndex() {
        // Act
        JsonNode workspace = getJson("/api/workspaces/42");

        // Assert
        assertThat(workspace.get("workspaceId").asLong()).isEqualTo(42);
        assertThat(workspace.get("name").asString()).isEqualTo("Raşit Bankası · Dijital Asistan");
        assertThat(workspace.get("embeddingModel").asString()).isEqualTo("bge-m3");
        assertThat(workspace.get("embeddingDimensions").asInt()).isEqualTo(1024);
        assertThat(workspace.get("targetChunkChars").asInt()).isEqualTo(1600);
        assertThat(workspace.get("maxChunkChars").asInt()).isEqualTo(2000);
        assertThat(workspace.get("overlapChars").asInt()).isEqualTo(200);
        assertThat(workspace.get("vectorIndexType").asString()).isEqualTo("HNSW");
        assertThat(workspace.get("distanceMetric").asString()).isEqualTo("COSINE");
    }

    @Test
    void givenWorkspace43_whenGetWorkspace_thenReturns404WorkspaceNotFound() {
        // Act
        var response = get("/api/workspaces/43");

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenGuideIngested_whenListSources_thenItemCarriesVersionCountsAndLatestJob() {
        // Act
        JsonNode item = uploadGuideAndWait();

        // Assert
        List<Integer> tokenCounts = jdbc.queryForList("select token_count from chunks", Integer.class);
        long expectedAverage = Math.round(tokenCounts.stream().mapToInt(Integer::intValue).average().orElseThrow());
        assertThat(item.get("name").asString()).isEqualTo(GUIDE_NAME);
        assertThat(item.get("fileName").asString()).isEqualTo(GUIDE_FILE);
        assertThat(item.get("fileSizeBytes").asLong()).isEqualTo(guideBytes().length);
        assertThat(item.get("version").asString()).isEqualTo("2026.10");
        assertThat(item.get("pageCount").asInt()).isEqualTo(70);
        assertThat(item.get("chapterCount").asInt()).isEqualTo(11);
        assertThat(item.get("chunkCount").asInt()).isEqualTo(tokenCounts.size()).isEqualTo(154);
        assertThat(item.get("averageTokenCount").asLong()).isEqualTo(expectedAverage);
        assertThat(item.get("latestJob").get("stage").asString()).isEqualTo("DONE");
        String finishedAt = item.get("latestJob").get("finishedAt").asString();
        assertThat(Instant.parse(item.get("syncedAt").asString()))
                .isEqualTo(Instant.parse(finishedAt));
    }

    @Test
    void givenGuideIngested_whenGetSource_thenSameFieldsAsListItem() {
        // Arrange
        JsonNode listItem = uploadGuideAndWait();

        // Act
        JsonNode detail = getJson("/api/workspaces/42/sources/" + listItem.get("sourceId").asLong());

        // Assert
        assertThat(detail).isEqualTo(listItem);
    }

    @Test
    void givenUnknownSource_whenGetSource_thenReturns404SourceNotFound() {
        // Act
        var response = get("/api/workspaces/42/sources/99999");

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenWorkspace43_whenGetSource_thenReturns404WorkspaceNotFound() {
        // Act
        var response = get("/api/workspaces/43/sources/10001");

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenSourceWhoseOnlyJobFailed_whenListSources_thenFileFieldsFromJobAndCountsNull() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Başarısız belge");
        ChunkFixtures.insertFailedJob(jdbc, 10001, "3.1", "basarisiz.pdf", 777,
                new byte[] {1});

        // Act
        JsonNode item = getJson("/api/workspaces/42/sources").get("content").get(0);

        // Assert
        assertThat(item.get("fileName").asString()).isEqualTo("basarisiz.pdf");
        assertThat(item.get("fileSizeBytes").asLong()).isEqualTo(777);
        assertThat(item.get("version").asString()).isEqualTo("3.1");
        for (String field : List.of("pageCount", "chapterCount", "chunkCount", "averageTokenCount", "syncedAt")) {
            assertThat(item.has(field)).as(field + " is present").isTrue();
            assertThat(item.get(field).isNull()).as(field + " is null").isTrue();
        }
        assertThat(item.get("latestJob").get("stage").asString()).isEqualTo("FAILED");
        assertThat(item.get("latestJob").get("errorCode").asString()).isEqualTo("EMBEDDING_UNAVAILABLE");
    }

    @Test
    void givenSourceWithFailedAndRetriedJobs_whenListSources_thenLatestJobIsHighestId() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Yeniden denenen");
        long failedJobId = ChunkFixtures.insertFailedJob(jdbc, 10001, "1", "a.pdf", 5,
                new byte[] {1});
        long versionId = SearchFixtures.insertVersion(jdbc, 10001, "1", true);
        long doneJobId = ChunkFixtures.jobIdOfVersion(jdbc, versionId);

        // Act
        JsonNode item = getJson("/api/workspaces/42/sources").get("content").get(0);

        // Assert
        assertThat(doneJobId).isGreaterThan(failedJobId);
        assertThat(item.get("latestJob").get("jobId").asLong()).isEqualTo(doneJobId);
        assertThat(item.get("latestJob").get("stage").asString()).isEqualTo("DONE");
    }

    @Test
    void givenSourcesWithDifferentLatestJobs_whenListSources_thenEachItemGetsItsOwnJob() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Birinci");
        SearchFixtures.insertSource(jdbc, 10002, "İkinci");
        long firstJob = ChunkFixtures.insertFailedJob(jdbc, 10001, "1", "bir.pdf", 5,
                new byte[] {1});
        long secondJob = ChunkFixtures.insertFailedJob(jdbc, 10002, "2", "iki.pdf", 6,
                new byte[] {2});

        // Act
        JsonNode content = getJson("/api/workspaces/42/sources").get("content");

        // Assert
        assertThat(content.get(0).get("latestJob").get("jobId").asLong()).isEqualTo(firstJob);
        assertThat(content.get(0).get("fileName").asString()).isEqualTo("bir.pdf");
        assertThat(content.get(1).get("latestJob").get("jobId").asLong()).isEqualTo(secondJob);
        assertThat(content.get(1).get("fileName").asString()).isEqualTo("iki.pdf");
    }

    @Test
    void givenFailedThenRetriedSource_whenListIngestionJobs_thenNewestFirst() {
        // Arrange
        IngestionJobResponse first = failFirstJobThenRetryToDone();

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/" + first.sourceId() + "/ingestion-jobs");

        // Assert
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        assertThat(page.get("content").get(0).get("jobId").asLong()).isGreaterThan(first.jobId());
        assertThat(page.get("content").get(0).get("stage").asString()).isEqualTo("DONE");
        assertThat(page.get("content").get(1).get("jobId").asLong()).isEqualTo(first.jobId());
        assertThat(page.get("content").get(1).get("stage").asString()).isEqualTo("FAILED");
    }

    @Test
    void givenTwoJobs_whenListIngestionJobsSize1Page1_thenSecondNewestWithPageMetadata() {
        // Arrange
        IngestionJobResponse first = failFirstJobThenRetryToDone();

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/" + first.sourceId() + "/ingestion-jobs?page=1&size=1");

        // Assert
        assertThat(page.get("content").size()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("jobId").asLong()).isEqualTo(first.jobId());
        assertThat(page.get("page").asInt()).isEqualTo(1);
        assertThat(page.get("size").asInt()).isEqualTo(1);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
    }

    @Test
    void givenUnknownSource_whenListIngestionJobs_thenReturns404() {
        // Act
        var response = get("/api/workspaces/42/sources/99999/ingestion-jobs");

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenJobsOfTwoSources_whenListIngestionJobsOfFirst_thenOnlyItsJobs() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Birinci");
        SearchFixtures.insertSource(jdbc, 10002, "İkinci");
        long firstJob = ChunkFixtures.insertFailedJob(jdbc, 10001, "1", "bir.pdf", 5,
                new byte[] {1});
        ChunkFixtures.insertFailedJob(jdbc, 10002, "1", "iki.pdf", 5, new byte[] {1});

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/10001/ingestion-jobs");

        // Assert
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("jobId").asLong()).isEqualTo(firstJob);
    }

    private IngestionJobResponse failFirstJobThenRetryToDone() {
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE",
                        "down"));
        IngestionJobResponse first = upload("Rehber", "2026.10", "rehber.pdf", PdfFixtures.validPdf());
        awaitJobStage(first.jobId(), "FAILED");
        stubEmbeddingSuccess();
        IngestionJobResponse second = ingestionService.retryIngestion(42, first.sourceId());
        awaitJobStage(second.jobId(), "DONE");
        return first;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"/api/workspaces/43/sources",
            "/api/workspaces/43/sources/10001/file", "/api/workspaces/43/sources/10001/ingestion-jobs",
            "/api/workspaces/43/sources/10001/chunks"})
    void givenWorkspace43_whenReadEndpointCalled_thenReturns404WorkspaceNotFound(String path) {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Başka alanda gibi");

        // Act
        var response = get(path);

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("WORKSPACE_NOT_FOUND");
    }
}
