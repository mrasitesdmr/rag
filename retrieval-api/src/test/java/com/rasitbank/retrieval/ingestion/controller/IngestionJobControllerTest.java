package com.rasitbank.retrieval.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.rasitbank.retrieval.exception.GlobalExceptionHandler;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.service.IngestionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * İçe alma işi okuma ucunun sözleşmeye uygun gövde ve hata yanıtlarını doğrular.
 */
@WebMvcTest(controllers = IngestionJobController.class)
@Import(GlobalExceptionHandler.class)
class IngestionJobControllerTest {

    private static final Instant CREATED = Instant.parse("2026-10-30T10:00:00Z");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IngestionService ingestionService;

    private static IngestionJobResponse response(IngestionStage stage, IngestionStage failedStage, double progress,
            int embedded, String errorCode, Instant finishedAt) {
        return new IngestionJobResponse(1, 42, 10001, "2026.10", "rehber.pdf", stage, failedStage, progress, embedded,
                150, errorCode, CREATED, CREATED.plusSeconds(1), finishedAt, "/api/workspaces/42/ingestion-jobs/1");
    }

    @Test
    void givenRunningJob_whenGetIngestionJob_thenReturns200WithAllFields() {
        given(ingestionService.getIngestionJob(42, 1))
                .willReturn(response(IngestionStage.EMBEDDING, null, 0.612, 96, null, null));

        var result = mvc.get().uri("/api/workspaces/42/ingestion-jobs/1").exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$").asMap().containsOnlyKeys("jobId",
                "workspaceId", "sourceId", "version", "fileName", "stage", "failedStage", "progress",
                "embeddedChunks", "totalChunks", "errorCode", "createdAt", "startedAt", "finishedAt", "statusUrl");
        assertThat(result).bodyJson().extractingPath("$.jobId").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.workspaceId").isEqualTo(42);
        assertThat(result).bodyJson().extractingPath("$.sourceId").isEqualTo(10001);
        assertThat(result).bodyJson().extractingPath("$.version").isEqualTo("2026.10");
        assertThat(result).bodyJson().extractingPath("$.stage").isEqualTo("EMBEDDING");
        assertThat(result).bodyJson().extractingPath("$.failedStage").isNull();
        assertThat(result).bodyJson().extractingPath("$.progress").isEqualTo(0.612);
        assertThat(result).bodyJson().extractingPath("$.embeddedChunks").isEqualTo(96);
        assertThat(result).bodyJson().extractingPath("$.totalChunks").isEqualTo(150);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isNull();
        assertThat(result).bodyJson().extractingPath("$.finishedAt").isNull();
        assertThat(result).bodyJson().extractingPath("$.statusUrl").isEqualTo("/api/workspaces/42/ingestion-jobs/1");
    }

    @Test
    void givenFailedJob_whenGetIngestionJob_thenReturnsErrorCodeAndFailedStage() {
        given(ingestionService.getIngestionJob(42, 1)).willReturn(response(IngestionStage.FAILED,
                IngestionStage.EMBEDDING, 0.1, 0, "EMBEDDING_UNAVAILABLE", CREATED.plusSeconds(9)));

        var result = mvc.get().uri("/api/workspaces/42/ingestion-jobs/1").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.stage").isEqualTo("FAILED");
        assertThat(result).bodyJson().extractingPath("$.failedStage").isEqualTo("EMBEDDING");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("EMBEDDING_UNAVAILABLE");
        assertThat(result).bodyJson().extractingPath("$.finishedAt").isEqualTo("2026-10-30T10:00:09Z");
    }

    @Test
    void givenUnknownJobId_whenGetIngestionJob_thenReturns404IngestionJobNotFound() {
        given(ingestionService.getIngestionJob(42, 99))
                .willThrow(new NotFoundException("INGESTION_JOB_NOT_FOUND", "yok"));

        var result = mvc.get().uri("/api/workspaces/42/ingestion-jobs/99").exchange();

        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("INGESTION_JOB_NOT_FOUND");
    }

    @Test
    void givenWorkspace43_whenGetIngestionJob_thenReturns404WorkspaceNotFound() {
        given(ingestionService.getIngestionJob(43, 1))
                .willThrow(new NotFoundException("WORKSPACE_NOT_FOUND", "yok"));

        var result = mvc.get().uri("/api/workspaces/43/ingestion-jobs/1").exchange();

        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("WORKSPACE_NOT_FOUND");
    }

    @Test
    void givenNonNumericJobId_whenGetIngestionJob_thenReturns400BadRequest() {
        var result = mvc.get().uri("/api/workspaces/42/ingestion-jobs/abc").exchange();

        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }
}
