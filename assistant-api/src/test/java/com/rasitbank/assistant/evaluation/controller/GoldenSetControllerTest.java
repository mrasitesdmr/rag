package com.rasitbank.assistant.evaluation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rasitbank.assistant.evaluation.domain.dto.GoldenSetSummaryResponse;
import com.rasitbank.assistant.evaluation.service.GoldenSetService;
import com.rasitbank.assistant.exception.GlobalExceptionHandler;
import com.rasitbank.assistant.exception.NotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Golden set yükleme isteğinin tüm doğrulama ihlallerini alan yoluyla bildirdiğini ve set yokken 404 döndüğünü doğrular.
 */
@WebMvcTest(GoldenSetController.class)
@Import(GlobalExceptionHandler.class)
class GoldenSetControllerTest {

    private static final String URI = "/api/assistant/golden-set";
    private static final String ANSWER_OK = "Kartınızı kaybettiyseniz hemen bildirin.";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private GoldenSetService goldenSetService;

    private static String question(String text, int page, String answer) {
        return "{\"category\":\"Kartlar\",\"text\":\"" + text + "\",\"pageNumber\":" + page + ",\"answerText\":\""
                + answer + "\"}";
    }

    @Test
    void givenInvalidGoldenSet_whenReplaceGoldenSet_thenReportsAllViolations() {
        // Arrange: [0] answerText 19 karakter, [1] text [0] ile normalize aynı, [2] pageNumber 0
        String body = "{\"version\":\"v1\",\"questions\":["
                + question("Kartımı kaybettim?", 3, "a".repeat(19)) + ","
                + question("kartımı KAYBETTİM", 4, ANSWER_OK) + ","
                + question("Limit nasıl artar?", 0, ANSWER_OK) + "]}";

        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        // Assert
        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations").asList().hasSize(3);
        assertThat(result).bodyJson().extractingPath("$.violations[?(@.field=='questions[1].text')].message")
                .asList().containsExactly("must be unique");
        assertThat(result).bodyJson().extractingPath("$.violations[?(@.field=='questions[0].answerText')].message")
                .asList().containsExactly("size must be between 20 and 300 characters");
        assertThat(result).bodyJson().extractingPath("$.violations[?(@.field=='questions[2].pageNumber')].message")
                .asList().containsExactly("must be greater than or equal to 1");
        verify(goldenSetService, never()).replaceGoldenSet(any());
    }

    @Test
    void givenAnswerTextOf300AndPaddedTo20Chars_whenReplaceGoldenSet_thenBoundariesAccepted() {
        // Arrange: kırpıldıktan sonra tam 20 ve tam 300 karakter geçerlidir
        String twenty = "  " + "b".repeat(20) + "  ";
        String body = "{\"version\":\"v1\",\"questions\":[" + question("Soru bir?", 1, twenty) + ","
                + question("Soru iki?", 1, "c".repeat(300)) + "]}";
        given(goldenSetService.replaceGoldenSet(any()))
                .willReturn(new GoldenSetSummaryResponse("v1", 2, Instant.parse("2026-11-02T10:00:00Z")));

        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        // Assert
        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.questionCount").isEqualTo(2);
    }

    @Test
    void givenAnswerTextOf301Chars_whenReplaceGoldenSet_thenValidationFailed() {
        // Arrange
        String body = "{\"version\":\"v1\",\"questions\":[" + question("Soru bir?", 1, "c".repeat(301)) + "]}";

        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("questions[0].answerText");
    }

    @Test
    void givenEmptyQuestionsAndLongVersion_whenReplaceGoldenSet_thenBothReported() {
        // Arrange
        String body = "{\"version\":\"" + "v".repeat(41) + "\",\"questions\":[]}";

        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[?(@.field=='version')].message").asList()
                .containsExactly("size must be at most 40 characters");
        assertThat(result).bodyJson().extractingPath("$.violations[?(@.field=='questions')].message").asList()
                .containsExactly("size must be between 1 and 500");
    }

    @Test
    void givenNoGoldenSet_whenGetGoldenSet_thenReturns404GoldenSetNotLoaded() {
        // Arrange
        given(goldenSetService.getGoldenSet())
                .willThrow(new NotFoundException("GOLDEN_SET_NOT_LOADED", "No golden set has been loaded"));

        // Act
        var result = mvc.get().uri(URI).exchange();

        // Assert
        assertThat(result).hasStatus(404);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("GOLDEN_SET_NOT_LOADED");
    }
}
