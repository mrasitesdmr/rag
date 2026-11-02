package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.conversation.domain.dto.ConversationResponse;
import com.rasitbank.assistant.conversation.domain.entity.Channel;
import com.rasitbank.assistant.conversation.service.AnswerService;
import com.rasitbank.assistant.conversation.service.ConversationService;
import com.rasitbank.assistant.conversation.service.ConversationReadService;
import com.rasitbank.assistant.exception.GlobalExceptionHandler;
import com.rasitbank.assistant.conversation.domain.dto.ConversationSummaryResponse;
import com.rasitbank.assistant.conversation.domain.entity.TurnStatus;
import com.rasitbank.assistant.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Konuşma açma ucunun status, Location ve doğrulama hatası sözleşmesini doğrular.
 */
@WebMvcTest(ConversationController.class)
@Import(GlobalExceptionHandler.class)
class ConversationControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private AnswerService answerService;

    @MockitoBean
    private ConversationReadService conversationReadService;

    @Test
    void givenPreviewChannel_whenCreateConversation_thenReturns201WithLocation() {
        given(conversationService.createConversation(any())).willReturn(
                new ConversationResponse(1L, Channel.PREVIEW, Instant.parse("2026-11-01T10:00:00Z")));

        var result = mvc.post().uri("/api/assistant/conversations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"PREVIEW\"}").exchange();

        assertThat(result).hasStatus(201).hasHeader("Location", "/api/assistant/conversations/1");
        assertThat(result).bodyJson().extractingPath("$.conversationId").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.channel").isEqualTo("PREVIEW");
        assertThat(result).bodyJson().extractingPath("$.createdAt").isEqualTo("2026-11-01T10:00:00Z");
    }

    @Test
    void givenMissingChannel_whenCreateConversation_thenReturns400OnChannel() {
        var result = mvc.post().uri("/api/assistant/conversations").contentType(MediaType.APPLICATION_JSON)
                .content("{}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo("channel");
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo("must not be null");
        verifyNoInteractions(conversationService);
    }

    @Test
    void givenUnknownChannelWeb_whenCreateConversation_thenReturns400BadRequest() {
        var result = mvc.post().uri("/api/assistant/conversations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"WEB\"}").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(conversationService);
    }

    @Test
    void givenTextPlainBody_whenCreateConversation_thenReturns415() {
        var result = mvc.post().uri("/api/assistant/conversations").contentType(MediaType.TEXT_PLAIN)
                .content("PREVIEW").exchange();

        assertThat(result).hasStatus(415);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @ParameterizedTest
    @CsvSource({
            "page=-1,page,must be greater than or equal to 0",
            "size=0,size,must be between 1 and 100",
            "size=101,size,must be between 1 and 100"})
    void givenInvalidPageOrSize_whenListConversations_thenValidationFailed(String query, String field, String message) {
        // Arrange
        String uri = "/api/assistant/conversations?" + query;

        // Act
        var result = mvc.get().uri(uri).exchange();

        // Assert
        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.violations[0].field").isEqualTo(field);
        assertThat(result).bodyJson().extractingPath("$.violations[0].message").isEqualTo(message);
        verifyNoInteractions(conversationReadService);
    }

    @Test
    void givenNonNumericSize_whenListConversations_thenBadRequest() {
        var result = mvc.get().uri("/api/assistant/conversations?size=abc").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
    }

    @Test
    void givenNoParameters_whenListConversations_thenDefaultsPageZeroSizeTwentyAndPageResponseShape() {
        // Arrange
        var row = new ConversationSummaryResponse(12L, Channel.PREVIEW, Instant.parse("2026-11-02T10:31:05Z"), 2,
                "Kartımı kaybettim", TurnStatus.GROUNDED, true);
        given(conversationReadService.listConversations(0, 20)).willReturn(
                new PageImpl<>(List.of(row), PageRequest.of(0, 20), 41));

        // Act
        var result = mvc.get().uri("/api/assistant/conversations").exchange();

        // Assert
        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(41);
        assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(20);
        assertThat(result).bodyJson().extractingPath("$.content[0].conversationId").isEqualTo(12);
        assertThat(result).bodyJson().extractingPath("$.content[0].lastTurnStatus").isEqualTo("GROUNDED");
        assertThat(result).bodyJson().extractingPath("$.content[0].handedOff").isEqualTo(true);
    }

    @Test
    void givenUnknownConversation_whenGetConversationDetail_thenConversationNotFound() {
        // Arrange
        given(conversationReadService.getConversationDetail(99L))
                .willThrow(new NotFoundException("CONVERSATION_NOT_FOUND", "missing"));

        // Act
        var result = mvc.get().uri("/api/assistant/conversations/99").exchange();

        // Assert
        assertThat(result).hasStatus(404).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("CONVERSATION_NOT_FOUND");
    }

    @Test
    void givenNonNumericId_whenGetConversationDetail_thenBadRequest() {
        var result = mvc.get().uri("/api/assistant/conversations/abc").exchange();

        assertThat(result).hasStatus(400).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.errorCode").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(conversationReadService);
    }
}
