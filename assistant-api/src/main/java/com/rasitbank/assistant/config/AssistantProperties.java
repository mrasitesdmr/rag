package com.rasitbank.assistant.config;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Asistanın sunucu ayarlarını taşır: çalışma alanı, retrieval-api ve reranker adresleri, retrieval stratejisi,
 * zaman aşımları ve sabit "bulamadım" cümlesi.
 */
@Validated
@ConfigurationProperties("assistant")
public record AssistantProperties(
        long workspaceId,
        @Nullable String hostLabel,
        @Valid @NotNull RetrievalApi retrievalApi,
        @Valid @NotNull Retrieval retrieval,
        @Valid @NotNull Reranker reranker,
        @Valid @NotNull Generation generation) {

    public AssistantProperties {
        hostLabel = hostLabel == null || hostLabel.isBlank() ? null : hostLabel.strip();
    }

    public record RetrievalApi(@NotBlank String baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
    }

    public record Retrieval(
            @NotNull @Pattern(regexp = "top5|rerank", message = "must be one of top5, rerank") String strategy,
            @Min(1) @Max(50) int candidates,
            @Min(1) int topK) {

        public RetrievalStrategy configuredStrategy() {
            return "top5".equals(strategy) ? RetrievalStrategy.TOP_K : RetrievalStrategy.RERANK;
        }

        @AssertTrue(message = "top-k must not be greater than candidates")
        public boolean isTopKWithinCandidates() {
            return topK <= candidates;
        }
    }

    public record Reranker(
            @NotBlank String baseUrl,
            @NotBlank String model,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout) {
    }

    public record Generation(
            @NotBlank String notFoundAnswer,
            @NotNull Duration firstTokenTimeout,
            @NotNull Duration tokenIdleTimeout,
            @NotNull Duration streamTimeout) {
    }
}
