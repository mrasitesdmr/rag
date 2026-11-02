package com.rasitbank.assistant.suggestion.domain.dto;

import java.util.List;

/**
 * Kayıtlı öneri listesinin tamamını soru ve takip sayılarıyla birlikte döner.
 */
public record SuggestionSetResponse(int questionCount, int followUpCount, List<SuggestionCategoryResponse> categories) {

    public SuggestionSetResponse {
        categories = List.copyOf(categories);
    }
}
