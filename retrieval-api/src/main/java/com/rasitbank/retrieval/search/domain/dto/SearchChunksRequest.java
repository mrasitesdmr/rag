package com.rasitbank.retrieval.search.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Vektör arama isteğini taşır: aranacak metin ve en fazla kaç parça döneceği.
 */
public record SearchChunksRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = SearchChunksRequest.QUERY_MAX_LENGTH, message = "size must be at most 1000 characters")
        @Nullable String query,
        @NotNull(message = "must not be null")
        @Min(value = SearchChunksRequest.TOP_K_MIN, message = SearchChunksRequest.TOP_K_MESSAGE)
        @Max(value = SearchChunksRequest.TOP_K_MAX, message = SearchChunksRequest.TOP_K_MESSAGE)
        @Nullable Integer topK) {

    public static final int QUERY_MAX_LENGTH = 1000;
    public static final int TOP_K_MIN = 1;
    public static final int TOP_K_MAX = 50;
    public static final String TOP_K_MESSAGE = "must be between 1 and 50";
}
