package com.rasitbank.retrieval.search.repository;

import static com.rasitbank.retrieval.fixture.SearchFixtures.e0;
import static com.rasitbank.retrieval.fixture.SearchFixtures.e1;
import static com.rasitbank.retrieval.fixture.SearchFixtures.insertChunk;
import static com.rasitbank.retrieval.fixture.SearchFixtures.insertSource;
import static com.rasitbank.retrieval.fixture.SearchFixtures.insertVersion;
import static com.rasitbank.retrieval.fixture.SearchFixtures.literal;
import static com.rasitbank.retrieval.fixture.SearchFixtures.mixed;
import static com.rasitbank.retrieval.fixture.SearchFixtures.vector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.rasitbank.retrieval.fixture.SearchFixtures;
import com.rasitbank.retrieval.search.domain.dto.SearchHitResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gerçek pgvector veritabanında en yakın parça aramasının sırasını, güncel sürüm filtresini ve HNSW kullanımını doğrular.
 */
class ChunkSearchRepositoryIntegrationTest extends AbstractSearchIntegrationTest {

    @Autowired
    private ChunkSearchRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private long seedSourceWithChunks(long sourceId, String name, String version, float[]... embeddings) {
        insertSource(jdbc, sourceId, name);
        long versionId = insertVersion(jdbc, sourceId, version, true);
        for (int index = 0; index < embeddings.length; index++) {
            insertChunk(jdbc, sourceId * 100 + index, sourceId, versionId, index, embeddings[index]);
        }
        return versionId;
    }

    @Test
    void givenThreeChunks_whenFindNearestToE0_thenOrderedByCosineDistance() {
        // Arrange
        seedSourceWithChunks(10001, "Rehber", "2026.10", e1(), mixed(), e0());

        // Act
        List<SearchHitResponse> hits = repository.findNearest(42, e0(), SearchFixtures.MODEL, 5);

        // Assert
        assertThat(hits).extracting(SearchHitResponse::chunkId).containsExactly(1000102L, 1000101L, 1000100L);
        assertThat(hits.get(0).distance()).isCloseTo(0.0, within(1e-6));
        assertThat(hits.get(1).distance()).isCloseTo(0.2, within(1e-6));
        assertThat(hits.get(2).distance()).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void givenTwoChunksWithSameEmbedding_whenFindNearest_thenLowerChunkIdFirst() {
        // Arrange
        insertSource(jdbc, 10001, "Rehber");
        long versionId = insertVersion(jdbc, 10001, "1", true);
        insertChunk(jdbc, 7002, 10001, versionId, 0, e0());
        insertChunk(jdbc, 7001, 10001, versionId, 1, e0());

        // Act
        List<SearchHitResponse> hits = repository.findNearest(42, e0(), SearchFixtures.MODEL, 5);

        // Assert
        assertThat(hits).extracting(SearchHitResponse::chunkId).containsExactly(7001L, 7002L);
    }

    @Test
    void givenFiveChunks_whenFindNearestWithTopK2_thenTwoNearest() {
        // Arrange
        seedSourceWithChunks(10001, "Rehber", "1", vector(0f, 1f), vector(0.1f, 1f), vector(0.2f, 1f),
                vector(0.3f, 1f), vector(0.4f, 1f));

        // Act
        List<SearchHitResponse> hits = repository.findNearest(42, e1(), SearchFixtures.MODEL, 2);

        // Assert
        assertThat(hits).extracting(SearchHitResponse::chunkId).containsExactly(1000100L, 1000101L);
    }

    @Test
    void givenFiveChunks_whenFindNearestWithTopK10_thenAllFive() {
        // Arrange
        seedSourceWithChunks(10001, "Rehber", "1", e1(), e1(), e1(), e1(), e1());

        // Act
        List<SearchHitResponse> hits = repository.findNearest(42, vector(0f, -1f), SearchFixtures.MODEL, 10);

        // Assert
        assertThat(hits).hasSize(5);
    }

    @Test
    void givenOldActiveVersionAndNewerUnactivatedVersion_whenFindNearest_thenOnlyActiveVersionChunksReturned() {
        // Arrange
        insertSource(jdbc, 10001, "Rehber");
        long activeVersion = insertVersion(jdbc, 10001, "2026.10", true);
        long newerVersion = insertVersion(jdbc, 10001, "2026.11", false);
        insertChunk(jdbc, 500, 10001, activeVersion, 0, mixed());
        insertChunk(jdbc, 501, 10001, newerVersion, 0, e0());

        // Act
        List<SearchHitResponse> hits = repository.findNearest(42, e0(), SearchFixtures.MODEL, 5);

        // Assert
        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().chunkId()).isEqualTo(500);
        assertThat(hits.getFirst().version()).isEqualTo("2026.10");
    }

