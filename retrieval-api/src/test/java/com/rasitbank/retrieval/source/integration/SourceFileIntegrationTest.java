package com.rasitbank.retrieval.source.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.fixture.ChunkFixtures;
import com.rasitbank.retrieval.fixture.PdfFixtures;
import com.rasitbank.retrieval.fixture.SearchFixtures;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ContentDisposition;

/**
 * Saklanan PDF'in gerçek HTTP yanıtında aynı baytlarla, tarayıcıda açılacak ve HTML sayılmayacak başlıklarla döndüğünü
 * doğrular.
 */
@ExtendWith(OutputCaptureExtension.class)
class SourceFileIntegrationTest extends AbstractReadIntegrationTest {

    private static final String TURKISH_FILE_NAME = "Şirket El Kitabı.pdf";

    @Test
    void givenDoneSourceWithTurkishFileName_whenGetSourceFile_thenReturnsSameBytesAsPdfInline() {
        // Arrange
        byte[] pdf = PdfFixtures.validPdf();
        IngestionJobResponse job = upload("Şirket El Kitabı", "1", TURKISH_FILE_NAME, pdf);
        awaitJobStage(job.jobId(), "DONE");

        // Act
        HttpResponse<byte[]> response = get("/api/workspaces/42/sources/" + job.sourceId() + "/file");

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(pdf);
        assertThat(response.headers().firstValue("Content-Type")).contains("application/pdf");
        ContentDisposition disposition = ContentDisposition.parse(response.headers().firstValue("Content-Disposition")
                .orElseThrow());
        assertThat(disposition.isInline()).isTrue();
        assertThat(disposition.getFilename()).isEqualTo(TURKISH_FILE_NAME);
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    }

    @Test
    void givenSourceWhoseFirstJobFailed_whenGetSourceFile_thenReturnsLatestJobFile() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Başarısız");
        ChunkFixtures.insertFailedJob(jdbc, 10001, "1", "eski.pdf", 3, new byte[] {1, 1, 1});
        byte[] latest = {9, 8, 7, 6};
        ChunkFixtures.insertFailedJob(jdbc, 10001, "1", "yeni.pdf", 4, latest);

        // Act
        HttpResponse<byte[]> response = get("/api/workspaces/42/sources/10001/file");

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(latest);
        assertThat(ContentDisposition.parse(response.headers().firstValue("Content-Disposition").orElseThrow())
                .getFilename()).isEqualTo("yeni.pdf");
    }

    @Test
    void givenActiveVersionAndLaterFailedJob_whenGetSourceFile_thenReturnsActiveVersionFile() {
        // Arrange
        SearchFixtures.insertSource(jdbc, 10001, "Hazır");
        long versionId = SearchFixtures.insertVersion(jdbc, 10001, "1", true);
        byte[] active = {1, 2, 3};
        ChunkFixtures.insertFile(jdbc, ChunkFixtures.jobIdOfVersion(jdbc, versionId), active);
        ChunkFixtures.insertFailedJob(jdbc, 10001, "2", "sonraki.pdf", 2, new byte[] {5, 5});

        // Act
        HttpResponse<byte[]> response = get("/api/workspaces/42/sources/10001/file");

        // Assert
        assertThat(response.body()).isEqualTo(active);
    }

    @Test
    void givenUnknownSource_whenGetSourceFile_thenReturns404SourceNotFound() {
        // Act
        HttpResponse<byte[]> response = get("/api/workspaces/42/sources/99999/file");

        // Assert
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(response.body()).get("errorCode").asString()).isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenSourceFileServed_whenLogsCaptured_thenIdsLoggedButNoFileName(CapturedOutput output) {
        // Arrange
        IngestionJobResponse job = upload("Gizli Kaynak Adı", "1", "gizli-dosya.pdf", PdfFixtures.validPdf());
        awaitJobStage(job.jobId(), "DONE");

        // Act
        get("/api/workspaces/42/sources/" + job.sourceId() + "/file");

        // Assert
        assertThat(output.getOut()).contains("Source file served sourceId=" + job.sourceId() + " jobId=" + job.jobId());
        assertThat(output.getAll()).doesNotContain("gizli-dosya").doesNotContain("Gizli Kaynak Adı");
    }
}
