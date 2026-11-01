package com.rasitbank.retrieval.chunk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.fixture.ChunkFixtures;
import com.rasitbank.retrieval.fixture.SearchFixtures;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.source.integration.AbstractReadIntegrationTest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Parça listesinin sunucu tarafı filtresini, sayfalamasını, güncel sürüm kapsamını ve tek parça ucunu gerçek
 * veritabanıyla doğrular.
 */
class ChunkListIntegrationTest extends AbstractReadIntegrationTest {

    private static String enc(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private static List<Long> ids(JsonNode page) {
        List<Long> result = new ArrayList<>();
        page.get("content").forEach(item -> result.add(item.get("chunkId").asLong()));
        return result;
    }

    private long singleVersionSource() {
        SearchFixtures.insertSource(jdbc, 10001, "Kaynak");
        return SearchFixtures.insertVersion(jdbc, 10001, "2026.10", true);
    }

    @Test
    void givenGuide154Chunks_whenListChunksPage1Size12_thenChunkNo12To23() {
        // Arrange
        IngestionJobResponse job = upload("Rehber", "2026.10", "rehber.pdf", guideBytes());
        awaitJobStage(job.jobId(), "DONE");

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/" + job.sourceId() + "/chunks?page=1&size=12");

        // Assert
        List<Integer> chunkNos = new ArrayList<>();
        page.get("content").forEach(item -> chunkNos.add(item.get("chunkNo").asInt()));
        assertThat(chunkNos).containsExactly(12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
        assertThat(page.get("totalElements").asLong()).isEqualTo(154);
        assertThat(page.get("totalPages").asInt()).isEqualTo(13);
        assertThat(page.get("page").asInt()).isEqualTo(1);
        assertThat(page.get("size").asInt()).isEqualTo(12);
    }

    @Test
    void givenGuide_whenListChunksWithGuideWord_thenFilteredPageMatchesAllRowsContainingIt() {
        // Arrange
        IngestionJobResponse job = upload("Rehber", "2026.10", "rehber.pdf", guideBytes());
        awaitJobStage(job.jobId(), "DONE");

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/" + job.sourceId() + "/chunks?size=100&query="
                + enc("asgari ödeme"));

        // Assert
        Locale turkish = Locale.forLanguageTag("tr");
        long expected = jdbc.queryForList("select heading || ' ' || content from chunks", String.class).stream()
                .filter(text -> text.replaceAll("\\s+", " ").toLowerCase(turkish).contains("asgari ödeme")).count();
        assertThat(expected).isPositive();
        assertThat(page.get("totalElements").asLong()).isEqualTo(expected);
        page.get("content").forEach(item -> assertThat((item.get("heading").asString() + " "
                + item.get("content").asString()).replaceAll("\\s+", " ").toLowerCase(turkish))
                .contains("asgari ödeme"));
    }

    @Test
    void givenFourChunks_whenListChunksWithSpacedUpperCaseQuery_thenMatchesHeadingAndTextAcrossLineBreak() {
        // Arrange
        long versionId = singleVersionSource();
        ChunkFixtures.insertChunk(jdbc, 1, 10001, versionId, 0, "Genel", "Asgari\nödeme tutarı");
        ChunkFixtures.insertChunk(jdbc, 2, 10001, versionId, 1, "ASGARİ ÖDEME", "kart");
        ChunkFixtures.insertChunk(jdbc, 3, 10001, versionId, 2, "Genel", "asgari tutar");
        ChunkFixtures.insertChunk(jdbc, 4, 10001, versionId, 3, "Genel", "Asgari-ödeme");

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/10001/chunks?query=" + enc("  ASGARİ   ödeme "));

        // Assert
        assertThat(ids(page)).containsExactly(1L, 2L);
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void givenChunkKisaVadeli_whenListChunksWithTurkishLowerCase_thenMatchesOnlyDottedlessI() {
        // Arrange
        long versionId = singleVersionSource();
        ChunkFixtures.insertChunk(jdbc, 1, 10001, versionId, 0, "Genel", "KISA VADELİ");

        // Act
        JsonNode matching = getJson("/api/workspaces/42/sources/10001/chunks?query=" + enc("kısa vadeli"));
        JsonNode dotted = getJson("/api/workspaces/42/sources/10001/chunks?query=" + enc("kisa"));

        // Assert
        assertThat(matching.get("totalElements").asLong()).isEqualTo(1);
        assertThat(dotted.get("totalElements").asLong()).isZero();
    }

    @Test
    void givenQueryWithPercentAndUnderscore_whenListChunks_thenTakenLiterally() {
        // Arrange
        long versionId = singleVersionSource();
        ChunkFixtures.insertChunk(jdbc, 1, 10001, versionId, 0, "Faiz", "oran %5 yıllık");
        ChunkFixtures.insertChunk(jdbc, 2, 10001, versionId, 1, "Faiz", "oran 7 yıllık");

        // Act
        JsonNode percent = getJson("/api/workspaces/42/sources/10001/chunks?query=" + enc("%"));
        JsonNode underscore = getJson("/api/workspaces/42/sources/10001/chunks?query=" + enc("_"));

        // Assert
        assertThat(ids(percent)).containsExactly(1L);
        assertThat(underscore.get("totalElements").asLong()).isZero();
    }

    @Test
    void givenBlankQuery_whenListChunks_thenNoFilterApplied() {
        // Arrange
        long versionId = singleVersionSource();
        ChunkFixtures.insertChunk(jdbc, 1, 10001, versionId, 0, "A", "bir");
        ChunkFixtures.insertChunk(jdbc, 2, 10001, versionId, 1, "B", "iki");

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/10001/chunks?query=" + enc("   "));

        // Assert
        assertThat(ids(page)).containsExactly(1L, 2L);
    }

    @Test
    void givenQueryWithNoMatch_whenListChunks_thenEmptyPageWithZeroTotals() {
        // Arrange
        long versionId = singleVersionSource();
        ChunkFixtures.insertChunk(jdbc, 1, 10001, versionId, 0, "A", "bir");

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/10001/chunks?query=yok");

        // Assert
        assertThat(page.get("content").size()).isZero();
        assertThat(page.get("totalElements").asLong()).isZero();
    }

    @Test
    void givenTwoVersionsActiveIsSecond_whenListChunks_thenOnlySecondVersionChunks() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Kaynak");
        long first = SearchFixtures.insertVersion(jdbc, 10001, "1", false);
        long second = SearchFixtures.insertVersion(jdbc, 10001, "2", true);
        ChunkFixtures.insertChunk(jdbc, 1, 10001, first, 0, "Eski", "eski");
        ChunkFixtures.insertChunk(jdbc, 2, 10001, second, 0, "Yeni", "yeni");
        ChunkFixtures.insertChunk(jdbc, 3, 10001, second, 1, "Yeni", "yeni iki");

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/10001/chunks");

        // Assert
        assertThat(ids(page)).containsExactly(2L, 3L);
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void givenSourceWithoutActiveVersion_whenListChunks_thenEmptyPage() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Sürümsüz");
        ChunkFixtures.insertFailedJob(jdbc, 10001, "1", "a.pdf", 1, new byte[] {1});

        // Act
        JsonNode page = getJson("/api/workspaces/42/sources/10001/chunks");

        // Assert
        assertThat(page.get("totalElements").asLong()).isZero();
        assertThat(page.get("content").size()).isZero();
    }

    @Test
    void givenUnknownSource_whenListChunks_thenReturns404SourceNotFound() {
        // Act
        var response = get("/api/workspaces/42/sources/99999/chunks");

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenQueryOf201Chars_whenListChunksOverHttp_thenReturns400OnQuery() {
        // Arrange
        singleVersionSource();

        // Act
        var response = get("/api/workspaces/42/sources/10001/chunks?query=" + "a".repeat(201));

        // Assert
        JsonNode body = JSON.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(body.get("errorCode").asString()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.get("violations").get(0).get("field").asString()).isEqualTo("query");
    }

    @Test
    void givenChunk_whenGetChunk_thenReturnsTextFirstEightValuesAndSource() {
        // Arrange
        long versionId = singleVersionSource();
        float[] embedding = SearchFixtures.vector(0.5f, 0.25f);
        for (int index = 2; index < 8; index++) {
            embedding[index] = index / 10f;
        }
        embedding[8] = 0.9f;
        ChunkFixtures.insertChunk(jdbc, 151, 10001, versionId, 31, 19, "2.16 Kayıp ya da çalıntı bildirimi",
                "Kartınızı kaybederseniz\nhemen arayın.", 297, embedding);

        // Act
        JsonNode chunk = getJson("/api/workspaces/42/chunks/151");

        // Assert
        assertThat(chunk.get("chunkId").asLong()).isEqualTo(151);
        assertThat(chunk.get("chunkNo").asInt()).isEqualTo(31);
        assertThat(chunk.get("pageNumber").asInt()).isEqualTo(19);
        assertThat(chunk.get("heading").asString()).isEqualTo("2.16 Kayıp ya da çalıntı bildirimi");
        assertThat(chunk.get("content").asString()).isEqualTo("Kartınızı kaybederseniz\nhemen arayın.");
        assertThat(chunk.get("tokenCount").asInt()).isEqualTo(297);
        assertThat(chunk.get("embeddingModel").asString()).isEqualTo("bge-m3@1024");
        assertThat(chunk.get("embeddingDimensions").asInt()).isEqualTo(1024);
        List<Float> preview = new ArrayList<>();
        chunk.get("embeddingPreview").forEach(value -> preview.add((float) value.asDouble()));
        assertThat(preview).containsExactly(0.5f, 0.25f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f);
        assertThat(chunk.get("sourceId").asLong()).isEqualTo(10001);
        assertThat(chunk.get("sourceName").asString()).isEqualTo("Kaynak");
        assertThat(chunk.get("version").asString()).isEqualTo("2026.10");
    }

    @Test
    void givenChunkOfOldVersion_whenGetChunk_thenStillReturnedWithItsOwnVersion() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Kaynak");
        long old = SearchFixtures.insertVersion(jdbc, 10001, "1.0", false);
        SearchFixtures.insertVersion(jdbc, 10001, "2.0", true);
        ChunkFixtures.insertChunk(jdbc, 1, 10001, old, 0, "Eski", "eski metin");

        // Act
        JsonNode chunk = getJson("/api/workspaces/42/chunks/1");

        // Assert
        assertThat(chunk.get("version").asString()).isEqualTo("1.0");
    }

    @Test
    void givenUnknownChunk_whenGetChunk_thenReturns404ChunkNotFound() {
        // Act
        var response = get("/api/workspaces/42/chunks/999999");

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("CHUNK_NOT_FOUND");
    }

    @Test
    void givenWorkspace43_whenGetChunk_thenReturns404WorkspaceNotFound() {
        // Act
        var response = get("/api/workspaces/43/chunks/1");

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("WORKSPACE_NOT_FOUND");
    }
}
