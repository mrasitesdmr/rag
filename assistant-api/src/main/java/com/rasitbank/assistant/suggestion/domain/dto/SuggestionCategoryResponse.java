package com.rasitbank.assistant.suggestion.domain.dto;

import java.util.List;

/**
 * Kayıtlı öneri listesindeki bir kategoriyi ve sorularını gösterim sırasıyla döner.
 */
public record SuggestionCategoryResponse(String name, List<SuggestedQuestionResponse> questions) {

    public SuggestionCategoryResponse {
        questions = List.copyOf(questions);
    }
}
