package com.rasitbank.assistant.conversation.controller;

import com.rasitbank.assistant.conversation.domain.dto.ConversationDetailResponse;
import com.rasitbank.assistant.conversation.domain.dto.ConversationResponse;
import com.rasitbank.assistant.conversation.domain.dto.ConversationSummaryResponse;
import com.rasitbank.assistant.conversation.domain.dto.CreateConversationRequest;
import com.rasitbank.assistant.conversation.domain.dto.SendMessageRequest;
import com.rasitbank.assistant.conversation.service.AnswerService;
import com.rasitbank.assistant.conversation.service.ConversationReadService;
import com.rasitbank.assistant.conversation.service.ConversationService;
import com.rasitbank.assistant.pagination.PageParameters;
import com.rasitbank.assistant.pagination.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Konuşmaların REST ucudur: konuşma açar, soruyu alıp cevabı SSE akışı olarak döner ve Konuşmalar ekranı için
 * konuşma listesini ve detayını verir.
 */
@RestController
@RequestMapping("/api/assistant/conversations")
public class ConversationController {

    private static final String CONVERSATIONS_PATH = "/api/assistant/conversations/";

    private final ConversationService conversationService;
    private final AnswerService answerService;
    private final ConversationReadService conversationReadService;

    public ConversationController(ConversationService conversationService, AnswerService answerService,
            ConversationReadService conversationReadService) {
        this.conversationService = conversationService;
        this.answerService = answerService;
        this.conversationReadService = conversationReadService;
    }

    @GetMapping
    public PageResponse<ConversationSummaryResponse> listConversations(
            @RequestParam(name = "page", defaultValue = "0")
            @Min(value = 0, message = PageParameters.PAGE_MESSAGE) int page,
            @RequestParam(name = "size", defaultValue = "20")
            @Min(value = 1, message = PageParameters.SIZE_MESSAGE)
            @Max(value = PageParameters.MAX_SIZE, message = PageParameters.SIZE_MESSAGE) int size) {
        return PageResponse.from(conversationReadService.listConversations(page, size));
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse getConversationDetail(@PathVariable long conversationId) {
        return conversationReadService.getConversationDetail(conversationId);
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
