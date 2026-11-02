package com.rasitbank.assistant.evaluation.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Yüklenen golden set'teki bir soruyu taşır: kategori, soru metni, sayfa ve cevabın geçtiği cümle.
 */
public record GoldenQuestionRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = GoldenQuestionRequest.CATEGORY_MAX_LENGTH, message = "size must be at most 40 characters")
        @Nullable String category,
        @NotBlank(message = "must not be blank")
        @Size(max = GoldenQuestionRequest.TEXT_MAX_LENGTH, message = "size must be at most 200 characters")
        @Nullable String text,
        @NotNull(message = "must not be null")
        @Min(value = 1, message = "must be greater than or equal to 1")
        @Nullable Integer pageNumber,
        @NotBlank(message = "must not be blank")
        @Nullable String answerText) {

    public static final int CATEGORY_MAX_LENGTH = 40;
    public static final int TEXT_MAX_LENGTH = 200;
    public static final int ANSWER_TEXT_MIN_LENGTH = 20;
    public static final int ANSWER_TEXT_MAX_LENGTH = 300;
}
