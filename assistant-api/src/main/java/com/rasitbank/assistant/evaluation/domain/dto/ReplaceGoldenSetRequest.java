package com.rasitbank.assistant.evaluation.domain.dto;

import com.rasitbank.assistant.evaluation.domain.validation.ValidGoldenSet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Ürün ekibinin yüklediği tam golden set'i taşır; kayıtlı setin yerine geçer.
 */
@ValidGoldenSet
public record ReplaceGoldenSetRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = ReplaceGoldenSetRequest.VERSION_MAX_LENGTH, message = "size must be at most 40 characters")
        @Nullable String version,
        @NotNull(message = "must not be null")
        @Size(min = ReplaceGoldenSetRequest.QUESTIONS_MIN, max = ReplaceGoldenSetRequest.QUESTIONS_MAX,
                message = "size must be between 1 and 500")
        @Nullable List<@NotNull(message = "must not be null") @Valid GoldenQuestionRequest> questions) {

    public static final int VERSION_MAX_LENGTH = 40;
    public static final int QUESTIONS_MIN = 1;
    public static final int QUESTIONS_MAX = 500;
}
