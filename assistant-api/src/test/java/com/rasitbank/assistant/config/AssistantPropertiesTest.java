package com.rasitbank.assistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.generation.MessageLabel;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
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
            "assistant.reranker.health-check-interval=15s",
            "assistant.reranker.health-check-enabled=true",
            "assistant.generation.mode=rag",
            "assistant.generation.not-found-answer=yok",
            "assistant.generation.first-token-timeout=60s",
            "assistant.generation.token-idle-timeout=30s",
            "assistant.generation.stream-timeout=300s",
            "assistant.generation.personal-info-opener=Kisisel.",
            "assistant.generation.other-banks-opener=Baska banka.",
            "assistant.generation.investment-advice-opener=Yatirim.",
            "assistant.small-talk.greeting-answer=Merhaba",
            "assistant.small-talk.thanks-answer=Rica ederim",
            "assistant.small-talk.farewell-answer=Gorusuruz",
            "assistant.small-talk.label-timeout=10s",
            "assistant.query-rewrite.enabled=true",
            "assistant.query-rewrite.timeout=20s",
            "assistant.query-rewrite.max-length=1000",
            "assistant.history.max-turns=3",
            "assistant.handoff.queue=bireysel"
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

    @Test
    void givenDefaults_whenBound_thenRewriteOnTwentySecondsThreeTurns() {
        runner().run(context -> {
            AssistantProperties properties = context.getBean(AssistantProperties.class);

            assertThat(properties.queryRewrite().enabled()).isTrue();
            assertThat(properties.queryRewrite().timeout()).isEqualTo(java.time.Duration.ofSeconds(20));
            assertThat(properties.queryRewrite().maxLength()).isEqualTo(1000);
            assertThat(properties.history().maxTurns()).isEqualTo(3);
        });
    }

    @Test
    void givenEnabledFalse_whenBound_thenDisabled() {
        runner("assistant.query-rewrite.enabled=false").run(context ->
                assertThat(context.getBean(AssistantProperties.class).queryRewrite().enabled()).isFalse());
    }

    @Test
    void givenMaxTurnsOutOfRange_whenBound_thenContextFails() {
        runner("assistant.history.max-turns=0").run(context -> assertThat(context).hasFailed());
        runner("assistant.history.max-turns=11").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenMaxLengthOutOfRange_whenBound_thenContextFails() {
        runner("assistant.query-rewrite.max-length=0").run(context -> assertThat(context).hasFailed());
        runner("assistant.query-rewrite.max-length=1001").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenRewriteSectionMissing_whenBound_thenContextFails() {
        new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withPropertyValues(java.util.Arrays.stream(BASE)
                        .filter(entry -> !entry.startsWith("assistant.query-rewrite.")).toArray(String[]::new))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenApplicationPropertiesDefaults_whenBound_thenRagModeAnd15SecondHealthCheck() {
        // Arrange
        ApplicationContextRunner shipped = new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withInitializer(new ConfigDataApplicationContextInitializer());

        // Act + Assert
        shipped.run(context -> {
            AssistantProperties properties = context.getBean(AssistantProperties.class);
            assertThat(properties.generation().mode()).isEqualTo("rag");
            assertThat(properties.generation().configuredMode()).isEqualTo(GenerationMode.RAG);
            assertThat(properties.reranker().healthCheckInterval()).isEqualTo(Duration.ofSeconds(15));
            assertThat(properties.reranker().healthCheckEnabled()).isTrue();
        });
    }

    @Test
    void givenModeLlm_whenBound_thenLlmOnly() {
        runner("assistant.generation.mode=llm").run(context ->
                assertThat(context.getBean(AssistantProperties.class).generation().configuredMode())
                        .isEqualTo(GenerationMode.LLM_ONLY));
    }

    @Test
    void givenModeFoo_whenBound_thenContextFails() {
        runner("assistant.generation.mode=foo").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenShippedProperties_whenBound_thenHandoffQueueIsBireysel() {
        ApplicationContextRunner shipped = new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withInitializer(new ConfigDataApplicationContextInitializer());

        shipped.run(context -> assertThat(context.getBean(AssistantProperties.class).handoff().queue())
                .isEqualTo("bireysel"));
    }

    @Test
    void givenQueueOverride_whenBound_thenThatQueue() {
        runner("assistant.handoff.queue=kurumsal").run(context ->
                assertThat(context.getBean(AssistantProperties.class).handoff().queue()).isEqualTo("kurumsal"));
    }

    @Test
    void givenBlankHandoffQueue_whenBound_thenContextFails() {
        runner("assistant.handoff.queue= ").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenHandoffQueueLongerThan40_whenBound_thenContextFails() {
        runner("assistant.handoff.queue=" + "a".repeat(41)).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenEmptyMinRerankScore_whenBound_thenNullAndThresholdDisabled() {
        runner("assistant.retrieval.min-rerank-score=").run(context ->
                assertThat(context.getBean(AssistantProperties.class).retrieval().minRerankScore()).isNull());
    }

    @Test
    void givenNegativeMinRerankScore_whenBound_thenKeptAsNumber() {
        runner("assistant.retrieval.min-rerank-score=-4.0").run(context ->
                assertThat(context.getBean(AssistantProperties.class).retrieval().minRerankScore())
                        .isEqualTo(-4.0));
    }

    @Test
    void givenShippedProperties_whenBound_thenThresholdMinusFourAndSmallTalkAnswersFirstPerson() {
        ApplicationContextRunner shipped = new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withInitializer(new ConfigDataApplicationContextInitializer());

        shipped.run(context -> {
            AssistantProperties properties = context.getBean(AssistantProperties.class);
            assertThat(properties.retrieval().minRerankScore()).isEqualTo(-4.0);
            assertThat(properties.smallTalk().labelTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.smallTalk().greetingAnswer()).startsWith("Merhaba, ben Raşit Bankası'nın dijital asistanıyım.");
            assertThat(properties.smallTalk().thanksAnswer()).startsWith("Rica ederim.");
            assertThat(properties.smallTalk().farewellAnswer()).startsWith("Görüşmek üzere.");
            assertThat(properties.generation().targetedOpeners()).containsExactly(
                    "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum.",
                    "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum.",
                    "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum.");
        });
    }

    @Test
    void givenSmallTalkLabels_whenAnswerFor_thenEachLabelItsOwnAnswerAndOtherRejected() {
        AssistantProperties.SmallTalk smallTalk = new AssistantProperties.SmallTalk("g", "t", "f", Duration.ofSeconds(1));

        assertThat(smallTalk.answerFor(MessageLabel.GREETING)).isEqualTo("g");
        assertThat(smallTalk.answerFor(MessageLabel.THANKS)).isEqualTo("t");
        assertThat(smallTalk.answerFor(MessageLabel.FAREWELL)).isEqualTo("f");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> smallTalk.answerFor(MessageLabel.OTHER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenBlankSmallTalkAnswer_whenBound_thenContextFails() {
        runner("assistant.small-talk.thanks-answer= ").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void givenBlankOpener_whenBound_thenContextFails() {
        runner("assistant.generation.other-banks-opener= ").run(context -> assertThat(context).hasFailed());
    }
}
