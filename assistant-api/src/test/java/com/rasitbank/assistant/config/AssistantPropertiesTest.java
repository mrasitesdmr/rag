package com.rasitbank.assistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Asistan ayarlarının varsayılanlarla bağlandığını ve geçersiz strateji ya da top-k ile açılışın düştüğünü doğrular.
 */
class AssistantPropertiesTest {

    private static final String[] BASE = {
            "assistant.workspace-id=42",
            "assistant.retrieval-api.base-url=http://r",
            "assistant.retrieval-api.connect-timeout=2s",
            "assistant.retrieval-api.read-timeout=15s",
            "assistant.retrieval.strategy=rerank",
            "assistant.retrieval.candidates=20",
            "assistant.retrieval.top-k=5",
            "assistant.reranker.base-url=http://x",
            "assistant.reranker.model=m",
            "assistant.reranker.connect-timeout=1s",
            "assistant.reranker.read-timeout=5s",
            "assistant.generation.not-found-answer=yok",
            "assistant.generation.first-token-timeout=60s",
            "assistant.generation.token-idle-timeout=30s",
            "assistant.generation.stream-timeout=300s"
    };

    @Configuration
    @EnableConfigurationProperties(AssistantProperties.class)
    static class Config {
    }

    private ApplicationContextRunner runner(String... overrides) {
        return new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withPropertyValues(BASE).withPropertyValues(overrides);
    }

    @Test
    void givenDefaults_whenBound_thenRerank20To5() {
        runner().run(context -> {
            AssistantProperties properties = context.getBean(AssistantProperties.class);

            assertThat(properties.retrieval().configuredStrategy()).isEqualTo(RetrievalStrategy.RERANK);
            assertThat(properties.retrieval().candidates()).isEqualTo(20);
            assertThat(properties.retrieval().topK()).isEqualTo(5);
            assertThat(properties.hostLabel()).isNull();
        });
    }

    @Test
    void givenTop5Strategy_whenBound_thenTopK() {
        runner("assistant.retrieval.strategy=top5").run(context ->
                assertThat(context.getBean(AssistantProperties.class).retrieval().configuredStrategy())
                        .isEqualTo(RetrievalStrategy.TOP_K));
    }

    @Test
    void givenStrategyFoo_whenBound_thenContextFails() {
        runner("assistant.retrieval.strategy=foo").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenTopKGreaterThanCandidates_whenBound_thenContextFails() {
        runner("assistant.retrieval.top-k=21").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenTopKEqualToCandidates_whenBound_thenAccepted() {
        runner("assistant.retrieval.top-k=20").run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void givenCandidatesAboveFifty_whenBound_thenContextFails() {
        runner("assistant.retrieval.candidates=51").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenBlankHostLabel_whenBound_thenNullAndFilledLabelIsStripped() {
        runner("assistant.host-label=  ").run(context ->
                assertThat(context.getBean(AssistantProperties.class).hostLabel()).isNull());
        runner("assistant.host-label= Apple M5 · 16 GB ").run(context ->
                assertThat(context.getBean(AssistantProperties.class).hostLabel()).isEqualTo("Apple M5 · 16 GB"));
    }
}
