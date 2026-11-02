package com.rasitbank.assistant.suggestion.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Yüklenen öneri listesindeki bir kategoriyi ve sorularını gösterim sırasıyla taşır.
 */
public record SuggestionCategoryRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = SuggestionCategoryRequest.NAME_MAX_LENGTH, message = "size must be at most 40 characters")
        @Nullable String name,
        @NotEmpty(message = "must not be empty")
        @Nullable List<@NotNull(message = "must not be null") @Valid SuggestedQuestionRequest> questions) {

    public static final int NAME_MAX_LENGTH = 40;
}
