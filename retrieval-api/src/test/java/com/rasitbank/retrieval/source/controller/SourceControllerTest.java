package com.rasitbank.retrieval.source.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.rasitbank.retrieval.exception.ConflictException;
import com.rasitbank.retrieval.exception.GlobalExceptionHandler;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.service.IngestionService;
import com.rasitbank.retrieval.source.domain.dto.SourceResponse;
import com.rasitbank.retrieval.source.domain.dto.SourceUpload;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import com.rasitbank.retrieval.source.service.SourceService;
import com.rasitbank.retrieval.source.service.SourceUploadValidator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Kaynak listesi, belge ekleme ve yeniden deneme uçlarının sözleşmeye uygun gövde, başlık ve hata yanıtlarını doğrular.
 */
@WebMvcTest(controllers = SourceController.class)
@Import(GlobalExceptionHandler.class)
class SourceControllerTest {

    private static final Instant CREATED = Instant.parse("2026-10-31T09:00:00Z");
    private static final SourceUpload UPLOAD = new SourceUpload("Rehber", "2026.10", "rehber.pdf", new byte[] {1});

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SourceService sourceService;

    @MockitoBean
    private SourceUploadValidator sourceUploadValidator;

    @MockitoBean
    private IngestionService ingestionService;

    private static IngestionJobResponse queuedJob(long jobId) {
        return new IngestionJobResponse(jobId, 42, 10001, "2026.10", "rehber.pdf", IngestionStage.QUEUED, null, 0.0,
                0, null, null, CREATED, null, null, "/api/workspaces/42/ingestion-jobs/" + jobId);
    }

    private static SourceResponse sourceResponse(long sourceId, String name) {
        return new SourceResponse(sourceId, 42, name, SourceKind.PDF, CREATED, "belge-b.pdf", 412873, "2026.10", 70,
                11, 154, 283, Instant.parse("2026-10-29T13:15:44Z"), queuedJob(7));
    }

    private static MockMultipartFile text(String name, String value) {
        return new MockMultipartFile(name, "", "text/plain", value.getBytes(StandardCharsets.UTF_8));
    }

    private MockMvcTester.MockMultipartMvcRequestBuilder upload(long workspaceId) {
        return mvc.post().uri("/api/workspaces/" + workspaceId + "/sources").multipart()
                .file(new MockMultipartFile("file", "rehber.pdf", "application/pdf", new byte[] {1}))
                .file(text("name", "Rehber")).file(text("version", "2026.10"));
    }

    @Test
    void givenValidUpload_whenCreateSource_thenReturns202WithLocationAndJob() {
        // Arrange
        given(sourceUploadValidator.validate(any(), any(), any())).willReturn(UPLOAD);
        given(ingestionService.createSource(42, UPLOAD)).willReturn(queuedJob(7));

        // Act
        var result = upload(42).exchange();

        // Assert
        assertThat(result).hasStatus(202).headers().hasValue("Location", "/api/workspaces/42/ingestion-jobs/7");
        assertThat(result).bodyJson().extractingPath("$.jobId").isEqualTo(7);
        assertThat(result).bodyJson().extractingPath("$.stage").isEqualTo("QUEUED");
        assertThat(result).bodyJson().extractingPath("$.statusUrl").isEqualTo("/api/workspaces/42/ingestion-jobs/7");
    }

    @Test
    void givenMultipartParts_whenCreateSource_thenValidatorReceivesFileNameAndVersionParts() {
        // Arrange
        given(sourceUploadValidator.validate(any(), any(), any())).willReturn(UPLOAD);
        given(ingestionService.createSource(42, UPLOAD)).willReturn(queuedJob(7));

        // Act
        upload(42).exchange();

        // Assert
        then(sourceUploadValidator).should().validate(
                org.mockito.ArgumentMatchers.argThat(file -> file != null
                        && "rehber.pdf".equals(file.getOriginalFilename())),
                org.mockito.ArgumentMatchers.eq("Rehber"), org.mockito.ArgumentMatchers.eq("2026.10"));
    }

    @Test
    void givenSourceNameTaken_whenCreateSource_thenReturns409SourceNameTaken() {
        // Arrange
        given(sourceUploadValidator.validate(any(), any(), any())).willReturn(UPLOAD);
        given(ingestionService.createSource(42, UPLOAD))
                .willThrow(new ConflictException("SOURCE_NAME_TAKEN", "taken"));

        // Act
        var result = upload(42).exchange();

        // Assert
        assertThat(result).hasStatus(409).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("SOURCE_NAME_TAKEN");
    }

    @Test
    void givenJsonBody_whenCreateSource_thenReturns415UnsupportedMediaType() {
        // Act
        var result = mvc.post().uri("/api/workspaces/42/sources").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Rehber\"}").exchange();

