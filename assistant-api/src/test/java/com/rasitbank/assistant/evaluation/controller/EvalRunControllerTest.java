package com.rasitbank.assistant.evaluation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.evaluation.domain.dto.EvalOverviewResponse;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunResponse;
import com.rasitbank.assistant.evaluation.domain.entity.EvalRunStatus;
import com.rasitbank.assistant.evaluation.service.EvalRunService;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.GlobalExceptionHandler;
import com.rasitbank.assistant.exception.NotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import java.util.List;

/**
 * Değerlendirme uçlarının durum kodlarını, Location başlığını, parametre doğrulamasını ve hata kodlarını doğrular.
 */
@WebMvcTest(EvalRunController.class)
@Import(GlobalExceptionHandler.class)
class EvalRunControllerTest {

    private static final String URI = "/api/assistant/evals";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private EvalRunService evalRunService;

    private static EvalRunResponse runningRun(long id) {
        return new EvalRunResponse(id, EvalRunStatus.RUNNING, "rehber-golden-v1", RetrievalStrategy.RERANK, 20, 5,
                "2026.10", 100, 100, 0, 0, null, null, null, null, 0, null, Instant.parse("2026-11-02T10:00:00Z"), null,
                null);
    }

    @Test
    void givenAcceptedRun_whenStartEvalRun_thenAcceptedWithLocation() {
        // Arrange
        given(evalRunService.startEvalRun()).willReturn(runningRun(7));

        // Act
        var result = mvc.post().uri(URI).exchange();

        // Assert
        assertThat(result).hasStatus(202);
        assertThat(result).headers().hasValue("Location", "/api/assistant/evals/7");
        assertThat(result).bodyJson().extractingPath("$.evalRunId").isEqualTo(7);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("RUNNING");
        assertThat(result).bodyJson().extractingPath("$.retrievalStrategy").isEqualTo("RERANK");
    }

    @Test
    void givenRunInProgress_whenStartEvalRun_thenConflictWithErrorCode() {
        // Arrange
        given(evalRunService.startEvalRun())
                .willThrow(new ConflictException("EVAL_RUN_IN_PROGRESS", "Another evaluation run is running"));

        // Act
        var result = mvc.post().uri(URI).exchange();

        // Assert
        assertThat(result).hasStatus(409);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("EVAL_RUN_IN_PROGRESS");
    }

    @Test
    void givenNonNumericId_whenGetEvalRun_thenBadRequest() {
        // Act
        var result = mvc.get().uri(URI + "/abc").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void givenUnknownId_whenGetEvalRun_thenNotFound() {
        // Arrange
        given(evalRunService.getEvalRun(999))
                .willThrow(new NotFoundException("EVAL_RUN_NOT_FOUND", "Eval run not found: 999"));

        // Act
        var result = mvc.get().uri(URI + "/999").exchange();

        // Assert
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("EVAL_RUN_NOT_FOUND");
    }

    @Test
    void givenSizeOver100_whenListEvalRuns_thenValidationFailedOnSize() {
        // Act
        var result = mvc.get().uri(URI + "?size=101").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("size");
    }

    @Test
    void givenPageAndSize_whenListEvalRuns_thenServiceGetsParamsAndBodyHasPageMetadata() {
        // Arrange
        given(evalRunService.listEvalRuns(1, 20)).willReturn(new PageImpl<>(List.of(runningRun(3)), PageRequest.of(1, 20), 21));

        // Act
        var result = mvc.get().uri(URI + "?page=1&size=20").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(21);
        assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.content[0].evalRunId").isEqualTo(3);
    }

    @Test
    void givenNoRunsAndNoGoldenSet_whenGetEvalOverview_thenAllFieldsNull() {
        // Arrange
        given(evalRunService.getEvalOverview()).willReturn(new EvalOverviewResponse(null, null, null, null));

        // Act
        var result = mvc.get().uri(URI + "/overview").exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.goldenSet").isNull();
        assertThat(result).bodyJson().extractingPath("$.runningRun").isNull();
        assertThat(result).bodyJson().extractingPath("$.lastDoneRun").isNull();
    }
}
