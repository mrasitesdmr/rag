package com.rasitbank.assistant.config;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.generation.MessageLabel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Asistanın sunucu ayarlarını taşır: çalışma alanı, üretim modu, retrieval-api ve reranker adresleri, reranker sağlık
 * kontrolü, retrieval stratejisi ve reranker puan eşiği, sorgu yeniden yazma, konuşma geçmişi sınırı, temsilci kuyruğu,
 * selamlaşma karşılıkları, zaman aşımları, sabit "bulamadım" cümlesi ve hedefli soruların giriş cümleleri.
 */
@Validated
@ConfigurationProperties("assistant")
public record AssistantProperties(
        long workspaceId,
        @Nullable String hostLabel,
        @Valid @NotNull RetrievalApi retrievalApi,
        @Valid @NotNull Retrieval retrieval,
        @Valid @NotNull Reranker reranker,
        @Valid @NotNull Generation generation,
        @Valid @NotNull QueryRewrite queryRewrite,
        @Valid @NotNull History history,
        @Valid @NotNull Handoff handoff,
        @Valid @NotNull SmallTalk smallTalk) {

    public AssistantProperties {
        hostLabel = hostLabel == null || hostLabel.isBlank() ? null : hostLabel.strip();
    }

    public record RetrievalApi(@NotBlank String baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
    }

    public record Retrieval(
            @NotNull @Pattern(regexp = "top5|rerank", message = "must be one of top5, rerank") String strategy,
            @Min(1) @Max(50) int candidates,
            @Min(1) int topK,
            @Nullable Double minRerankScore) {

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
            @NotNull Duration readTimeout,
            @NotNull Duration healthCheckInterval,
            boolean healthCheckEnabled) {
    }

    public record Generation(
            @NotNull @Pattern(regexp = "rag|llm", message = "must be one of rag, llm") String mode,
            @NotBlank String notFoundAnswer,
            @NotNull Duration firstTokenTimeout,
            @NotNull Duration tokenIdleTimeout,
            @NotNull Duration streamTimeout,
            @NotBlank String personalInfoOpener,
            @NotBlank String otherBanksOpener,
            @NotBlank String investmentAdviceOpener) {

        public List<String> targetedOpeners() {
            return List.of(personalInfoOpener, otherBanksOpener, investmentAdviceOpener);
        }

        public GenerationMode configuredMode() {
            return "llm".equals(mode) ? GenerationMode.LLM_ONLY : GenerationMode.RAG;
        }
    }

    public record QueryRewrite(boolean enabled, @NotNull Duration timeout, @Min(1) @Max(1000) int maxLength) {
    }

    public record History(@Min(1) @Max(10) int maxTurns) {
    }

    public record Handoff(@NotBlank @Size(max = 40) String queue) {
    }

    public record SmallTalk(
            @NotBlank String greetingAnswer,
            @NotBlank String thanksAnswer,
            @NotBlank String farewellAnswer,
            @NotNull Duration labelTimeout) {

        public String answerFor(MessageLabel label) {
            return switch (label) {
                case GREETING -> greetingAnswer;
                case THANKS -> thanksAnswer;
                case FAREWELL -> farewellAnswer;
                case OTHER -> throw new IllegalArgumentException("No small talk answer for label " + label);
            };
        }
    }
}
