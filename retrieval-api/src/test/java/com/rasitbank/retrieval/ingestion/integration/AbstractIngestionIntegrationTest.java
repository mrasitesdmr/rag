package com.rasitbank.retrieval.ingestion.integration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.embedding.client.EmbeddingClient;
import com.rasitbank.retrieval.fixture.PdfFixtures;
import com.rasitbank.retrieval.ingestion.repository.IngestionJobRepository;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.service.IngestionStartupListener;
import com.rasitbank.retrieval.source.service.SourceUploadValidator;
import org.springframework.mock.web.MockMultipartFile;
import com.rasitbank.retrieval.ingestion.service.IngestionService;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Gerçek pgvector veritabanı ve sahte embedding istemcisiyle içe alma entegrasyon testlerinin ortak kurulumudur.
 */
@SpringBootTest
abstract class AbstractIngestionIntegrationTest {

    static final Path GUIDE = Path.of("../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf");

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:0.8.1-pg18");

    static {
        POSTGRES.start();
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    IngestionJobRepository ingestionJobRepository;

    @Autowired
    IngestionService ingestionService;

    @Autowired
    SourceUploadValidator sourceUploadValidator;

    @Autowired
    IngestionStartupListener startupListener;

    @MockitoBean
    EmbeddingClient embeddingClient;

    @BeforeEach
    void cleanDatabaseAndStubEmbedding() {
        jdbc.execute("update sources set active_version_id = null");
        jdbc.execute("delete from chunks");
        jdbc.execute("delete from source_versions");
        jdbc.execute("delete from ingestion_job_files");
        jdbc.execute("delete from ingestion_jobs");
        jdbc.execute("delete from sources");
        jdbc.execute("alter sequence sources_id_seq restart with 10001");
        stubEmbeddingSuccess();
    }

    IngestionJobResponse upload(String name, String version, String fileName, byte[] pdf) {
        return ingestionService.createSource(42,
                sourceUploadValidator.validate(new MockMultipartFile("file", fileName, "application/pdf", pdf), name,
                        version));
    }

    IngestionJobResponse uploadValidPdf(String name) {
        return upload(name, "1", "belge.pdf", PdfFixtures.validPdf());
    }

    static byte[] guideBytes() {
        try {
            return java.nio.file.Files.readAllBytes(GUIDE);
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    void stubEmbeddingSuccess() {
        org.mockito.Mockito.reset(embeddingClient);
        given(embeddingClient.embed(anyList())).willAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream().map(input -> deterministicVector(input)).toList();
        });
    }

    void awaitJobStage(long jobId, String stage) {
        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofMillis(100)).until(() -> {
            String current = jdbc.queryForObject("select stage from ingestion_jobs where id = ?", String.class, jobId);
            return stage.equals(current);
        });
    }

    long singleJobId() {
        Long id = jdbc.queryForObject("select max(id) from ingestion_jobs", Long.class);
        return id == null ? -1 : id;
    }

    long count(String table) {
        Long count = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0 : count;
    }

    private static float[] deterministicVector(String text) {
        float[] vector = new float[1024];
        int hash = text.hashCode();
        for (int index = 0; index < vector.length; index++) {
            vector[index] = Math.floorMod(hash + index, 7) / 7f;
        }
        return vector;
    }
}
