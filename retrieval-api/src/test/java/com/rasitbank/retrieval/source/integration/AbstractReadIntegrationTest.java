package com.rasitbank.retrieval.source.integration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.embedding.client.EmbeddingClient;
import com.rasitbank.retrieval.fixture.SearchFixtures;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.service.IngestionService;
import com.rasitbank.retrieval.source.service.SourceUploadValidator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gerçek pgvector veritabanı, gerçek HTTP sunucusu ve sahte embedding istemcisiyle okuma uçlarının entegrasyon testlerinin
 * ortak kurulumudur.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractReadIntegrationTest {

    protected static final Path GUIDE = Path.of("../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf");
    protected static final JsonMapper JSON = JsonMapper.builder().build();

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:0.8.1-pg18");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected IngestionService ingestionService;

    @Autowired
    protected SourceUploadValidator sourceUploadValidator;

    @MockitoBean
    protected EmbeddingClient embeddingClient;

    @LocalServerPort
    private int port;

    @BeforeEach
    void cleanDatabaseAndStubEmbedding() {
        SearchFixtures.clean(jdbc);
        jdbc.execute("alter sequence sources_id_seq restart with 10001");
        stubEmbeddingSuccess();
    }

    protected void stubEmbeddingSuccess() {
        org.mockito.Mockito.reset(embeddingClient);
        given(embeddingClient.embed(anyList())).willAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream().map(AbstractReadIntegrationTest::deterministicVector).toList();
        });
    }

    protected IngestionJobResponse upload(String name, String version, String fileName, byte[] pdf) {
        return ingestionService.createSource(42, sourceUploadValidator
                .validate(new MockMultipartFile("file", fileName, "application/pdf", pdf), name, version));
    }

    protected void awaitJobStage(long jobId, String stage) {
        await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(100)).until(() -> stage.equals(
                jdbc.queryForObject("select stage from ingestion_jobs where id = ?", String.class, jobId)));
    }

    protected static byte[] guideBytes() {
        try {
            return Files.readAllBytes(GUIDE);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    protected HttpResponse<byte[]> get(String path) {
        try {
            return HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    protected JsonNode getJson(String path) {
        return JSON.readTree(get(path).body());
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
