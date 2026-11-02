package com.rasitbank.retrieval.chunk.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Hangi parçalarda geçtiği aranacak metinleri (golden set'in cevap cümleleri) taşır.
 */
public record FindChunkMatchesRequest(
        @NotNull(message = "must not be null")
        @Size(min = FindChunkMatchesRequest.TEXTS_MIN, max = FindChunkMatchesRequest.TEXTS_MAX,
                message = "size must be between 1 and 500")
        @Nullable List<
                @NotBlank(message = "must not be blank")
                @Size(max = FindChunkMatchesRequest.TEXT_MAX_LENGTH, message = "size must be at most 1000 characters")
                @Nullable String> texts) {

    public static final int TEXTS_MIN = 1;
    public static final int TEXTS_MAX = 500;
    public static final int TEXT_MAX_LENGTH = 1000;
}
