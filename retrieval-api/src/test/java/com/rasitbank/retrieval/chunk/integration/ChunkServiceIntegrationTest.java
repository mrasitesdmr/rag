package com.rasitbank.retrieval.chunk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.chunk.domain.dto.ChunkMatchesResponse;
import com.rasitbank.retrieval.chunk.domain.dto.ChunkTextMatchResponse;
import com.rasitbank.retrieval.chunk.domain.dto.MatchedChunkResponse;
import com.rasitbank.retrieval.chunk.service.ChunkService;
import com.rasitbank.retrieval.fixture.ChunkFixtures;
import com.rasitbank.retrieval.fixture.SearchFixtures;
import com.rasitbank.retrieval.source.integration.AbstractReadIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Golden set cevap cümlelerinin güncel sürüm parçalarında bulunmasını gerçek veritabanıyla doğrular.
 */
class ChunkServiceIntegrationTest extends AbstractReadIntegrationTest {

    private static final String SENTENCE = "Kartınızı kaybettiyseniz ya da kartınız çalındıysa vakit kaybetmeden bildirin.";

    @Autowired
    private ChunkService chunkService;

    private long activeVersion(long sourceId, String name) {
        SearchFixtures.insertSource(jdbc, sourceId, name);
        return SearchFixtures.insertVersion(jdbc, sourceId, "2026.10", true);
    }

    private static List<Long> chunkIds(ChunkTextMatchResponse match) {
        return match.chunks().stream().map(MatchedChunkResponse::chunkId).toList();
    }

    @Test
    void givenTextDifferingInCaseAndLineBreaks_whenFindChunkMatches_thenReturnsChunk() {
        // Arrange
        long versionId = activeVersion(10001, "Rehber");
        ChunkFixtures.insertChunk(jdbc, 151, 10001, versionId, 0, 19, "Kayıp kart",
                "Kartınızı\nKAYBETTİYSENİZ ya da kartınız  çalındıysa\nvakit kaybetmeden bildirin.", 10,
                SearchFixtures.e0());

        // Act
        ChunkMatchesResponse response = chunkService.findChunkMatches(42, List.of(SENTENCE.toLowerCase()));

        // Assert
        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().getFirst().chunks()).containsExactly(
                new MatchedChunkResponse(151, 10001, "Rehber", "2026.10", 19));
    }

    @Test
    void givenTextOnlyInSourceWithoutCurrentVersion_whenFindChunkMatches_thenNoChunks() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Eski");
        long oldVersion = SearchFixtures.insertVersion(jdbc, 10001, "2026.09", false);
        ChunkFixtures.insertChunk(jdbc, 1, 10001, oldVersion, 0, "Başlık", SENTENCE);
        long second = activeVersion(10002, "Yeni");
        ChunkFixtures.insertChunk(jdbc, 2, 10002, second, 0, "Başlık", "başka bir içerik");

        // Act
        ChunkMatchesResponse response = chunkService.findChunkMatches(42, List.of(SENTENCE));

        // Assert
        assertThat(response.matches().getFirst().chunks()).isEmpty();
    }

    @Test
    void givenTextInOldAndCurrentVersionOfSameSource_whenFindChunkMatches_thenOnlyCurrentVersionChunk() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Rehber");
        long oldVersion = SearchFixtures.insertVersion(jdbc, 10001, "2026.09", false);
        long current = SearchFixtures.insertVersion(jdbc, 10001, "2026.10", true);
        ChunkFixtures.insertChunk(jdbc, 1, 10001, oldVersion, 0, "Eski", SENTENCE);
        ChunkFixtures.insertChunk(jdbc, 2, 10001, current, 0, "Yeni", SENTENCE);

        // Act
        ChunkMatchesResponse response = chunkService.findChunkMatches(42, List.of(SENTENCE));

        // Assert
        assertThat(chunkIds(response.matches().getFirst())).containsExactly(2L);
    }

    @Test
    void givenTextInTwoChunks_whenFindChunkMatches_thenReturnsBothByChunkId() {
        // Arrange
        long versionId = activeVersion(10001, "Rehber");
        ChunkFixtures.insertChunk(jdbc, 30, 10001, versionId, 1, "B", "sonu " + SENTENCE);
        ChunkFixtures.insertChunk(jdbc, 20, 10001, versionId, 0, "A", SENTENCE + " başı");
        ChunkFixtures.insertChunk(jdbc, 40, 10001, versionId, 2, "C", "ilgisiz içerik");

        // Act
        ChunkMatchesResponse response = chunkService.findChunkMatches(42, List.of(SENTENCE));

        // Assert
        assertThat(chunkIds(response.matches().getFirst())).containsExactly(20L, 30L);
    }

    @Test
    void givenNoSearchableSource_whenFindChunkMatches_thenEveryMatchIsEmpty() {
        // Act
        ChunkMatchesResponse response = chunkService.findChunkMatches(42, List.of("birinci metin", "ikinci metin", "üçüncü"));

        // Assert
        assertThat(response.matches()).hasSize(3);
        assertThat(response.matches()).allSatisfy(match -> assertThat(match.chunks()).isEmpty());
    }

    @Test
    void givenDuplicateTexts_whenFindChunkMatches_thenKeepsRequestOrder() {
        // Arrange
        long versionId = activeVersion(10001, "Rehber");
        ChunkFixtures.insertChunk(jdbc, 1, 10001, versionId, 0, "H", "alfa içerik");
        ChunkFixtures.insertChunk(jdbc, 2, 10001, versionId, 1, "H", "beta içerik");

        // Act
        ChunkMatchesResponse response = chunkService.findChunkMatches(42, List.of("ALFA içerik", "beta içerik", "ALFA içerik"));

        // Assert
        assertThat(response.matches()).extracting(ChunkTextMatchResponse::text)
                .containsExactly("ALFA içerik", "beta içerik", "ALFA içerik");
        assertThat(response.matches().stream().map(ChunkServiceIntegrationTest::chunkIds).toList())
                .containsExactly(List.of(1L), List.of(2L), List.of(1L));
    }

    @Test
    void givenDifferentDigitPunctuation_whenFindChunkMatches_thenNoMatch() {
        // Arrange
        long versionId = activeVersion(10001, "Rehber");
        ChunkFixtures.insertChunk(jdbc, 1, 10001, versionId, 0, "Ücret", "Yıllık ücret 1.650 TL tutarındadır.");

        // Act
        ChunkMatchesResponse response = chunkService.findChunkMatches(42, List.of("1650 TL", "1.650 TL"));

        // Assert
        assertThat(response.matches().get(0).chunks()).isEmpty();
        assertThat(chunkIds(response.matches().get(1))).containsExactly(1L);
    }

    @Test
    void givenUpperCaseDottedI_whenFindChunkMatches_thenMatchesTurkishLowerCaseButNotAsciiI() {
        // Arrange
        long versionId = activeVersion(10001, "Rehber");
        ChunkFixtures.insertChunk(jdbc, 1, 10001, versionId, 0, "H", "KISA VADELİ hesap");

        // Act
        ChunkMatchesResponse response = chunkService.findChunkMatches(42, List.of("kısa vadeli hesap", "kisa vadeli hesap"));

        // Assert
        assertThat(chunkIds(response.matches().get(0))).containsExactly(1L);
        assertThat(response.matches().get(1).chunks()).isEmpty();
    }
}
