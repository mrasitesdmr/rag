package com.rasitbank.retrieval.ingestion.integration;

import static com.rasitbank.retrieval.fixture.PdfFixtures.validPdf;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gerçek sunucu üzerinden multipart yükleme sınırını, UTF-8 adı, 202 yanıtını ve kaynak listesinin sırasını doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SourceUploadHttpIntegrationTest extends AbstractIngestionIntegrationTest {

    private static final String BOUNDARY = "----rag-test-boundary";
    private static final int MAX_FILE_BYTES = 20 * 1024 * 1024;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    private HttpResponse<String> postSources(String name, String version, String fileName, byte[] file) {
        try {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            part(body, "name", null, name.getBytes(StandardCharsets.UTF_8));
            part(body, "version", null, version.getBytes(StandardCharsets.UTF_8));
            part(body, "file", fileName, file);
            body.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/workspaces/42/sources"))
                    .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private HttpResponse<String> get(String path) {
        try {
            return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri(path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static void part(ByteArrayOutputStream out, String name, String fileName, byte[] content)
            throws IOException {
        String disposition = "Content-Disposition: form-data; name=\"" + name + "\""
                + (fileName == null ? "" : "; filename=\"" + fileName + "\"");
        out.write(("--" + BOUNDARY + "\r\n" + disposition + "\r\n"
                + (fileName == null ? "" : "Content-Type: application/pdf\r\n") + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(content);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] pdfSignatureWithPadding(int size) {
        byte[] content = new byte[size];
        Arrays.fill(content, (byte) 'a');
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, content, 0, 5);
        return content;
    }

    @Test
    void givenFileOf20MiBPlusOneByte_whenPostSources_thenReturns413FileTooLarge() {
        // Act
        HttpResponse<String> response = postSources("Büyük", "1", "buyuk.pdf", pdfSignatureWithPadding(MAX_FILE_BYTES + 1));

        // Assert
        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(response.body()).contains("\"errorCode\":\"FILE_TOO_LARGE\"");
        assertThat(count("sources")).isZero();
    }

    @Test
    void givenFileOfExactly20MiB_whenPostSources_thenNot413ButRejectedAsUnreadablePdf() {
        // Act
        HttpResponse<String> response = postSources("Sınırda", "1", "sinir.pdf", pdfSignatureWithPadding(MAX_FILE_BYTES));

        // Assert
        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("\"errorCode\":\"PDF_UNREADABLE\"");
        assertThat(count("sources")).isZero();
    }

    @Test
    void givenTurkishName_whenPostSources_thenReturns202LocationAndNameStoredIntact() {
        // Act
        HttpResponse<String> response = postSources("Raşit Bankası İşlem Rehberi", "2026.10", "rehber.pdf", validPdf());

        // Assert
        assertThat(response.statusCode()).isEqualTo(202);
        JsonNode job = JSON.readTree(response.body());
        assertThat(response.headers().firstValue("Location")).contains(job.get("statusUrl").asString());
        assertThat(job.get("stage").asString()).isEqualTo("QUEUED");
        assertThat(job.get("version").asString()).isEqualTo("2026.10");
        awaitJobStage(job.get("jobId").asLong(), "DONE");
        assertThat(jdbc.queryForObject("select name from sources", String.class)).isEqualTo("Raşit Bankası İşlem Rehberi");
    }

    @Test
    void givenNoUploadYet_whenGetSources_thenEmptyPage() {
        // Act
        HttpResponse<String> response = get("/api/workspaces/42/sources");

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode page = JSON.readTree(response.body());
        assertThat(page.get("content").size()).isZero();
        assertThat(page.get("totalElements").asLong()).isZero();
    }

    @Test
    void givenTwoSources_whenGetSourcesPaged_thenOrderedByIdAscendingWithPageMetadata() {
        // Arrange
        awaitJobStage(JSON.readTree(postSources("Birinci belge", "1", "b1.pdf", validPdf()).body()).get("jobId").asLong(),
                "DONE");
        awaitJobStage(JSON.readTree(postSources("İkinci belge", "1", "b2.pdf", validPdf()).body()).get("jobId").asLong(),
                "DONE");

        // Act
        JsonNode firstPage = JSON.readTree(get("/api/workspaces/42/sources?page=0&size=1").body());
        JsonNode secondPage = JSON.readTree(get("/api/workspaces/42/sources?page=1&size=1").body());

        // Assert
        assertThat(firstPage.get("content").get(0).get("sourceId").asLong()).isEqualTo(10001);
        assertThat(firstPage.get("content").get(0).get("name").asString()).isEqualTo("Birinci belge");
        assertThat(firstPage.get("content").get(0).get("kind").asString()).isEqualTo("PDF");
        assertThat(firstPage.get("totalElements").asLong()).isEqualTo(2);
        assertThat(firstPage.get("totalPages").asInt()).isEqualTo(2);
        assertThat(secondPage.get("content").get(0).get("sourceId").asLong()).isEqualTo(10002);
        assertThat(secondPage.get("page").asInt()).isEqualTo(1);
    }

    @Test
    void givenSameNameDifferentCase_whenPostSourcesTwice_thenSecondReturns409SourceNameTaken() {
        // Arrange
        awaitJobStage(JSON.readTree(postSources("Rehber", "1", "r.pdf", validPdf()).body()).get("jobId").asLong(),
                "DONE");

        // Act
        HttpResponse<String> response = postSources("REHBER", "1", "r2.pdf", validPdf());

        // Assert
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"errorCode\":\"SOURCE_NAME_TAKEN\"");
    }
}
