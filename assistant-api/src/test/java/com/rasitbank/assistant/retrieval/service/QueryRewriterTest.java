package com.rasitbank.assistant.retrieval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.generation.HistoryTurn;
import com.rasitbank.assistant.support.ChatResponses;
import com.rasitbank.assistant.support.TestProperties;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;

/**
 * Takip sorusunu yeniden yazan sınıfın kapalı/ilk tur atlamasını, lab şablonunu, çıktı temizliğini ve hata
 * durumunda özgün soruya dönüşünü sahte dil modeliyle doğrular.
 */
class QueryRewriterTest {

    private static final String QUESTION = "Yenisi kaç günde gelir?";
    private static final String REWRITTEN = "Kaybolan kartın yenisi kaç günde gelir?";
    private static final List<HistoryTurn> HISTORY = List.of(
            new HistoryTurn("Kartımı kaybettim, ne yapmalıyım?", "Kartını hemen kapat."));

    private final ChatModel chatModel = mock(ChatModel.class);

    private QueryRewriter rewriter(AssistantProperties properties) {
        return new QueryRewriter(chatModel, new ClassPathResource("prompts/query-rewrite.st"), properties);
    }

    private QueryRewriter rewriter() {
        return rewriter(TestProperties.defaults());
    }

    private void modelReturns(String text) {
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text(text)));
    }

    @Test
    void givenRewriteDisabled_whenRewriteQuery_thenDisabledWithoutModelCall() {
        QueryRewriter disabled = rewriter(TestProperties.withQueryRewrite(false, Duration.ofSeconds(20), 1000));

        QueryRewriteResult result = disabled.rewriteQuery(QUESTION, HISTORY);

        assertThat(result).isEqualTo(new QueryRewriteResult(QueryRewriteStatus.DISABLED, QUESTION, null));
        verifyNoInteractions(chatModel);
    }

    @Test
    void givenFirstTurn_whenRewriteQuery_thenNoHistoryAndSearchesWithQuestion() {
        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, List.of());

        assertThat(result).isEqualTo(new QueryRewriteResult(QueryRewriteStatus.NO_HISTORY, QUESTION, null));
        verifyNoInteractions(chatModel);
    }

    private String capturedPrompt() {
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        verify(chatModel).stream(prompt.capture());
        assertThat(prompt.getValue().getInstructions()).hasSize(1);
        assertThat(prompt.getValue().getInstructions().get(0)).isInstanceOf(UserMessage.class);
        return prompt.getValue().getInstructions().get(0).getText().stripTrailing();
    }

    @Test
    void givenOneHistoryTurn_whenRewriteQuery_thenSendsTurkishTemplateAsSingleUserMessage() {
        modelReturns(REWRITTEN);

        rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(capturedPrompt()).isEqualTo(
                "Bir bankanın dijital asistanına müşterinin yazdığı son mesajı, rehberde arama yapmak için tek başına "
                        + "anlaşılır bir soruya çevir.\n"
                        + "Son mesaj müşterinin önceki sorularından birine dayanıyorsa (ör. \"Peki ücretli mi\", "
                        + "\"Ya kredi kartında\") eksik konuyu önceki sorulardan tamamla.\n"
                        + "Son mesaj önceki sorulardan bağımsız yeni bir konuysa son mesajı hiç değiştirmeden aynen yaz.\n"
                        + "Müşterinin niyetini koru; yeni bilgi, selamlama ya da açıklama ekleme. Sadece soruyu yaz.\n"
                        + "\n"
                        + "Müşterinin önceki soruları:\n"
                        + "- Kartımı kaybettim, ne yapmalıyım?\n"
                        + "\n"
                        + "Son mesaj:\n"
                        + "Yenisi kaç günde gelir?\n"
                        + "\n"
                        + "Arama sorusu:");
    }

    @Test
    void givenTwoHistoryTurns_whenRewriteQuery_thenQuestionLinesOldestFirst() {
        modelReturns(REWRITTEN);
        List<HistoryTurn> history = List.of(new HistoryTurn("q1", "a1"), new HistoryTurn("q2", "a2"));

        rewriter().rewriteQuery(QUESTION, history);

        assertThat(capturedPrompt()).contains("- q1\n- q2\n").doesNotContain("a1", "a2");
    }

    @Test
    void givenHistoryWithAssistantGreeting_whenRewriteQuery_thenPromptHasOnlyPreviousQuestions() {
        modelReturns(REWRITTEN);
        List<HistoryTurn> history = List.of(new HistoryTurn("Ek kart nasıl alabilirim",
                "Merhaba Ahmet, ben Raşit Bankası'nın dijital asistanıyım. Ek kartı Raşit Mobil'den alabilirsin [1]."));

        rewriter().rewriteQuery("Peki ücretli mi", history);

        assertThat(capturedPrompt()).contains("- Ek kart nasıl alabilirim", "Peki ücretli mi")
                .doesNotContain("Merhaba Ahmet", "asistanıyım", "[1]", "ASSISTANT");
    }

    @Test
    void givenFollowUp_whenRewriteQuery_thenTurkishInstructionKeepsIndependentQuestion() {
        modelReturns(REWRITTEN);

        rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(capturedPrompt()).contains("bağımsız yeni bir konuysa son mesajı hiç değiştirmeden aynen yaz",
                "Arama sorusu:").doesNotContain("Standalone query");
    }

    @Test
    void givenCleanOutput_whenRewriteQuery_thenRewrittenWithLatency() {
        modelReturns(REWRITTEN);

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.status()).isEqualTo(QueryRewriteStatus.REWRITTEN);
        assertThat(result.searchQuery()).isEqualTo(REWRITTEN);
        assertThat(result.latencyMs()).isNotNull().isBetween(0L, 5_000L);
    }

    @Test
    void givenModelFails_whenRewriteQuery_thenFailedAndOriginalQuestion() {
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.error(new IllegalStateException("refused")));

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.status()).isEqualTo(QueryRewriteStatus.FAILED);
        assertThat(result.searchQuery()).isEqualTo(QUESTION);
        assertThat(result.latencyMs()).isNotNull().isGreaterThanOrEqualTo(0L);
    }

    @Test
    void givenModelThrowsSynchronously_whenRewriteQuery_thenFailedAndOriginalQuestion() {
        given(chatModel.stream(any(Prompt.class))).willThrow(new IllegalStateException("boom"));

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.status()).isEqualTo(QueryRewriteStatus.FAILED);
        assertThat(result.searchQuery()).isEqualTo(QUESTION);
    }

    @Test
    void givenModelSilentLongerThanTimeout_whenRewriteQuery_thenFailedWithinTimeout() {
        AtomicBoolean cancelled = new AtomicBoolean();
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.<org.springframework.ai.chat.model.ChatResponse>never()
                .doOnCancel(() -> cancelled.set(true)));
        QueryRewriter shortTimeout = rewriter(TestProperties.withQueryRewrite(true, Duration.ofMillis(100), 1000));
        long start = System.nanoTime();

        QueryRewriteResult result = shortTimeout.rewriteQuery(QUESTION, HISTORY);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        assertThat(result.status()).isEqualTo(QueryRewriteStatus.FAILED);
        assertThat(result.searchQuery()).isEqualTo(QUESTION);
        assertThat(result.latencyMs()).isNotNull().isGreaterThanOrEqualTo(100L);
        assertThat(cancelled).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  \n", "Standalone query:", "standalone query:  \n", "Arama sorusu:", "ARAMA SORUSU:  \n"})
    void givenBlankOrLabelOnlyOutput_whenRewriteQuery_thenEmpty(String output) {
        modelReturns(output);

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.status()).isEqualTo(QueryRewriteStatus.EMPTY);
        assertThat(result.searchQuery()).isEqualTo(QUESTION);
        assertThat(result.latencyMs()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Standalone query: \"Kaybolan kartın yenisi kaç günde gelir?\"\n",
            "“Kaybolan kartın\nyenisi kaç günde gelir?”",
            "  Kaybolan   kartın yenisi\tkaç günde gelir?  ",
            "STANDALONE QUERY: Kaybolan kartın yenisi kaç günde gelir?",
            "Arama sorusu: Kaybolan kartın yenisi kaç günde gelir?",
            "ARAMA SORUSU: Kaybolan kartın yenisi kaç günde gelir?"})
    void givenDecoratedOutput_whenRewriteQuery_thenCleanQuery(String output) {
        modelReturns(output);

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.status()).isEqualTo(QueryRewriteStatus.REWRITTEN);
        assertThat(result.searchQuery()).isEqualTo("Kaybolan kartın yenisi kaç günde gelir?");
    }

    @Test
    void givenQuoteInsideQuery_whenRewriteQuery_thenQuotesKept() {
        modelReturns("\"Kart\" ücreti \"Gold\" için ne kadar?");

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.searchQuery()).isEqualTo("\"Kart\" ücreti \"Gold\" için ne kadar?");
    }

    @Test
    void givenOnlyEmptyQuotesOutput_whenRewriteQuery_thenEmpty() {
        modelReturns("\"\"");

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.status()).isEqualTo(QueryRewriteStatus.EMPTY);
        assertThat(result.searchQuery()).isEqualTo(QUESTION);
    }

    @Test
    void givenSingleQuoteCharacterOutput_whenRewriteQuery_thenKeptAsIs() {
        modelReturns("\"");

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.status()).isEqualTo(QueryRewriteStatus.REWRITTEN);
        assertThat(result.searchQuery()).isEqualTo("\"");
    }

    @Test
    void givenOutputLongerThanMaxLength_whenRewriteQuery_thenFailed() {
        modelReturns("a".repeat(1001));

        QueryRewriteResult tooLong = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(tooLong.status()).isEqualTo(QueryRewriteStatus.FAILED);
        assertThat(tooLong.searchQuery()).isEqualTo(QUESTION);
    }

    @Test
    void givenOutputOfExactlyMaxLength_whenRewriteQuery_thenRewritten() {
        modelReturns("a".repeat(1000));

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.status()).isEqualTo(QueryRewriteStatus.REWRITTEN);
        assertThat(result.searchQuery()).hasSize(1000);
    }

    @Test
    void givenChunkedOutput_whenRewriteQuery_thenChunksJoinedInOrder() {
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("Kaybolan kartın "),
                ChatResponses.text("yenisi kaç "), ChatResponses.text("günde gelir?")));

        QueryRewriteResult result = rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(result.searchQuery()).isEqualTo(REWRITTEN);
    }

    @Test
    void givenRewrite_whenModelCalled_thenNoTransactionActive() {
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        given(chatModel.stream(any(Prompt.class))).willAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return Flux.just(ChatResponses.text(REWRITTEN));
        });

        rewriter().rewriteQuery(QUESTION, HISTORY);

        assertThat(transactionActive).isFalse();
    }
}
