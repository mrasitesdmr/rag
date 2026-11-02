package com.rasitbank.assistant.handoff.controller;

import com.rasitbank.assistant.handoff.domain.dto.HandoffResponse;
import com.rasitbank.assistant.handoff.domain.dto.RequestHandoffRequest;
import com.rasitbank.assistant.handoff.service.HandoffService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Önizleme'deki "Temsilciye bağlan" düğmesinin REST ucudur: konuşmayı temsilci kuyruğuna alır ve kaydı 202 ile döner.
 */
@RestController
@RequestMapping("/api/assistant/conversations")
public class HandoffController {

    private final HandoffService handoffService;

    public HandoffController(HandoffService handoffService) {
        this.handoffService = handoffService;
    }

    @PostMapping(path = "/{conversationId}/handoff", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HandoffResponse> requestHandoff(@PathVariable long conversationId,
            @Valid @RequestBody RequestHandoffRequest request) {
        return ResponseEntity.accepted().body(handoffService.requestHandoff(conversationId, request));
    }
}
