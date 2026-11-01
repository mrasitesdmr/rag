package com.rasitbank.assistant.conversation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.conversation.domain.dto.ConversationResponse;
import com.rasitbank.assistant.conversation.domain.entity.Channel;
import com.rasitbank.assistant.conversation.service.AnswerService;
import com.rasitbank.assistant.conversation.service.ConversationService;
import com.rasitbank.assistant.exception.GlobalExceptionHandler;
import java.time.Instant;
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
}