    @Test
    void givenSourceWithoutActiveVersion_whenFindNearest_thenItsChunksAreNotReturned() {
        // Arrange
        seedSourceWithChunks(10001, "Aktif", "1", e1());
        insertSource(jdbc, 10002, "Etkin değil");
        long inactiveVersion = insertVersion(jdbc, 10002, "1", false);
        insertChunk(jdbc, 900, 10002, inactiveVersion, 0, e0());

        // Act
        List<SearchHitResponse> hits = repository.findNearest(42, e0(), SearchFixtures.MODEL, 5);

        // Assert
        assertThat(hits).extracting(SearchHitResponse::sourceId).containsExactly(10001L);
    }

    @Test
    void givenTwoActiveSources_whenFindNearest_thenHitsComeFromBothWithTheirNamesAndVersions() {
        // Arrange
        seedSourceWithChunks(10001, "Rehber", "2026.10", e0());
        seedSourceWithChunks(10002, "El Kitabı", "v1", mixed());

        // Act
        List<SearchHitResponse> hits = repository.findNearest(42, e0(), SearchFixtures.MODEL, 5);

        // Assert
        assertThat(hits).hasSize(2);
        SearchHitResponse first = hits.get(0);
        assertThat(first.sourceId()).isEqualTo(10001);
        assertThat(first.sourceName()).isEqualTo("Rehber");
        assertThat(first.version()).isEqualTo("2026.10");
        assertThat(first.chunkId()).isEqualTo(1000100);
        assertThat(first.chunkNo()).isZero();
        assertThat(first.pageNumber()).isEqualTo(3);
        assertThat(first.heading()).isEqualTo("Başlık 1000100");
        assertThat(first.content()).isEqualTo("İçerik 1000100");
        assertThat(hits.get(1).sourceId()).isEqualTo(10002);
        assertThat(hits.get(1).sourceName()).isEqualTo("El Kitabı");
        assertThat(hits.get(1).version()).isEqualTo("v1");
    }

    @Test
    void givenChunkOfAnotherEmbeddingModel_whenFindNearest_thenItIsNotReturned() {
        // Arrange
        insertSource(jdbc, 10001, "Rehber");
        long versionId = insertVersion(jdbc, 10001, "1", true);
        insertChunk(jdbc, 1, 10001, versionId, 0, e0(), "other@1024");
        insertChunk(jdbc, 2, 10001, versionId, 1, e1());

        // Act
        List<SearchHitResponse> hits = repository.findNearest(42, e0(), SearchFixtures.MODEL, 5);

        // Assert
        assertThat(hits).extracting(SearchHitResponse::chunkId).containsExactly(2L);
    }

    @Test
    void givenOtherWorkspaceId_whenFindNearest_thenNothingReturned() {
        // Arrange
        seedSourceWithChunks(10001, "Rehber", "1", e0());

        // Act
        List<SearchHitResponse> hits = repository.findNearest(43, e0(), SearchFixtures.MODEL, 5);

        // Assert
        assertThat(hits).isEmpty();
    }

    @Test
    void givenSearchSettings_whenExplainNearestChunksQuery_thenPlanUsesHnswIndex() {
        // Arrange
        seedSourceWithChunks(10001, "Rehber", "1", e0(), e1(), mixed());

        // Act
        List<String> plan = transactionTemplate.execute(status -> {
            jdbc.queryForList(
                    "select set_config('hnsw.ef_search', ?, true), set_config('hnsw.iterative_scan', ?, true), "
                            + "set_config('hnsw.max_scan_tuples', ?, true), set_config('enable_sort', ?, true)",
                    "40", ChunkSearchRepository.ITERATIVE_SCAN, "20000", ChunkSearchRepository.ENABLE_SORT);
            String sql = ChunkSearchRepository.NEAREST_CHUNKS_SQL
                    .replace(":embeddingModel", "'" + SearchFixtures.MODEL + "'")
                    .replace(":embedding", "'" + literal(e0()) + "'").replace(":workspaceId", "42")
                    .replace(":topK", "5");
            return jdbc.queryForList("EXPLAIN " + sql, String.class);
        });

        // Assert
        assertThat(String.join("\n", plan)).contains("Index Scan using chunks_embedding_hnsw_idx");
    }

    @Test
    void givenFindNearestInsideTransaction_whenReadingSessionSettings_thenHnswSettingsAreApplied() {
        // Arrange
        seedSourceWithChunks(10001, "Rehber", "1", e0());

        // Act
        List<String> settings = transactionTemplate.execute(status -> {
            repository.findNearest(42, e0(), SearchFixtures.MODEL, 5);
            return List.of(jdbc.queryForObject("select current_setting('hnsw.ef_search')", String.class),
                    jdbc.queryForObject("select current_setting('hnsw.iterative_scan')", String.class),
                    jdbc.queryForObject("select current_setting('hnsw.max_scan_tuples')", String.class),
                    jdbc.queryForObject("select current_setting('enable_sort')", String.class));
        });

        // Assert
        assertThat(settings).containsExactly("40", "relaxed_order", "20000", "off");
    }

    @Test
    void givenEmbedding_whenToVectorLiteral_thenCommaSeparatedWithoutSpaces() {
        // Act
        String literal = ChunkSearchRepository.toVectorLiteral(new float[] {1f, 0.5f, -0.25f});

        // Assert
        assertThat(literal).isEqualTo("[1.0,0.5,-0.25]");
    }
}
