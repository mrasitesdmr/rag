package com.rasitbank.assistant.support;

import com.rasitbank.assistant.config.AssistantProperties;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Birim testlerinde kullanılan AssistantProperties'i varsayılan değerlerle kurar.
 */
public final class TestProperties {

    public static final String NOT_FOUND_ANSWER = "Bu konuda rehberimizde bilgi bulamadım.";
    public static final String PERSONAL_INFO_OPENER =
            "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum.";
    public static final String OTHER_BANKS_OPENER =
            "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum.";
    public static final String INVESTMENT_ADVICE_OPENER = "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum.";
    public static final String GREETING_ANSWER = "Merhaba, ben test asistanıyım. Ne sormak istersin?";
    public static final String THANKS_ANSWER = "Rica ederim. Başka bir sorun olursa yazabilirsin.";
    public static final String FAREWELL_ANSWER = "Görüşmek üzere. Bir sorun olursa buradayım.";
    public static final double MIN_RERANK_SCORE = -2.0;

    private TestProperties() {
    }

    public static AssistantProperties defaults() {
        return create("rerank", 20, 5, Duration.ofSeconds(60), Duration.ofSeconds(30), null, MIN_RERANK_SCORE);
    }

    public static AssistantProperties withStrategy(String strategy) {
        return create(strategy, 20, 5, Duration.ofSeconds(60), Duration.ofSeconds(30), null, MIN_RERANK_SCORE);
    }

    public static AssistantProperties withMinRerankScore(@Nullable Double minRerankScore) {
        return create("rerank", 20, 5, Duration.ofSeconds(60), Duration.ofSeconds(30), null, minRerankScore);
    }

    public static AssistantProperties withLabelTimeout(Duration labelTimeout) {
        return new AssistantProperties(42, null, retrievalApi("http://localhost:8080"),
                retrieval("rerank", MIN_RERANK_SCORE), reranker("http://localhost:8081", Duration.ofSeconds(5)),
                generation("rag", Duration.ofSeconds(60), Duration.ofSeconds(30)),
                new AssistantProperties.QueryRewrite(true, Duration.ofSeconds(20), 1000),
                new AssistantProperties.History(3), new AssistantProperties.Handoff("bireysel"),
                smallTalk(labelTimeout));
    }

    public static AssistantProperties withModelTimeouts(Duration firstToken, Duration tokenIdle) {
        return create("rerank", 20, 5, firstToken, tokenIdle, null, MIN_RERANK_SCORE);
    }

    public static AssistantProperties withHostLabel(String hostLabel) {
        return create("rerank", 20, 5, Duration.ofSeconds(60), Duration.ofSeconds(30), hostLabel, MIN_RERANK_SCORE);
    }

    public static AssistantProperties withQueryRewrite(boolean enabled, Duration timeout, int maxLength) {
        return new AssistantProperties(42, null, retrievalApi("http://localhost:8080"),
                retrieval("rerank", MIN_RERANK_SCORE), reranker("http://localhost:8081", Duration.ofSeconds(5)),
                generation("rag", Duration.ofSeconds(60), Duration.ofSeconds(30)),
                new AssistantProperties.QueryRewrite(enabled, timeout, maxLength),
                new AssistantProperties.History(3), new AssistantProperties.Handoff("bireysel"),
                smallTalk(Duration.ofSeconds(10)));
    }

    public static AssistantProperties withHandoffQueue(String queue) {
        return new AssistantProperties(42, null, retrievalApi("http://localhost:8080"),
                retrieval("rerank", MIN_RERANK_SCORE), reranker("http://localhost:8081", Duration.ofSeconds(5)),
                generation("rag", Duration.ofSeconds(60), Duration.ofSeconds(30)),
                new AssistantProperties.QueryRewrite(true, Duration.ofSeconds(20), 1000),
                new AssistantProperties.History(3), new AssistantProperties.Handoff(queue),
                smallTalk(Duration.ofSeconds(10)));
    }

