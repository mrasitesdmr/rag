package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.conversation.domain.dto.SubmitFeedbackRequest;
import com.rasitbank.assistant.conversation.service.FeedbackService;
import com.rasitbank.assistant.exception.GlobalExceptionHandler;
import com.rasitbank.assistant.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Geri bildirim ucunun status, gövde doğrulaması ve Problem Details sözleşmesini doğrular.
 */
@WebMvcTest(FeedbackController.class)
@Import(GlobalExceptionHandler.class)
class FeedbackControllerTest {

    private static final String URI = "/api/assistant/turns/12/feedback";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @Test
    void givenHelpfulFalse_whenSubmitFeedback_thenNoContentAndServiceGetsTurnIdAndValue() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"helpful\":false}")
                .exchange();

        assertThat(result).hasStatus(204);
        assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
        ArgumentCaptor<SubmitFeedbackRequest> captor = ArgumentCaptor.forClass(SubmitFeedbackRequest.class);
        verify(feedbackService).submitFeedback(eq(12L), captor.capture());
        assertThat(captor.getValue().helpful()).isFalse();
    }

    @Test
    void givenUnknownTurnId_whenSubmitFeedback_thenTurnNotFound() {
        willThrow(new NotFoundException("TURN_NOT_FOUND", "Turn not found: 12")).given(feedbackService)
                .submitFeedback(eq(12L), any());

        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"helpful\":true}")
                .exchange();

        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("TURN_NOT_FOUND");
    }

    @Test
    void givenMissingHelpful_whenSubmitFeedback_thenValidationFailed() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("helpful");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must not be null");
        verifyNoInteractions(feedbackService);
    }

    @Test
    void givenNullHelpful_whenSubmitFeedback_thenValidationFailed() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"helpful\":null}")
                .exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        verifyNoInteractions(feedbackService);
    }

    @Test
    void givenMalformedJson_whenSubmitFeedback_thenBadRequest() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"helpful\":").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(feedbackService);
    }

    @Test
    void givenUnconvertibleHelpful_whenSubmitFeedback_thenBadRequest() {
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"helpful\":\"yes\"}")
                .exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(feedbackService);
    }

    @Test
    void givenNonNumericTurnId_whenSubmitFeedback_thenBadRequest() {
        var result = mvc.post().uri("/api/assistant/turns/abc/feedback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"helpful\":true}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(feedbackService);
    }

    @Test
    void givenTextPlain_whenSubmitFeedback_thenUnsupportedMediaType() {
        var result = mvc.post().uri(URI).contentType(MediaType.TEXT_PLAIN).content("true").exchange();

        assertThat(result).hasStatus(415);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
        verifyNoInteractions(feedbackService);
    }
}
