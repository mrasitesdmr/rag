package com.rasitbank.assistant.support;

import com.rasitbank.assistant.config.AssistantProperties;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Birim testlerinde kullanılan AssistantProperties'i varsayılan değerlerle kurar.
 */
public final class TestProperties {

    public static final String NOT_FOUND_ANSWER = "Bu konuda rehberimizde bilgi bulamadım.";

    private TestProperties() {
    }

    public static AssistantProperties defaults() {
        return create("rerank", 20, 5, Duration.ofSeconds(60), Duration.ofSeconds(30), null);
    }

    public static AssistantProperties withStrategy(String strategy) {
        return create(strategy, 20, 5, Duration.ofSeconds(60), Duration.ofSeconds(30), null);
    }

    public static AssistantProperties withModelTimeouts(Duration firstToken, Duration tokenIdle) {
        return create("rerank", 20, 5, firstToken, tokenIdle, null);
    }

    public static AssistantProperties withHostLabel(String hostLabel) {
        return create("rerank", 20, 5, Duration.ofSeconds(60), Duration.ofSeconds(30), hostLabel);
    }

    public static AssistantProperties withReranker(String baseUrl, Duration readTimeout) {
        return new AssistantProperties(42, null,
                new AssistantProperties.RetrievalApi("http://localhost:8080", Duration.ofSeconds(2),
                        Duration.ofSeconds(15)),
                new AssistantProperties.Retrieval("rerank", 20, 5),
                new AssistantProperties.Reranker(baseUrl, "bge-reranker-v2-m3", Duration.ofSeconds(1), readTimeout),
                generation(Duration.ofSeconds(60), Duration.ofSeconds(30)));
    }

    public static AssistantProperties withRetrievalApi(String baseUrl) {
        return new AssistantProperties(42, null,
                new AssistantProperties.RetrievalApi(baseUrl, Duration.ofSeconds(1), Duration.ofSeconds(5)),
                new AssistantProperties.Retrieval("rerank", 20, 5),
                new AssistantProperties.Reranker("http://localhost:8081", "bge-reranker-v2-m3",
                        Duration.ofSeconds(1), Duration.ofSeconds(5)),
                generation(Duration.ofSeconds(60), Duration.ofSeconds(30)));
    }

    private static AssistantProperties create(String strategy, int candidates, int topK, Duration firstToken,
            Duration tokenIdle, @Nullable String hostLabel) {
        return new AssistantProperties(42, hostLabel,
                new AssistantProperties.RetrievalApi("http://localhost:8080", Duration.ofSeconds(2),
                        Duration.ofSeconds(15)),
                new AssistantProperties.Retrieval(strategy, candidates, topK),
                new AssistantProperties.Reranker("http://localhost:8081", "bge-reranker-v2-m3",
                        Duration.ofSeconds(1), Duration.ofSeconds(5)),
                generation(firstToken, tokenIdle));
    }

    private static AssistantProperties.Generation generation(Duration firstToken, Duration tokenIdle) {
        return new AssistantProperties.Generation(NOT_FOUND_ANSWER, firstToken, tokenIdle, Duration.ofSeconds(300));
    }
}
