package com.rasitbank.assistant.conversation.controller;

import com.rasitbank.assistant.conversation.domain.dto.SubmitFeedbackRequest;
import com.rasitbank.assistant.conversation.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Önizleme'deki "Faydalı mı?" satırının REST ucudur: tura geri bildirimi kaydeder.
 */
@RestController
@RequestMapping("/api/assistant/turns")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping(path = "/{turnId}/feedback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> submitFeedback(@PathVariable long turnId,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        feedbackService.submitFeedback(turnId, request);
        return ResponseEntity.noContent().build();
    }
}