    public static AssistantProperties withReranker(String baseUrl, Duration readTimeout) {
        return new AssistantProperties(42, null, retrievalApi("http://localhost:8080"),
                retrieval("rerank", MIN_RERANK_SCORE), reranker(baseUrl, readTimeout),
                generation("rag", Duration.ofSeconds(60), Duration.ofSeconds(30)),
                new AssistantProperties.QueryRewrite(true, Duration.ofSeconds(20), 1000),
                new AssistantProperties.History(3), new AssistantProperties.Handoff("bireysel"),
                smallTalk(Duration.ofSeconds(10)));
    }

    public static AssistantProperties withRetrievalApi(String baseUrl) {
        return new AssistantProperties(42, null,
                new AssistantProperties.RetrievalApi(baseUrl, Duration.ofSeconds(1), Duration.ofSeconds(5)),
                retrieval("rerank", MIN_RERANK_SCORE), reranker("http://localhost:8081", Duration.ofSeconds(5)),
                generation("rag", Duration.ofSeconds(60), Duration.ofSeconds(30)),
                new AssistantProperties.QueryRewrite(true, Duration.ofSeconds(20), 1000),
                new AssistantProperties.History(3), new AssistantProperties.Handoff("bireysel"),
                smallTalk(Duration.ofSeconds(10)));
    }

    public static AssistantProperties withMode(String mode, String strategy) {
        return new AssistantProperties(42, null, retrievalApi("http://localhost:8080"),
                retrieval(strategy, MIN_RERANK_SCORE), reranker("http://localhost:8081", Duration.ofSeconds(5)),
                generation(mode, Duration.ofSeconds(60), Duration.ofSeconds(30)),
                new AssistantProperties.QueryRewrite(true, Duration.ofSeconds(20), 1000),
                new AssistantProperties.History(3), new AssistantProperties.Handoff("bireysel"),
                smallTalk(Duration.ofSeconds(10)));
    }

    private static AssistantProperties create(String strategy, int candidates, int topK, Duration firstToken,
            Duration tokenIdle, @Nullable String hostLabel, @Nullable Double minRerankScore) {
        return new AssistantProperties(42, hostLabel, retrievalApi("http://localhost:8080"),
                new AssistantProperties.Retrieval(strategy, candidates, topK, minRerankScore),
                reranker("http://localhost:8081", Duration.ofSeconds(5)),
                generation("rag", firstToken, tokenIdle),
                new AssistantProperties.QueryRewrite(true, Duration.ofSeconds(20), 1000),
                new AssistantProperties.History(3), new AssistantProperties.Handoff("bireysel"),
                smallTalk(Duration.ofSeconds(10)));
    }

    private static AssistantProperties.RetrievalApi retrievalApi(String baseUrl) {
        return new AssistantProperties.RetrievalApi(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(15));
    }

    private static AssistantProperties.Retrieval retrieval(String strategy, @Nullable Double minRerankScore) {
        return new AssistantProperties.Retrieval(strategy, 20, 5, minRerankScore);
    }

    private static AssistantProperties.Reranker reranker(String baseUrl, Duration readTimeout) {
        return new AssistantProperties.Reranker(baseUrl, "bge-reranker-v2-m3", Duration.ofSeconds(1), readTimeout,
                Duration.ofSeconds(15), true);
    }

    private static AssistantProperties.Generation generation(String mode, Duration firstToken, Duration tokenIdle) {
        return new AssistantProperties.Generation(mode, NOT_FOUND_ANSWER, firstToken, tokenIdle,
                Duration.ofSeconds(300), PERSONAL_INFO_OPENER, OTHER_BANKS_OPENER, INVESTMENT_ADVICE_OPENER);
    }

    private static AssistantProperties.SmallTalk smallTalk(Duration labelTimeout) {
        return new AssistantProperties.SmallTalk(GREETING_ANSWER, THANKS_ANSWER, FAREWELL_ANSWER, labelTimeout);
    }
}