        // Assert
        assertThat(result).hasStatus(415).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void givenWorkspace43_whenCreateSource_thenReturns404WorkspaceNotFound() {
        // Arrange
        given(sourceUploadValidator.validate(any(), any(), any())).willReturn(UPLOAD);
        given(ingestionService.createSource(43, UPLOAD))
                .willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok"));

        // Act
        var result = upload(43).exchange();

        // Assert
        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenModelMismatch_whenCreateSource_thenReturns409EmbeddingModelMismatch() {
        // Arrange
        given(sourceUploadValidator.validate(any(), any(), any())).willReturn(UPLOAD);
        given(ingestionService.createSource(42, UPLOAD))
                .willThrow(new ConflictException("EMBEDDING_MODEL_MISMATCH", "differs"));

        // Act
        var result = upload(42).exchange();

        // Assert
        assertThat(result).hasStatus(409).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("EMBEDDING_MODEL_MISMATCH");
    }

    @Test
    void givenNoSources_whenListSources_thenReturnsEmptyPage() {
        // Arrange
        given(sourceService.listSources(42, 0, 20)).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content").asList().isEmpty();
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(20);
        assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(0);
    }

    @Test
    void givenTwoSources_whenListSourcesPage1Size1_thenReturnsSourceFieldsAndPageMetadata() {
        // Arrange
        SourceResponse source = sourceResponse(10002, "Belge B");
        given(sourceService.listSources(42, 1, 1))
                .willReturn(new PageImpl<>(List.of(source), PageRequest.of(1, 1), 2));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources?page=1&size=1").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content[0]").asMap().containsOnlyKeys("sourceId",
                "workspaceId", "name", "kind", "createdAt", "fileName", "fileSizeBytes", "version", "pageCount",
                "chapterCount", "chunkCount", "averageTokenCount", "syncedAt", "latestJob");
        assertThat(result).bodyJson().extractingPath("$.content[0].fileName").isEqualTo("belge-b.pdf");
        assertThat(result).bodyJson().extractingPath("$.content[0].fileSizeBytes").isEqualTo(412873);
        assertThat(result).bodyJson().extractingPath("$.content[0].version").isEqualTo("2026.10");
        assertThat(result).bodyJson().extractingPath("$.content[0].pageCount").isEqualTo(70);
        assertThat(result).bodyJson().extractingPath("$.content[0].chapterCount").isEqualTo(11);
        assertThat(result).bodyJson().extractingPath("$.content[0].chunkCount").isEqualTo(154);
        assertThat(result).bodyJson().extractingPath("$.content[0].averageTokenCount").isEqualTo(283);
        assertThat(result).bodyJson().extractingPath("$.content[0].syncedAt").isEqualTo("2026-10-29T13:15:44Z");
        assertThat(result).bodyJson().extractingPath("$.content[0].latestJob.jobId").isEqualTo(7);
        assertThat(result).bodyJson().extractingPath("$.content[0].latestJob.stage").isEqualTo("QUEUED");
        assertThat(result).bodyJson().extractingPath("$.content[0].sourceId").isEqualTo(10002);
        assertThat(result).bodyJson().extractingPath("$.content[0].name").isEqualTo("Belge B");
        assertThat(result).bodyJson().extractingPath("$.content[0].kind").isEqualTo("PDF");
        assertThat(result).bodyJson().extractingPath("$.content[0].createdAt").isEqualTo("2026-10-31T09:00:00Z");
        assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
    }

    @Test
    void givenPageMinus1_whenListSources_thenReturns400OnPage() {
        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources?page=-1").exchange();

        // Assert
        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("page");
    }

    @Test
    void givenSize101_whenListSources_thenReturns400OnSize() {
        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources?size=101").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("size");
    }

