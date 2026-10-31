package com.rasitbank.retrieval.ingestion.integration;

import static com.rasitbank.retrieval.fixture.PdfFixtures.PageSpec;
import static com.rasitbank.retrieval.fixture.PdfFixtures.corruptPdf;
import static com.rasitbank.retrieval.fixture.PdfFixtures.imageOnlyPdf;
import static com.rasitbank.retrieval.fixture.PdfFixtures.pdf;
import static com.rasitbank.retrieval.fixture.PdfFixtures.validPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.exception.ConflictException;
import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.rasitbank.retrieval.exception.UnprocessableContentException;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Belge ekleme akışını (doğrulama, kaynak ve iş oluşturma, gerçek hat, ad tekilliği) gerçek PostgreSQL'e karşı doğrular.
 */
@ExtendWith(OutputCaptureExtension.class)
class SourceUploadIntegrationTest extends AbstractIngestionIntegrationTest {

    private static final String GUIDE_NAME = "Raşit Bankası Ürün ve Hizmet Rehberi";

    private long uploadAndAwaitDone(String name, byte[] bytes) {
        IngestionJobResponse job = upload(name, "1", "belge.pdf", bytes);
        awaitJobStage(job.jobId(), "DONE");
        return job.jobId();
    }

    @Test
    void givenFreshDatabase_whenCreateSourceWithGuide_thenJobDoneSource10001WithChunkCountOfM0Report() {
        // Arrange
        byte[] guide = guideBytes();

        // Act
        IngestionJobResponse job = upload(GUIDE_NAME, "2026.10", "Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf",
                guide);
        awaitJobStage(job.jobId(), "DONE");

        // Assert
        assertThat(job.stage().name()).isEqualTo("QUEUED");
        assertThat(jdbc.queryForList("select id, name, kind from sources")).singleElement().satisfies(source ->
                assertThat(source).containsEntry("id", 10001L).containsEntry("kind", "PDF")
                        .containsEntry("name", GUIDE_NAME));
        Map<String, Object> version = jdbc.queryForMap("select * from source_versions");
        assertThat(version).containsEntry("version", "2026.10").containsEntry("page_count", 70)
                .containsEntry("chapter_count", 11).containsEntry("chunk_count", 154)
                .containsEntry("file_size_bytes", (long) guide.length)
                .containsEntry("file_name", "Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf")
                .containsEntry("ingestion_job_id", job.jobId());
        assertThat(count("chunks")).isEqualTo(154);
        assertThat(jdbc.queryForMap("select min(page_number) as first_page, max(page_number) as last_page from chunks"))
                .containsEntry("first_page", 5).containsEntry("last_page", 70);
        Map<String, Object> stored = jdbc.queryForMap("select stage, progress, embedded_chunks, total_chunks,"
                + " finished_at from ingestion_jobs where id = ?", job.jobId());
        assertThat(stored).containsEntry("stage", "DONE").containsEntry("progress", 1.0)
                .containsEntry("embedded_chunks", 154).containsEntry("total_chunks", 154);
        assertThat(((Timestamp) version.get("synced_at")).toInstant())
                .isEqualTo(((Timestamp) stored.get("finished_at")).toInstant());
        assertThat(jdbc.queryForObject("select content from ingestion_job_files where ingestion_job_id = ?",
                byte[].class, job.jobId())).isEqualTo(guide);
        assertThat(jdbc.queryForObject("select active_version_id = ? from sources where id = 10001", Boolean.class,
                version.get("id"))).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from chunks where vector_dims(embedding) <> 1024",
                Long.class)).isZero();
        assertThat(jdbc.queryForObject("select min(embedding_model) from chunks", String.class))
                .isEqualTo("bge-m3@1024");
        assertThat(jdbc.queryForObject("select min(page_number) from chunks", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select max(char_length(content)) from chunks", Integer.class))
                .isLessThanOrEqualTo(2000);
    }

    @Test
    void givenPdfWithUnnumberedBookmarks_whenCreateSource_thenJobDoneChunksCarryBookmarkHeadings() {
        // Arrange
        String body = "Bu paragraf deneme belgesinin gövdesidir ve birden fazla cümle içerir. ";
        byte[] bytes = pdf(true,
                PageSpec.of("Giriş", "Giriş", body, body, body),
                PageSpec.of("Teslimat", "Teslimat", body, body, body),
                PageSpec.of(">Gecikmeler", "Gecikmeler", body, body, body));

        // Act
        uploadAndAwaitDone("Yer imli deneme", bytes);

        // Assert
        assertThat(jdbc.queryForObject("select chapter_count from source_versions", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("select distinct heading from chunks", String.class))
                .isNotEmpty().isSubsetOf("Giriş", "Teslimat", "Gecikmeler");
        assertThat(jdbc.queryForObject("select min(page_number) from chunks", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select max(page_number) from chunks", Integer.class)).isEqualTo(3);
    }

    @Test
    void givenImageOnlyPdf_whenValidateAndCreate_thenRejectedPdfNoTextAndNothingWritten() {
        // Act
        Throwable thrown = catchThrowable(() -> upload("Taranmış", "1", "taranmis.pdf", imageOnlyPdf(true)));

        // Assert
        assertThat(thrown).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_TEXT");
        assertThat(count("sources")).isZero();
        assertThat(count("ingestion_jobs")).isZero();
    }

    @Test
    void givenPdfWithoutBookmarksRejected_whenFixedPdfUploadedWithSameName_thenAccepted() {
        // Arrange
        byte[] withoutBookmarks = pdf(false, PageSpec.of(null, "Satır bir.", "Satır iki."));
        Throwable rejected = catchThrowable(() -> upload("Aynı ad", "1", "belge.pdf", withoutBookmarks));

        // Act
        long jobId = uploadAndAwaitDone("Aynı ad", validPdf());

        // Assert
        assertThat(rejected).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_NO_BOOKMARKS");
        assertThat(count("sources")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select stage from ingestion_jobs where id = ?", String.class, jobId))
                .isEqualTo("DONE");
    }

    @Test
    void givenPdfSignatureButCorruptBody_whenValidate_thenRejectedPdfUnreadableAndNothingWritten() {
        // Act
        Throwable thrown = catchThrowable(() -> upload("Bozuk", "1", "bozuk.pdf", corruptPdf()));

        // Assert
        assertThat(thrown).isInstanceOf(UnprocessableContentException.class)
                .extracting("errorCode").isEqualTo("PDF_UNREADABLE");
        assertThat(count("sources")).isZero();
        assertThat(count("ingestion_jobs")).isZero();
    }

    @Test
    void givenDoneSourceRehber_whenCreateSourceNamedSpacedUpperRehber_thenSourceNameTakenAndNothingWritten() {
        // Arrange
        uploadAndAwaitDone("Rehber", validPdf());

        // Act
        Throwable thrown = catchThrowable(() -> uploadValidPdf(" REHBER "));

        // Assert
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("SOURCE_NAME_TAKEN");
        assertThat(count("sources")).isEqualTo(1);
        assertThat(count("ingestion_jobs")).isEqualTo(1);
    }

    @Test
    void givenSourceWhoseOnlyJobFailed_whenCreateSourceWithSameName_thenSourceNameTaken() {
        // Arrange
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE", "down"));
        IngestionJobResponse failed = uploadValidPdf("Başarısız");
        awaitJobStage(failed.jobId(), "FAILED");

        // Act
        Throwable thrown = catchThrowable(() -> uploadValidPdf("Başarısız"));

        // Assert
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("SOURCE_NAME_TAKEN");
        assertThat(count("sources")).isEqualTo(1);
        assertThat(count("ingestion_jobs")).isEqualTo(1);
    }

    @Test
    void givenTwoUploadsBackToBack_whenJobsRun_thenBothDoneOneAfterAnother() throws InterruptedException {
        // Arrange
        CountDownLatch release = new CountDownLatch(1);
        given(embeddingClient.embed(anyList())).willAnswer(invocation -> {
            release.await(30, TimeUnit.SECONDS);
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream().map(input -> new float[1024]).toList();
        });
        IngestionJobResponse first = uploadValidPdf("Birinci");
        awaitJobStage(first.jobId(), "EMBEDDING");

        // Act
        IngestionJobResponse second = uploadValidPdf("İkinci");
        String secondStageWhileFirstRuns = jdbc.queryForObject("select stage from ingestion_jobs where id = ?",
                String.class, second.jobId());
        release.countDown();
        awaitJobStage(first.jobId(), "DONE");
        awaitJobStage(second.jobId(), "DONE");

        // Assert
        assertThat(secondStageWhileFirstRuns).isEqualTo("QUEUED");
        Timestamp firstFinished = jdbc.queryForObject("select finished_at from ingestion_jobs where id = ?",
                Timestamp.class, first.jobId());
        Timestamp secondStarted = jdbc.queryForObject("select started_at from ingestion_jobs where id = ?",
                Timestamp.class, second.jobId());
        assertThat(secondStarted).isAfterOrEqualTo(firstFinished);
    }

    @Test
    void givenDoneSource_whenOnApplicationReadyAgain_thenCountsUnchangedAndNoNewJob() {
        // Arrange
        uploadAndAwaitDone("Rehber", guideBytes());

        // Act
        startupListener.onApplicationReady();

        // Assert
        assertThat(count("sources")).isEqualTo(1);
        assertThat(count("ingestion_jobs")).isEqualTo(1);
        assertThat(count("chunks")).isEqualTo(154);
    }

    @Test
    void givenUploadedAndRejectedPdfs_whenLogsCaptured_thenIdsAndCountsLoggedButNoNamesOrText(CapturedOutput output) {
        // Arrange
        byte[] accepted = pdf(true, PageSpec.of("Atlaskap Başlığı", "Atlaskap Başlığı", "Gizli gövde cümlesi burada."));
        byte[] rejected = pdf(false, PageSpec.of(null, "Gizli gövde cümlesi burada."));

        // Act
        IngestionJobResponse job = upload("Atlaskap El Kitabı", "v-9.9.9-gizli", "Atlaskap-El-Kitabi.pdf", accepted);
        awaitJobStage(job.jobId(), "DONE");
        catchThrowable(() -> upload("Atlaskap Yer İmsiz", "1", "Atlaskap-Yer-Imsiz.pdf", rejected));

        // Assert
        assertThat(output.getAll()).contains("Source created sourceId=").contains("Document parsed jobId=")
                .contains("Upload rejected errorCode=PDF_NO_BOOKMARKS");
        assertThat(output.getAll()).doesNotContain("Atlaskap").doesNotContain("v-9.9.9-gizli")
                .doesNotContain("Gizli gövde cümlesi");
    }
}
