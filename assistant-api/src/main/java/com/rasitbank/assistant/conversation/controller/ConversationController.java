package com.rasitbank.assistant.conversation.controller;

import com.rasitbank.assistant.conversation.domain.dto.ConversationResponse;
import com.rasitbank.assistant.conversation.domain.dto.CreateConversationRequest;
import com.rasitbank.assistant.conversation.domain.dto.SendMessageRequest;
import com.rasitbank.assistant.conversation.service.AnswerService;
import com.rasitbank.assistant.conversation.service.ConversationService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Önizleme konuşmalarının REST ucudur: konuşma açar ve soruyu alıp cevabı SSE akışı olarak döner.
 */
@RestController
@RequestMapping("/api/assistant/conversations")
public class ConversationController {

    private static final String CONVERSATIONS_PATH = "/api/assistant/conversations/";

    private final ConversationService conversationService;
    private final AnswerService answerService;

    public ConversationController(ConversationService conversationService, AnswerService answerService) {
        this.conversationService = conversationService;
        this.answerService = answerService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse conversation = conversationService.createConversation(request);
        return ResponseEntity.created(URI.create(CONVERSATIONS_PATH + conversation.conversationId()))
                .body(conversation);
    }

    @PostMapping(path = "/{conversationId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> sendMessage(@PathVariable long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(answerService.sendMessage(conversationId, request));
    }
}
