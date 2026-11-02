package com.rasitbank.assistant.suggestion.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Yüklenen öneri listesindeki bir soruyu, başlangıç önerisi olup olmadığını ve takip sorularını taşır.
 */
public record SuggestedQuestionRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = SuggestedQuestionRequest.TEXT_MAX_LENGTH, message = "size must be at most 200 characters")
        @Nullable String text,
        @Nullable Boolean starter,
        @Size(max = SuggestedQuestionRequest.FOLLOW_UPS_MAX_COUNT, message = "size must be at most 5")
        @Nullable List<
                @NotBlank(message = "must not be blank")
                @Size(max = SuggestedQuestionRequest.TEXT_MAX_LENGTH, message = "size must be at most 200 characters")
                String> followUps) {

    public static final int TEXT_MAX_LENGTH = 200;
    public static final int FOLLOW_UPS_MAX_COUNT = 5;

    public boolean isStarter() {
        return Boolean.TRUE.equals(starter);
    }

    public List<String> followUpsOrEmpty() {
        return Objects.requireNonNullElse(followUps, List.of());
    }
}
