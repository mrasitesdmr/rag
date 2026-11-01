package com.rasitbank.retrieval.search.repository;

import com.rasitbank.retrieval.config.SearchProperties;
import com.rasitbank.retrieval.search.domain.dto.SearchHitResponse;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sorgu vektörüne en yakın parçaları, her kaynağın sadece güncel sürümünde HNSW index'iyle arar.
 */
@Repository
public class ChunkSearchRepository {

    static final String ITERATIVE_SCAN = "relaxed_order";
    static final String ENABLE_SORT = "off";

    static final String SEARCH_SETTINGS_SQL = """
            SELECT set_config('hnsw.ef_search', :efSearch, true),
                   set_config('hnsw.iterative_scan', :iterativeScan, true),
                   set_config('hnsw.max_scan_tuples', :maxScanTuples, true),
                   set_config('enable_sort', :enableSort, true)
            """;

    static final String NEAREST_CHUNKS_SQL = """
            WITH nearest AS MATERIALIZED (
                SELECT id, embedding <=> CAST(:embedding AS vector) AS distance
                FROM chunks
                WHERE workspace_id = :workspaceId
                  AND embedding_model = :embeddingModel
                  AND source_version_id = ANY (ARRAY(
                      SELECT active_version_id
                      FROM sources
                      WHERE workspace_id = :workspaceId
                        AND active_version_id IS NOT NULL))
                ORDER BY embedding <=> CAST(:embedding AS vector)
                LIMIT :topK
            )
            SELECT c.id AS chunk_id, c.chunk_no, c.page_number, c.heading, c.content, c.source_id,
                   s.name AS source_name, v.version, nearest.distance
            FROM nearest
            JOIN chunks c ON c.id = nearest.id
            JOIN sources s ON s.id = c.source_id
            JOIN source_versions v ON v.id = c.source_version_id
            ORDER BY nearest.distance, nearest.id
            """;

    private final JdbcClient jdbcClient;
    private final SearchProperties searchProperties;

    public ChunkSearchRepository(JdbcClient jdbcClient, SearchProperties searchProperties) {
        this.jdbcClient = jdbcClient;
        this.searchProperties = searchProperties;
    }

    @Transactional(readOnly = true)
    public List<SearchHitResponse> findNearest(long workspaceId, float[] queryEmbedding, String embeddingModel,
            int topK) {
        applySearchSettings();
        return jdbcClient.sql(NEAREST_CHUNKS_SQL)
                .param("embedding", toVectorLiteral(queryEmbedding))
                .param("workspaceId", workspaceId)
                .param("embeddingModel", embeddingModel)
                .param("topK", topK)
                .query((resultSet, rowNumber) -> new SearchHitResponse(
                        resultSet.getLong("chunk_id"),
                        resultSet.getInt("chunk_no"),
                        resultSet.getInt("page_number"),
                        resultSet.getString("heading"),
                        resultSet.getString("content"),
                        resultSet.getLong("source_id"),
                        resultSet.getString("source_name"),
                        resultSet.getString("version"),
                        resultSet.getDouble("distance")))
                .list();
    }

    static String toVectorLiteral(float[] embedding) {
        StringBuilder literal = new StringBuilder(embedding.length * 12).append('[');
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                literal.append(',');
            }
            literal.append(Float.toString(embedding[index]));
        }
        return literal.append(']').toString();
    }

    private void applySearchSettings() {
        jdbcClient.sql(SEARCH_SETTINGS_SQL)
                .param("efSearch", String.valueOf(searchProperties.efSearch()))
                .param("iterativeScan", ITERATIVE_SCAN)
                .param("maxScanTuples", String.valueOf(searchProperties.maxScanTuples()))
                .param("enableSort", ENABLE_SORT)
                .query()
                .listOfRows();
    }
}
