package com.rasitbank.assistant.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * GlobalExceptionHandler'ın her hata türünü sabit errorCode'lu RFC 9457 yanıtına çevirdiğini doğrular.
 */
@WebMvcTest
@Import({ErrorProbeController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvcTester mvc;

    @Test
    void givenUnknownPath_whenGet_thenReturns404ProblemWithNotFoundErrorCode() {
        var result = mvc.get().uri("/api/assistant/yok").exchange();

        assertThat(result).hasStatus(404).hasContentType(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("NOT_FOUND");
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo("https://rag.example/errors/not-found");
        assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo("/api/assistant/yok");
    }

    @Test
    void givenGetOnlyEndpoint_whenDelete_thenReturns405MethodNotAllowed() {
        var result = mvc.delete().uri("/api/assistant/probe/get-only").exchange();

        assertThat(result).hasStatus(405);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(result).bodyJson().extractingPath("$.type")
                .isEqualTo("https://rag.example/errors/method-not-allowed");
    }

    @Test
    void givenMalformedJson_whenPost_thenReturns400BadRequest() {
        var result = mvc.post().uri("/api/assistant/probe/echo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        assertThat(result).bodyJson().doesNotHavePath("$.violations");
    }

    @Test
    void givenTextPlainBody_whenPostJsonEndpoint_thenReturns415UnsupportedMediaType() {
        var result = mvc.post().uri("/api/assistant/probe/echo").contentType(MediaType.TEXT_PLAIN)
                .content("hello").exchange();

        assertThat(result).hasStatus(415);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void givenJsonOnlyEndpoint_whenAcceptTextCsv_thenReturns406NotAcceptable() {
        var result = mvc.get().uri("/api/assistant/probe/json-only").header(HttpHeaders.ACCEPT, "text/csv").exchange();

        assertThat(result).hasStatus(406);
    }

    @Test
    void givenBlankRequiredField_whenPost_thenReturns400ValidationFailedWithFieldViolation() {
        var result = mvc.post().uri("/api/assistant/probe/echo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\" \"}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo("https://rag.example/errors/validation-failed");
        assertThat(result).bodyJson().extractingPath("$.violations").asList().hasSize(1);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("text");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must not be blank");
    }

    @Test
    void givenNegativePageParam_whenGet_thenReturns400ValidationFailedWithPageViolation() {
        var result = mvc.get().uri("/api/assistant/probe/page?page=-1").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo("https://rag.example/errors/validation-failed");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("page");
    }

    @Test
    void givenHandlerThrowsUnexpectedException_whenGet_thenReturns500InternalErrorWithoutInternalDetails() {
        var result = mvc.get().uri("/api/assistant/probe/boom").exchange();

        assertThat(result).hasStatus(500);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("INTERNAL_ERROR");
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo("https://rag.example/errors/internal-error");
        assertThat(result).bodyJson().extractingPath("$.instance").asString().endsWith("/probe/boom");
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("An unexpected error occurred");
        assertThat(result).bodyText().doesNotContain("boom-secret").doesNotContain("trace");
    }

    @Test
    void givenValidRequest_whenGet_thenHandlerRunsWithoutProblemResponse() {
        var result = mvc.get().uri("/api/assistant/probe/page?page=0").exchange();

        assertThat(result).hasStatusOk().bodyText().isEqualTo("page 0");
    }

    @Test
    void givenNamedRequestParamBelowMinimum_whenGet_thenViolationFieldUsesDeclaredParamName() {
        var result = mvc.get().uri("/api/assistant/probe/named-page?pageNumber=-5").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("pageNumber");
    }
}
