package com.rasitbank.assistant.suggestion.domain.dto;

import java.util.List;

/**
 * Kayıtlı bir öneri sorusunu, başlangıç önerisi olup olmadığını ve takip sorularını döner.
 */
public record SuggestedQuestionResponse(String text, boolean starter, List<String> followUps) {

    public SuggestedQuestionResponse {
        followUps = List.copyOf(followUps);
    }
}
