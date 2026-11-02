package com.rasitbank.assistant.suggestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rasitbank.assistant.exception.GlobalExceptionHandler;
import com.rasitbank.assistant.suggestion.domain.dto.ReplaceSuggestionsRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionSetResponse;
import com.rasitbank.assistant.suggestion.service.SuggestionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Öneri uçlarının doğrulama hatalarını alan yoluyla, desteklenmeyen istekleri sabit errorCode'la döndüğünü doğrular.
 */
@WebMvcTest(SuggestionController.class)
@Import(GlobalExceptionHandler.class)
class SuggestionControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String URI = "/api/assistant/suggestions";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SuggestionService suggestionService;

    private static String longText(int length) {
        return "a".repeat(length);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"categories\":[{\"name\":\"K\",\"questions\":[{\"text\":\"\"}]}]}|categories[0].questions[0].text|must not be blank",
            "{\"categories\":[{\"name\":\"K\",\"questions\":[{\"text\":\"%201%\"}]}]}|categories[0].questions[0].text|size must be at most 200 characters",
            "{\"categories\":[{\"name\":\"%41%\",\"questions\":[{\"text\":\"S\"}]}]}|categories[0].name|size must be at most 40 characters",
            "{\"categories\":[{\"name\":\"K\",\"questions\":[]}]}|categories[0].questions|must not be empty",
            "{\"categories\":[{\"name\":\"K\",\"questions\":[{\"text\":\"S\",\"followUps\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"]}]}]}|categories[0].questions[0].followUps|size must be at most 5",
            "{}|categories|must not be null",
            "{\"categories\":[null]}|categories[0]|must not be null"})
    void givenInvalidField_whenReplace_thenValidationFailedWithPath(String template, String field, String message) {
        // Arrange
        String body = template.replace("%201%", longText(201)).replace("%41%", longText(41));

        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).hasContentType(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[?(@.field=='" + field + "')].message")
                .asList().containsExactly(message);
        verify(suggestionService, never()).replaceSuggestions(any());
    }

    @Test
    void givenFiftyOneCategories_whenReplace_thenSizeViolationOnCategories() {
        // Arrange
        String category = "{\"name\":\"K%d\",\"questions\":[{\"text\":\"S%d\"}]}";
        StringBuilder body = new StringBuilder("{\"categories\":[");
        for (int i = 0; i < 51; i++) {
            body.append(i > 0 ? "," : "").append(category.formatted(i, i));
        }
        body.append("]}");

        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body.toString()).exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.violations[?(@.field=='categories')].message")
                .asList().containsExactly("size must be at most 50");
    }

    @Test
    void givenValidBody_whenReplace_thenServiceResponseReturned() {
        // Arrange
        given(suggestionService.replaceSuggestions(any(ReplaceSuggestionsRequest.class)))
                .willReturn(new SuggestionSetResponse(0, 0, List.of()));

        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"categories\":[]}").exchange();

        // Assert
        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.questionCount").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.categories").asList().isEmpty();
    }

    @Test
    void givenTextPlain_whenReplace_thenUnsupportedMediaType() {
        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.TEXT_PLAIN).content("x").exchange();

        // Assert
        assertThat(result).hasStatus(415);
        assertThat(result).hasContentType(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void givenMalformedJson_whenReplace_thenBadRequest() {
        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{").exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).hasContentType(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void givenNonBooleanStarter_whenReplace_thenBadRequest() {
        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categories\":[{\"name\":\"K\",\"questions\":[{\"text\":\"S\",\"starter\":\"evet\"}]}]}")
                .exchange();

        // Assert
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void givenPost_whenSuggestions_thenMethodNotAllowed() {
        // Act
        var result = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{}").exchange();

        // Assert
        assertThat(result).hasStatus(405);
        assertThat(result).hasContentType(PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("METHOD_NOT_ALLOWED");
    }
}
