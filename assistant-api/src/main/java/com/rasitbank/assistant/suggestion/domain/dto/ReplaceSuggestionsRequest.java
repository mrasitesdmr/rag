package com.rasitbank.assistant.suggestion.domain.dto;

import com.rasitbank.assistant.suggestion.domain.validation.ValidSuggestionSet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Ürün ekibinin yüklediği tam öneri listesini taşır; kayıtlı listenin yerine geçer.
 */
@ValidSuggestionSet
public record ReplaceSuggestionsRequest(
        @NotNull(message = "must not be null")
        @Size(max = ReplaceSuggestionsRequest.CATEGORIES_MAX_COUNT, message = "size must be at most 50")
        @Nullable List<@NotNull(message = "must not be null") @Valid SuggestionCategoryRequest> categories) {

    public static final int CATEGORIES_MAX_COUNT = 50;
    public static final int QUESTIONS_MAX_TOTAL = 500;
}