    @Test
    void givenWorkspace43_whenListSources_thenReturns404WorkspaceNotFound() {
        // Arrange
        given(sourceService.listSources(43, 0, 20)).willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok"));

        // Act
        var result = mvc.get().uri("/api/workspaces/43/sources").exchange();

        // Assert
        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenRetryAccepted_whenRetryIngestion_thenReturns202WithLocation() {
        // Arrange
        given(ingestionService.retryIngestion(42, 10001)).willReturn(queuedJob(8));

        // Act
        var result = mvc.post().uri("/api/workspaces/42/sources/10001/ingestion-jobs").exchange();

        // Assert
        assertThat(result).hasStatus(202).headers().hasValue("Location", "/api/workspaces/42/ingestion-jobs/8");
        assertThat(result).bodyJson().extractingPath("$.jobId").isEqualTo(8);
        assertThat(result).bodyJson().extractingPath("$.sourceId").isEqualTo(10001);
    }

    @Test
    void givenSourceAlreadyIngested_whenRetryIngestion_thenReturns409() {
        // Arrange
        given(ingestionService.retryIngestion(42, 10001))
                .willThrow(new ConflictException("SOURCE_ALREADY_INGESTED", "done"));

        // Act
        var result = mvc.post().uri("/api/workspaces/42/sources/10001/ingestion-jobs").exchange();

        // Assert
        assertThat(result).hasStatus(409).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("SOURCE_ALREADY_INGESTED");
    }

    @Test
    void givenActiveJob_whenRetryIngestion_thenReturns409IngestionInProgress() {
        // Arrange
        given(ingestionService.retryIngestion(42, 10001))
                .willThrow(new ConflictException("INGESTION_IN_PROGRESS", "busy"));

        // Act
        var result = mvc.post().uri("/api/workspaces/42/sources/10001/ingestion-jobs").exchange();

        // Assert
        assertThat(result).hasStatus(409);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("INGESTION_IN_PROGRESS");
    }

    @Test
    void givenUnknownSource_whenRetryIngestion_thenReturns404SourceNotFound() {
        // Arrange
        given(ingestionService.retryIngestion(42, 99)).willThrow(new NotFoundException("SOURCE_NOT_FOUND", "yok"));

        // Act
        var result = mvc.post().uri("/api/workspaces/42/sources/99/ingestion-jobs").exchange();

        // Assert
        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenWorkspace43_whenRetryIngestion_thenReturns404WorkspaceNotFound() {
        // Arrange
        given(ingestionService.retryIngestion(43, 10001))
                .willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok"));

        // Act
        var result = mvc.post().uri("/api/workspaces/43/sources/10001/ingestion-jobs").exchange();

        // Assert
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenSource10001_whenGetSource_thenReturns200() {
        // Arrange
        given(sourceService.getSource(42, 10001)).willReturn(sourceResponse(10001, "Rehber"));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.sourceId").isEqualTo(10001);
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Rehber");
        assertThat(result).bodyJson().extractingPath("$.pageCount").isEqualTo(70);
    }

    @Test
    void givenUnknownSource_whenGetSource_thenReturns404SourceNotFound() {
        // Arrange
        given(sourceService.getSource(42, 99)).willThrow(new NotFoundException("SOURCE_NOT_FOUND", "yok"));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/99").exchange();

        // Assert
        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenWorkspace43_whenGetSource_thenReturns404WorkspaceNotFound() {
        // Arrange
        given(sourceService.getSource(43, 10001)).willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok"));

        // Act
        var result = mvc.get().uri("/api/workspaces/43/sources/10001").exchange();

        // Assert
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenNonNumericSourceId_whenGetSource_thenReturns400() {
        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/abc").exchange();

        // Assert
        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
    }

    @Test
    void givenJobs_whenListIngestionJobsPage1Size1_thenReturnsJobsAndPageMetadata() {
        // Arrange
        given(ingestionService.listIngestionJobs(42, 10001, 1, 1))
                .willReturn(new PageImpl<>(List.of(queuedJob(3)), PageRequest.of(1, 1), 3));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/ingestion-jobs?page=1&size=1").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content[0].jobId").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
    }

    @Test
    void givenNoPagingParams_whenListIngestionJobs_thenDefaultsPage0Size20() {
        // Arrange
        given(ingestionService.listIngestionJobs(42, 10001, 0, 20))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/ingestion-jobs").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(20);
    }

    @Test
    void givenSize101_whenListIngestionJobs_thenReturns400OnSize() {
        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/ingestion-jobs?size=101").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("size");
    }

    @Test
    void givenUnknownSource_whenListIngestionJobs_thenReturns404SourceNotFound() {
        // Arrange
        given(ingestionService.listIngestionJobs(42, 99, 0, 20))
                .willThrow(new NotFoundException("SOURCE_NOT_FOUND", "yok"));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/99/ingestion-jobs").exchange();

        // Assert
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenFile_whenGetSourceFile_thenPdfInlineNosniffNoStore() {
        // Arrange
        given(sourceService.getSourceFile(42, 10001))
                .willReturn(new com.rasitbank.retrieval.source.domain.dto.SourceFile("Şirket El Kitabı.pdf",
                        new byte[] {37, 80, 68, 70}));

        // Act
        var result = mvc.get().uri("/api/workspaces/42/sources/10001/file").exchange();

        // Assert
        assertThat(result).hasStatusOk().hasContentType("application/pdf");
        assertThat(result).headers().hasValue("X-Content-Type-Options", "nosniff");
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        var disposition = org.springframework.http.ContentDisposition
                .parse(result.getResponse().getHeader("Content-Disposition"));
        assertThat(disposition.isInline()).isTrue();
        assertThat(disposition.getFilename()).isEqualTo("Şirket El Kitabı.pdf");
        assertThat(result.getResponse().getContentAsByteArray()).containsExactly(37, 80, 68, 70);
    }
}
