package com.rasitbank.assistant.handoff.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.GlobalExceptionHandler;
import com.rasitbank.assistant.handoff.domain.dto.HandoffResponse;
import com.rasitbank.assistant.handoff.domain.entity.HandoffReason;
import com.rasitbank.assistant.handoff.service.HandoffService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Aktarma ucunun 202 gövdesini, alan sırasını, Location başlığının olmamasını ve gövde hatalarını doğrular.
 */
@WebMvcTest(HandoffController.class)
@Import(GlobalExceptionHandler.class)
class HandoffControllerTest {

    private static final String URI = "/api/assistant/conversations/1/handoff";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private HandoffService handoffService;

    @Test
    void givenValidBody_whenRequestHandoff_thenAcceptedWithFieldsInContractOrderAndNoLocation() {
        given(handoffService.requestHandoff(eq(1L), any())).willReturn(new HandoffResponse(1L, 12L,
                HandoffReason.NO_ANSWER, "bireysel", Instant.parse("2026-11-02T10:31:05Z")));

        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"NO_ANSWER\",\"turnId\":12}").exchange();

        assertThat(result).hasStatus(202);
        assertThat(result.getResponse().getHeader("Location")).isNull();
        assertThat(new String(result.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(
                "{\"conversationId\":1,\"turnId\":12,\"reason\":\"NO_ANSWER\",\"queue\":\"bireysel\",\"createdAt\":\"2026-11-02T10:31:05Z\"}");
    }

    @Test
    void givenNotAllowedTurn_whenRequestHandoff_thenConflictProblem() {
        given(handoffService.requestHandoff(eq(1L), any()))
                .willThrow(new ConflictException("HANDOFF_NOT_ALLOWED", "not allowed"));

        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"NO_ANSWER\",\"turnId\":12}").exchange();

        assertThat(result).hasStatus(409);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("HANDOFF_NOT_ALLOWED");
    }

    @Test
    void givenMissingTurnId_whenRequestHandoff_thenValidationFailed() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"NO_ANSWER\"}")
                .exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("turnId");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must not be null");
        verifyNoInteractions(handoffService);
    }

    @Test
    void givenMissingReason_whenRequestHandoff_thenValidationFailed() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"turnId\":12}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("reason");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must not be null");
        verifyNoInteractions(handoffService);
    }

    @Test
    void givenEmptyBody_whenRequestHandoff_thenBothFieldsReported() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[*].field")
                .asList().containsExactlyInAnyOrder("turnId", "reason");
        verifyNoInteractions(handoffService);
    }

    @Test
    void givenUnknownReason_whenRequestHandoff_thenBadRequest() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\",\"turnId\":12}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(handoffService);
    }

    @Test
    void givenMalformedJson_whenRequestHandoff_thenBadRequest() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void givenNonNumericConversationId_whenRequestHandoff_thenBadRequest() {
        var result = mvc.post().uri("/api/assistant/conversations/abc/handoff").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"NO_ANSWER\",\"turnId\":12}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(handoffService);
    }

    @Test
    void givenTextPlain_whenRequestHandoff_thenUnsupportedMediaType() {
        var result = mvc.post().uri(URI).contentType(MediaType.TEXT_PLAIN).content("x").exchange();

        assertThat(result).hasStatus(415);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"\"abc\"", "{}", "[1]"})
    void givenUnconvertibleTurnId_whenRequestHandoff_thenBadRequest(String turnId) {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"NO_ANSWER\",\"turnId\":" + turnId + "}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(handoffService);
    }
}
