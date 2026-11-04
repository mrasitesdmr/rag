package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.evaluation.service.GoldenAnswerLocator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.conversation.domain.dto.SendMessageRequest;
import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.conversation.domain.entity.AnswerSource;
import com.rasitbank.assistant.generation.MessageLabel;
import com.rasitbank.assistant.generation.MessageLabelResult;
import com.rasitbank.assistant.generation.MessageLabeler;
import com.rasitbank.assistant.generation.PromptBuilder;
import com.rasitbank.assistant.generation.ShortMessageDetector;
import com.rasitbank.assistant.retrieval.service.RerankScoreGate;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.generation.HistoryTurn;
import com.rasitbank.assistant.retrieval.service.ContextRetriever;
import com.rasitbank.assistant.retrieval.service.QueryRewriteResult;
import com.rasitbank.assistant.retrieval.service.QueryRewriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import com.rasitbank.assistant.support.ContextFixtures;
import com.rasitbank.assistant.support.TestProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Akış açılmadan önceki kontrollerin (konuşma, süren tur, retrieval) sırasını ve sonuçlarını, modele giden
 * prompt'un sıralamasını doğrular.
 */
class AnswerServiceTest {

    private static final long CONVERSATION_ID = 7L;
    private static final String LONG_GREETING = "merhaba ben bugün çok keyifli hissediyorum sizin nasıl geçiyor günleriniz acaba";

    private final ConversationService conversationService = mock(ConversationService.class);
    private final ActiveTurnRegistry registry = new ActiveTurnRegistry();
    private final QueryRewriter queryRewriter = mock(QueryRewriter.class);
    private final ContextRetriever contextRetriever = mock(ContextRetriever.class);
    private final AnswerStreamer answerStreamer = mock(AnswerStreamer.class);
    private final TurnService turnService = mock(TurnService.class);
    private final GoldenAnswerLocator goldenAnswerLocator = mock(GoldenAnswerLocator.class);
    private final MessageLabeler messageLabeler = mock(MessageLabeler.class);
    private final ShortMessageDetector shortMessageDetector = new ShortMessageDetector();
    private final PromptBuilder promptBuilder = new PromptBuilder(new ClassPathResource("prompts/rag-system.st"),
            new ClassPathResource("prompts/rag-user.st"), new ClassPathResource("prompts/llm-only-system.st"), TestProperties.defaults());
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);

    private final AnswerService service = new AnswerService(conversationService, registry, queryRewriter, contextRetriever,
            promptBuilder, answerStreamer, turnService, goldenAnswerLocator,
            shortMessageDetector, new RerankScoreGate(TestProperties.defaults()), messageLabeler,
            TestProperties.defaults(), "qwen3.5:9b", clock);

    @BeforeEach
    void stubFirstTurnRewriteAndOtherLabel() {
        given(messageLabeler.labelMessage(anyString()))
                .willReturn(new MessageLabelResult(MessageLabel.OTHER, 300L, false));
        given(queryRewriter.rewriteQuery(anyString(), any())).willAnswer(invocation -> new QueryRewriteResult(
                QueryRewriteStatus.NO_HISTORY, invocation.getArgument(0), null));
    }

    private static SendMessageRequest request(String text) {
        return new SendMessageRequest(text);
    }

    @Test
    void givenNoSearchableChunks_whenSendMessage_thenFixedNotFoundAnswerWithoutModelCall() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext("Kartımı kaybettim")).willReturn(
                ContextFixtures.emptyContext(RetrievalStrategy.RERANK));

        SseEmitter emitter = service.sendMessage(CONVERSATION_ID, request("Kartımı kaybettim"));

        assertThat(emitter).isNotNull();
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamFixedAnswer(draft.capture(), org.mockito.ArgumentMatchers.eq(
                "Bu konuda rehberimizde bilgi bulamadım."), org.mockito.ArgumentMatchers.eq(
                AnswerSource.EMPTY_KNOWLEDGE_BASE), org.mockito.ArgumentMatchers.same(emitter));
        verify(messageLabeler, times(1)).labelMessage("Kartımı kaybettim");
        verify(answerStreamer, never()).streamModelAnswer(any(), any(), any(), any());
        assertThat(draft.getValue().conversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(draft.getValue().chatModel()).isEqualTo("qwen3.5:9b");
    }

    @Test
    void givenQuestionWithSpaces_whenSendMessage_thenRegistersStrippedQuestionAndReceivedAt() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.emptyContext(RetrievalStrategy.RERANK));

        // Act
        service.sendMessage(CONVERSATION_ID, request("  Kartımı kaybettim, ne yapmalıyım?  "));

        // Assert
        assertThat(registry.findActiveTurn(CONVERSATION_ID)).contains(
                new ActiveTurn("Kartımı kaybettim, ne yapmalıyım?", Instant.parse("2026-11-01T10:00:00Z")));
    }

    @Test
    void givenRetrievalUnavailable_whenSendMessage_thenThrows503AndRecordsTurnWithErrorCode() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Soru")))
                .isInstanceOfSatisfying(ServiceUnavailableException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("RETRIEVAL_UNAVAILABLE"));

        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        ArgumentCaptor<TurnOutcome> outcome = ArgumentCaptor.captor();
        verify(turnService, times(1)).recordTurn(draft.capture(), outcome.capture());
        assertThat(outcome.getValue().errorCode()).isEqualTo("RETRIEVAL_UNAVAILABLE");
        assertThat(outcome.getValue().answer()).isNull();
        assertThat(draft.getValue().question()).isEqualTo("Soru");
        assertThat(draft.getValue().searchQuery()).isEqualTo("Soru");
        assertThat(draft.getValue().context()).isNull();
        verifyNoInteractions(answerStreamer);
        assertThat(outcome.getValue().latencyMs()).isBetween(0L, 5_000L);
        assertThat(registry.tryStart(CONVERSATION_ID, "q", Instant.EPOCH)).as("next request must not get 409").isTrue();
    }

    @Test
    void givenRetrievalUnavailableAndRecordFails_whenSendMessage_thenStillThrowsRetrievalUnavailable() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));
        given(turnService.recordTurn(any(), any())).willThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Soru")))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(registry.tryStart(CONVERSATION_ID, "q", Instant.EPOCH)).isTrue();
    }

    @Test
    void givenTurnInProgress_whenSendMessage_thenThrowsTurnInProgressAndDoesNotSearch() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        registry.tryStart(CONVERSATION_ID, "q", Instant.EPOCH);

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Soru")))
                .isInstanceOfSatisfying(ConflictException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("TURN_IN_PROGRESS"));

        verifyNoInteractions(contextRetriever, answerStreamer);
        verify(turnService, never()).recordTurn(any(), any());
        assertThat(registry.tryStart(CONVERSATION_ID, "q", Instant.EPOCH)).as("first turn stays active").isFalse();
    }

    @Test
    void givenUnknownConversation_whenSendMessage_thenThrowsConversationNotFound() {
        given(conversationService.getConversation(anyLong()))
                .willThrow(new NotFoundException("CONVERSATION_NOT_FOUND", "missing"));

        assertThatThrownBy(() -> service.sendMessage(99L, request("Soru")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("CONVERSATION_NOT_FOUND"));

        verifyNoInteractions(contextRetriever, answerStreamer, turnService);
        assertThat(registry.tryStart(99L, "q", Instant.EPOCH)).isTrue();
    }

    @Test
    void givenRerankedContext_whenSendMessage_thenModelReceivesChunksInRerankOrder() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext("Kartımı kaybettim"))
                .willReturn(ContextFixtures.rerankContext(7, 2, 15, 1, 3));

        service.sendMessage(CONVERSATION_ID, request("  Kartımı kaybettim  "));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamModelAnswer(draft.capture(), prompt.capture(), any(SseEmitter.class), any());
        Message user = prompt.getValue().getInstructions().get(1);
        assertThat(user.getText()).startsWith("<parcalar>\n[1] H107\nC107\n\n[2] H102\nC102\n\n[3] H115\nC115\n\n"
                + "[4] H101\nC101\n\n[5] H103\nC103\n</parcalar>");
        assertThat(user.getText()).endsWith("Soru: Kartımı kaybettim");
        assertThat(draft.getValue().question()).isEqualTo("Kartımı kaybettim");
        assertThat(draft.getValue().searchQuery()).isEqualTo("Kartımı kaybettim");
        verify(contextRetriever).retrieveContext("Kartımı kaybettim");
        verify(answerStreamer, never()).streamFixedAnswer(any(), any(), any(), any());
    }

    @Test
    void givenStreamStarted_whenSendMessage_thenConversationStaysActiveUntilStreamerFinishes() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.topKContext());

        service.sendMessage(CONVERSATION_ID, request("Soru"));

        assertThat(registry.tryStart(CONVERSATION_ID, "q", Instant.EPOCH)).isFalse();
    }

    @Test
    void givenStreamerThrowsBeforeStreaming_whenSendMessage_thenConversationReleased() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.topKContext());
        doThrow(new IllegalStateException("boom")).when(answerStreamer).streamModelAnswer(any(), any(), any(), any());

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Soru")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(registry.tryStart(CONVERSATION_ID, "q", Instant.EPOCH)).isTrue();
    }

    @Test
    void givenFollowUp_whenSendMessage_thenSearchesWithStandaloneAndPromptsCustomerQuestion() {
        List<HistoryTurn> history = List.of(new HistoryTurn("Kartımı kaybettim, ne yapmalıyım?", "Kartını hemen kapat."));
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(conversationService.getHistory(CONVERSATION_ID)).willReturn(history);
        given(queryRewriter.rewriteQuery("Yenisi kaç günde gelir?", history)).willReturn(new QueryRewriteResult(
                QueryRewriteStatus.REWRITTEN, "Kaybolan kartın yenisi kaç günde gelir?", 812L));
        given(contextRetriever.retrieveContext("Kaybolan kartın yenisi kaç günde gelir?"))
                .willReturn(ContextFixtures.rerankContext(7, 2, 15, 1, 3));

        service.sendMessage(CONVERSATION_ID, request("Yenisi kaç günde gelir?"));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamModelAnswer(draft.capture(), prompt.capture(), any(SseEmitter.class), any());
        List<Message> messages = prompt.getValue().getInstructions();
        assertThat(messages).hasSize(4);
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo("Kartımı kaybettim, ne yapmalıyım?");
        assertThat(messages.get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(3)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(3).getText()).endsWith("Soru: Yenisi kaç günde gelir?")
                .doesNotContain("Kaybolan kartın yenisi");
        assertThat(draft.getValue().question()).isEqualTo("Yenisi kaç günde gelir?");
        assertThat(draft.getValue().searchQuery()).isEqualTo("Kaybolan kartın yenisi kaç günde gelir?");
        assertThat(draft.getValue().queryRewriteStatus()).isEqualTo(QueryRewriteStatus.REWRITTEN);
        assertThat(draft.getValue().queryRewriteLatencyMs()).isEqualTo(812L);
        assertThat(draft.getValue().historyTurns()).isEqualTo(1);
    }

    @Test
    void givenRewriteFailed_whenSendMessage_thenSearchesWithQuestionAndStreams() {
        List<HistoryTurn> history = List.of(new HistoryTurn("Kartımı kaybettim", "Kapat."));
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(conversationService.getHistory(CONVERSATION_ID)).willReturn(history);
        given(queryRewriter.rewriteQuery("Yenisi kaç günde gelir?", history)).willReturn(
                new QueryRewriteResult(QueryRewriteStatus.FAILED, "Yenisi kaç günde gelir?", 40L));
        given(contextRetriever.retrieveContext("Yenisi kaç günde gelir?")).willReturn(ContextFixtures.topKContext());

        service.sendMessage(CONVERSATION_ID, request("Yenisi kaç günde gelir?"));

        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer, times(1)).streamModelAnswer(draft.capture(), any(), any(SseEmitter.class), any());
        assertThat(draft.getValue().queryRewriteStatus()).isEqualTo(QueryRewriteStatus.FAILED);
        assertThat(draft.getValue().searchQuery()).isEqualTo("Yenisi kaç günde gelir?");
    }

    @Test
    void givenTurnInProgress_whenSendMessage_thenNoHistoryReadAndNoRewrite() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        registry.tryStart(CONVERSATION_ID, "q", Instant.EPOCH);

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Soru")))
                .isInstanceOfSatisfying(ConflictException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("TURN_IN_PROGRESS"));

        verifyNoInteractions(queryRewriter);
        verify(conversationService, never()).getHistory(anyLong());
    }

    @Test
    void givenRetrievalUnavailableAfterRewrite_whenSendMessage_thenTurnKeepsSearchQueryAndStatus() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(conversationService.getHistory(CONVERSATION_ID))
                .willReturn(List.of(new HistoryTurn("Kartımı kaybettim", "Kapat.")));
        given(queryRewriter.rewriteQuery(anyString(), any())).willReturn(new QueryRewriteResult(
                QueryRewriteStatus.REWRITTEN, "Kaybolan kartın yenisi kaç günde gelir?", 812L));
        given(contextRetriever.retrieveContext(anyString()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Yenisi kaç günde gelir?")))
                .isInstanceOf(ServiceUnavailableException.class);

        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(turnService).recordTurn(draft.capture(), any());
        assertThat(draft.getValue().searchQuery()).isEqualTo("Kaybolan kartın yenisi kaç günde gelir?");
        assertThat(draft.getValue().queryRewriteStatus()).isEqualTo(QueryRewriteStatus.REWRITTEN);
        assertThat(draft.getValue().queryRewriteLatencyMs()).isEqualTo(812L);
        assertThat(draft.getValue().historyTurns()).isEqualTo(1);
    }

    private AnswerService llmOnlyService() {
        return new AnswerService(conversationService, registry, queryRewriter, contextRetriever, promptBuilder,
                answerStreamer, turnService, goldenAnswerLocator, shortMessageDetector,
                new RerankScoreGate(TestProperties.withMode("llm", "rerank")), messageLabeler,
                TestProperties.withMode("llm", "rerank"), "qwen3.5:9b", clock);
    }

    @Test
    void givenLlmOnlyMode_whenSendMessage_thenNoRewriteNoRetrieval() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));

        // Act
        SseEmitter emitter = llmOnlyService().sendMessage(CONVERSATION_ID, request("Kartımı kaybettim, ne yapmalıyım?"));

        // Assert
        verifyNoInteractions(contextRetriever);
        verify(queryRewriter, never()).rewriteQuery(anyString(), any());
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        verify(answerStreamer, times(1)).streamModelAnswer(draft.capture(), prompt.capture(),
                org.mockito.ArgumentMatchers.same(emitter), any());
        verify(answerStreamer, never()).streamFixedAnswer(any(), any(), any(), any());
        assertThat(draft.getValue().generationMode()).isEqualTo(GenerationMode.LLM_ONLY);
        assertThat(draft.getValue().searchQuery()).isNull();
        assertThat(draft.getValue().queryRewriteStatus()).isEqualTo(QueryRewriteStatus.DISABLED);
        assertThat(draft.getValue().queryRewriteLatencyMs()).isNull();
        assertThat(draft.getValue().context()).isNull();
        assertThat(draft.getValue().question()).isEqualTo("Kartımı kaybettim, ne yapmalıyım?");
        assertThat(prompt.getValue().getInstructions()).hasSize(2);
    }

    @Test
    void givenLlmOnlyMode_whenSendMessageWithNoDocuments_thenModelStillCalledNotFixedAnswer() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));

        // Act
        llmOnlyService().sendMessage(CONVERSATION_ID, request("Soru"));

        // Assert
        verify(answerStreamer).streamModelAnswer(any(), any(), any(SseEmitter.class), any());
        verify(answerStreamer, never()).streamFixedAnswer(any(), any(), any(), any());
    }

    @Test
    void givenLlmOnlyFollowUp_whenSendMessage_thenHistoryInPromptAndNoRewrite() {
        // Arrange
        List<HistoryTurn> history = List.of(new HistoryTurn("Kartımı kaybettim, ne yapmalıyım?", "Kartını hemen kapat."));
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(conversationService.getHistory(CONVERSATION_ID)).willReturn(history);

        // Act
        llmOnlyService().sendMessage(CONVERSATION_ID, request("Yenisi kaç günde gelir?"));

        // Assert
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamModelAnswer(draft.capture(), prompt.capture(), any(SseEmitter.class), any());
        List<Message> messages = prompt.getValue().getInstructions();
        assertThat(messages).hasSize(4);
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo("Kartımı kaybettim, ne yapmalıyım?");
        assertThat(messages.get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(2).getText()).isEqualTo("Kartını hemen kapat.");
        assertThat(messages.get(3)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(3).getText()).isEqualTo("Yenisi kaç günde gelir?");
        assertThat(draft.getValue().historyTurns()).isEqualTo(1);
        verify(queryRewriter, never()).rewriteQuery(anyString(), any());
        verifyNoInteractions(contextRetriever);
    }

    @Test
    void givenLlmOnlyAndTurnInProgress_whenSendMessage_thenConflictAndModelNotCalled() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        registry.tryStart(CONVERSATION_ID, "q", Instant.EPOCH);

        // Act
        Throwable thrown = org.assertj.core.api.Assertions
                .catchThrowable(() -> llmOnlyService().sendMessage(CONVERSATION_ID, request("Soru")));

        // Assert
        assertThat(thrown).isInstanceOfSatisfying(ConflictException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("TURN_IN_PROGRESS"));
        verifyNoInteractions(answerStreamer);
    }

    @Test
    void givenRagMode_whenSendMessage_thenDraftCarriesRagMode() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.topKContext());

        // Act
        service.sendMessage(CONVERSATION_ID, request("Soru"));

        // Assert
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamModelAnswer(draft.capture(), any(), any(SseEmitter.class), any());
        assertThat(draft.getValue().generationMode()).isEqualTo(GenerationMode.RAG);
    }

    @Test
    void givenRagWithContext_whenSendMessage_thenLocatorGetsStrippedQuestionAndIdsReachStreamer() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext("Kartımı kaybettim")).willReturn(ContextFixtures.topKContext());
        given(goldenAnswerLocator.locateAnswerChunks(java.util.List.of("Kartımı kaybettim")))
                .willReturn(java.util.Map.of("kartımı kaybettim", java.util.Set.of(102L)));

        // Act
        service.sendMessage(CONVERSATION_ID, request("  Kartımı kaybettim  "));

        // Assert
        verify(answerStreamer).streamModelAnswer(any(), any(), any(SseEmitter.class),
                org.mockito.ArgumentMatchers.eq(java.util.Set.of(102L)));
    }

    @Test
    void givenEmptyContext_whenSendMessage_thenLocatorNotCalled() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.emptyContext(RetrievalStrategy.RERANK));

        // Act
        service.sendMessage(CONVERSATION_ID, request("Soru"));

        // Assert
        verifyNoInteractions(goldenAnswerLocator);
    }

    @Test
    void givenLlmOnlyMode_whenSendMessage_thenLocatorNotCalledAndNoAnswerChunks() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));

        // Act
        llmOnlyService().sendMessage(CONVERSATION_ID, request("Soru"));

        // Assert
        verifyNoInteractions(goldenAnswerLocator);
        verify(answerStreamer).streamModelAnswer(any(), any(), any(SseEmitter.class),
                org.mockito.ArgumentMatchers.eq(java.util.Set.of()));
    }

    private void givenLabel(String message, MessageLabel label) {
        given(messageLabeler.labelMessage(message)).willReturn(new MessageLabelResult(label, 450L, false));
    }

    @Test
    void givenFirstShortMessageLabeledGreeting_whenSendMessage_thenFixedReplyWithoutRewriteOrRetrieval() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        givenLabel("Selam", MessageLabel.GREETING);

        // Act
        SseEmitter emitter = service.sendMessage(CONVERSATION_ID, request("Selam"));

        // Assert
        verifyNoInteractions(contextRetriever, queryRewriter, goldenAnswerLocator);
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamFixedAnswer(draft.capture(), org.mockito.ArgumentMatchers.eq(TestProperties.GREETING_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.SMALL_TALK), org.mockito.ArgumentMatchers.same(emitter));
        verify(answerStreamer, never()).streamModelAnswer(any(), any(), any(), any());
        assertThat(draft.getValue().messageLabel()).isEqualTo(MessageLabel.GREETING);
        assertThat(draft.getValue().messageLabelLatencyMs()).isEqualTo(450L);
        assertThat(draft.getValue().searchQuery()).isNull();
        assertThat(draft.getValue().queryRewriteStatus()).isEqualTo(QueryRewriteStatus.DISABLED);
        assertThat(draft.getValue().context()).isNull();
        assertThat(draft.getValue().generationMode()).isEqualTo(GenerationMode.RAG);
    }

    @Test
    void givenHistoryAndShortThanks_whenSendMessage_thenFixedThanksWithoutRewriteOrRetrieval() {
        // Arrange
        givenConversationWithHistory();
        givenLabel("Teşekkürler", MessageLabel.THANKS);

        // Act
        service.sendMessage(CONVERSATION_ID, request("Teşekkürler"));

        // Assert
        verifyNoInteractions(contextRetriever, queryRewriter, goldenAnswerLocator);
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamFixedAnswer(draft.capture(), org.mockito.ArgumentMatchers.eq(TestProperties.THANKS_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.SMALL_TALK), any(SseEmitter.class));
        assertThat(draft.getValue().messageLabel()).isEqualTo(MessageLabel.THANKS);
    }

    @Test
    void givenShortThanksAndFarewell_whenSendMessage_thenEachGetsItsOwnFixedReply() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        givenLabel("Teşekkürler", MessageLabel.THANKS);
        givenLabel("Görüşürüz", MessageLabel.FAREWELL);

        // Act
        service.sendMessage(CONVERSATION_ID, request("Teşekkürler"));
        registry.finish(CONVERSATION_ID);
        service.sendMessage(CONVERSATION_ID, request("Görüşürüz"));

        // Assert
        ArgumentCaptor<String> answers = ArgumentCaptor.captor();
        verify(answerStreamer, times(2)).streamFixedAnswer(any(), answers.capture(), any(), any());
        assertThat(answers.getAllValues()).containsExactly(TestProperties.THANKS_ANSWER,
                TestProperties.FAREWELL_ANSWER);
    }

    @Test
    void givenShortMessageLabeledOther_whenSendMessage_thenNormalPathAndLabelAskedOnce() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        givenLabel("Kredi limitim ne kadar", MessageLabel.OTHER);
        given(contextRetriever.retrieveContext("Kredi limitim ne kadar"))
                .willReturn(ContextFixtures.rerankContextWithTopScore(1.0));

        // Act
        service.sendMessage(CONVERSATION_ID, request("Kredi limitim ne kadar"));

        // Assert
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamModelAnswer(draft.capture(), any(), any(SseEmitter.class), any());
        verify(messageLabeler, times(1)).labelMessage(anyString());
        assertThat(draft.getValue().messageLabel()).isEqualTo(MessageLabel.OTHER);
        assertThat(draft.getValue().messageLabelLatencyMs()).isEqualTo(450L);
    }

    @Test
    void givenShortMessageLabeledOtherBelowThreshold_whenSendMessage_thenNotFoundWithoutSecondLabel() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        givenLabel("jjkbjknlk", MessageLabel.OTHER);
        given(contextRetriever.retrieveContext("jjkbjknlk"))
                .willReturn(ContextFixtures.rerankContextWithTopScore(-6.0));

        // Act
        service.sendMessage(CONVERSATION_ID, request("jjkbjknlk"));

        // Assert
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamFixedAnswer(draft.capture(), org.mockito.ArgumentMatchers.eq(TestProperties.NOT_FOUND_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.BELOW_RERANK_THRESHOLD), any(SseEmitter.class));
        verify(messageLabeler, times(1)).labelMessage(anyString());
        assertThat(draft.getValue().messageLabel()).isEqualTo(MessageLabel.OTHER);
    }

    @Test
    void givenRetrievalDownAndShortThanks_whenSendMessage_thenThanksReplyNot503() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));
        givenLabel("Teşekkür ederim", MessageLabel.THANKS);

        // Act
        Throwable thrown = org.assertj.core.api.Assertions
                .catchThrowable(() -> service.sendMessage(CONVERSATION_ID, request("Teşekkür ederim")));

        // Assert
        assertThat(thrown).isNull();
        verify(answerStreamer).streamFixedAnswer(any(), org.mockito.ArgumentMatchers.eq(TestProperties.THANKS_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.SMALL_TALK), any(SseEmitter.class));
        verifyNoInteractions(contextRetriever);
        verify(turnService, never()).recordTurn(any(), any());
    }

    @Test
    void givenLongMessage_whenSendMessage_thenNoLabelBeforeSearch() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.rerankContextWithTopScore(5.0));
        String eleven = "bir iki üç dört beş altı yedi sekiz dokuz on onbir";

        // Act
        service.sendMessage(CONVERSATION_ID, request(eleven));
        registry.finish(CONVERSATION_ID);
        service.sendMessage(CONVERSATION_ID, request("Bir iki üç dört beş altı?"));

        // Assert
        verifyNoInteractions(messageLabeler);
        verify(queryRewriter, times(2)).rewriteQuery(anyString(), any());
    }

    @Test
    void givenLongMessageBelowThreshold_whenSendMessage_thenLabelAskedAfterRetrievalOnce() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.rerankContextWithTopScore(-6.0));
        String eleven = "bir iki üç dört beş altı yedi sekiz dokuz on onbir";
        given(messageLabeler.labelMessage(eleven)).willReturn(new MessageLabelResult(MessageLabel.GREETING, 500L, false));

        // Act
        service.sendMessage(CONVERSATION_ID, request(eleven));

        // Assert
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(contextRetriever, messageLabeler);
        order.verify(contextRetriever).retrieveContext(anyString());
        order.verify(messageLabeler).labelMessage(eleven);
        verify(answerStreamer).streamFixedAnswer(any(), org.mockito.ArgumentMatchers.eq(TestProperties.GREETING_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.SMALL_TALK), any(SseEmitter.class));
    }

    @Test
    void givenShortMessageAndLabelFailure_whenSendMessage_thenNormalPathWithoutError() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(messageLabeler.labelMessage("Allah razı olsun"))
                .willReturn(new MessageLabelResult(MessageLabel.OTHER, 10_000L, true));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.rerankContextWithTopScore(5.0));

        // Act
        Throwable thrown = org.assertj.core.api.Assertions
                .catchThrowable(() -> service.sendMessage(CONVERSATION_ID, request("Allah razı olsun")));

        // Assert
        assertThat(thrown).isNull();
        verify(contextRetriever).retrieveContext(anyString());
        verify(answerStreamer).streamModelAnswer(any(), any(), any(SseEmitter.class), any());
    }

    @Test
    void givenMessageWithoutWords_whenSendMessage_thenNotFoundWithoutLabelRewriteRetrievalOrModel() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));

        // Act
        SseEmitter emitter = service.sendMessage(CONVERSATION_ID, request("???"));

        // Assert
        verifyNoInteractions(messageLabeler, contextRetriever, goldenAnswerLocator);
        verify(queryRewriter, never()).rewriteQuery(anyString(), any());
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamFixedAnswer(draft.capture(), org.mockito.ArgumentMatchers.eq(TestProperties.NOT_FOUND_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.NO_WORDS), org.mockito.ArgumentMatchers.same(emitter));
        verify(answerStreamer, never()).streamModelAnswer(any(), any(), any(), any());
        assertThat(draft.getValue().searchQuery()).isNull();
        assertThat(draft.getValue().messageLabel()).isNull();
        assertThat(draft.getValue().messageLabelLatencyMs()).isNull();
        assertThat(draft.getValue().queryRewriteStatus()).isEqualTo(QueryRewriteStatus.DISABLED);
        assertThat(draft.getValue().context()).isNull();
    }

    @Test
    void givenRetrievalDownAndMessageWithoutWords_whenSendMessage_thenNotFoundNot503() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        // Act
        Throwable thrown = org.assertj.core.api.Assertions
                .catchThrowable(() -> service.sendMessage(CONVERSATION_ID, request("🙂")));

        // Assert
        assertThat(thrown).isNull();
        verify(answerStreamer).streamFixedAnswer(any(), org.mockito.ArgumentMatchers.eq(TestProperties.NOT_FOUND_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.NO_WORDS), any(SseEmitter.class));
    }

    @Test
    void givenLlmOnlyModeAndMessageWithoutWords_whenSendMessage_thenModelCalled() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));

        // Act
        llmOnlyService().sendMessage(CONVERSATION_ID, request("???"));

        // Assert
        verify(answerStreamer).streamModelAnswer(any(), any(), any(SseEmitter.class), any());
        verify(answerStreamer, never()).streamFixedAnswer(any(), any(), any(), any());
        verifyNoInteractions(messageLabeler);
    }
    @Test
    void givenBelowThresholdAndLabelOther_whenSendMessage_thenNotFoundSentenceWithRetrievalRecordedAndNoModelAnswer() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext("Bugün hava nasıl?"))
                .willReturn(ContextFixtures.rerankContextWithTopScore(-2.5));
        given(messageLabeler.labelMessage("Bugün hava nasıl?"))
                .willReturn(new MessageLabelResult(MessageLabel.OTHER, 640L, false));

        // Act
        SseEmitter emitter = service.sendMessage(CONVERSATION_ID, request("Bugün hava nasıl?"));

        // Assert
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamFixedAnswer(draft.capture(),
                org.mockito.ArgumentMatchers.eq(TestProperties.NOT_FOUND_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.BELOW_RERANK_THRESHOLD),
                org.mockito.ArgumentMatchers.same(emitter));
        verify(answerStreamer, never()).streamModelAnswer(any(), any(), any(), any());
        assertThat(draft.getValue().context().contextChunks()).isEmpty();
        assertThat(draft.getValue().context().candidateChunks()).hasSize(20);
        assertThat(draft.getValue().messageLabel()).isEqualTo(MessageLabel.OTHER);
        assertThat(draft.getValue().messageLabelLatencyMs()).isEqualTo(640L);
        verifyNoInteractions(goldenAnswerLocator);
    }

    @Test
    void givenLongMessageBelowThresholdAndLabelGreeting_whenSendMessage_thenGreetingReplyAsSmallTalk() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(LONG_GREETING))
                .willReturn(ContextFixtures.rerankContextWithTopScore(-8.4));
        given(messageLabeler.labelMessage(LONG_GREETING))
                .willReturn(new MessageLabelResult(MessageLabel.GREETING, 700L, false));

        // Act
        service.sendMessage(CONVERSATION_ID, request(LONG_GREETING));

        // Assert
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamFixedAnswer(draft.capture(),
                org.mockito.ArgumentMatchers.eq(TestProperties.GREETING_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.SMALL_TALK), any(SseEmitter.class));
        assertThat(draft.getValue().messageLabel()).isEqualTo(MessageLabel.GREETING);
        assertThat(draft.getValue().context().contextChunks()).isEmpty();
        assertThat(draft.getValue().searchQuery()).isEqualTo(LONG_GREETING);
    }

    @Test
    void givenBelowThresholdSearchUsesStandaloneQuery_whenSendMessage_thenLabelerGetsCustomersOwnMessage() {
        // Arrange
        List<HistoryTurn> history = List.of(new HistoryTurn("Gold kart aidatı ne kadar?", "750 TL [1]."));
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(conversationService.getHistory(CONVERSATION_ID)).willReturn(history);
        given(queryRewriter.rewriteQuery("Çok makbule geçti", history)).willReturn(new QueryRewriteResult(
                QueryRewriteStatus.REWRITTEN, "Gold kart aidatı ödeme", 300L));
        given(contextRetriever.retrieveContext("Gold kart aidatı ödeme"))
                .willReturn(ContextFixtures.rerankContextWithTopScore(-5.0));
        given(messageLabeler.labelMessage(anyString()))
                .willReturn(new MessageLabelResult(MessageLabel.THANKS, 500L, false));

        // Act
        service.sendMessage(CONVERSATION_ID, request("Çok makbule geçti"));

        // Assert
        verify(messageLabeler).labelMessage("Çok makbule geçti");
    }

    @Test
    void givenTopKFallbackOrAboveThreshold_whenSendMessage_thenLabelAskedOnlyBeforeSearch() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext("top"))
                .willReturn(ContextFixtures.topKContext());
        given(contextRetriever.retrieveContext("ust"))
                .willReturn(ContextFixtures.rerankContextWithTopScore(-1.9));

        // Act
        service.sendMessage(CONVERSATION_ID, request("top"));
        registry.finish(CONVERSATION_ID);
        service.sendMessage(CONVERSATION_ID, request("ust"));

        // Assert
        verify(messageLabeler, times(2)).labelMessage(anyString());
        verify(answerStreamer, times(2)).streamModelAnswer(any(), any(), any(SseEmitter.class), any());
    }

    @Test
    void givenBelowThresholdAndModelDown_whenSendMessage_thenNotFoundWithoutError() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext("asdfgh")).willReturn(ContextFixtures.rerankContextWithTopScore(-6.0));
        given(messageLabeler.labelMessage("asdfgh"))
                .willReturn(new MessageLabelResult(MessageLabel.OTHER, 10_000L, true));

        // Act
        Throwable thrown = org.assertj.core.api.Assertions
                .catchThrowable(() -> service.sendMessage(CONVERSATION_ID, request("asdfgh")));

        // Assert
        assertThat(thrown).isNull();
        verify(answerStreamer).streamFixedAnswer(any(), org.mockito.ArgumentMatchers.eq(TestProperties.NOT_FOUND_ANSWER),
                org.mockito.ArgumentMatchers.eq(AnswerSource.BELOW_RERANK_THRESHOLD), any(SseEmitter.class));
    }

    @Test
    void givenLlmOnlyModeAndGreeting_whenSendMessage_thenModelCalledAndNoSmallTalkRoute() {
        // Arrange
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));

        // Act
        llmOnlyService().sendMessage(CONVERSATION_ID, request("Selam"));

        // Assert
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamModelAnswer(draft.capture(), any(), any(SseEmitter.class), any());
        verify(answerStreamer, never()).streamFixedAnswer(any(), any(), any(), any());
        verifyNoInteractions(messageLabeler);
        assertThat(draft.getValue().messageLabel()).isNull();
    }

    private static final List<HistoryTurn> ONE_TURN = List.of(new HistoryTurn("Gold kart aidatı ne kadar?", "750 TL [1]."));

    private void givenConversationWithHistory() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(conversationService.getHistory(CONVERSATION_ID)).willReturn(ONE_TURN);
    }

    @Test
    void givenLlmOnlyModeAndShortGreeting_whenSendMessage_thenModelCalledAndLabelerNotCalled() {
        // Arrange
        givenConversationWithHistory();

        // Act
        llmOnlyService().sendMessage(CONVERSATION_ID, request("Allah razı olsun"));

        // Assert
        verifyNoInteractions(messageLabeler);
        verify(answerStreamer).streamModelAnswer(any(), any(), any(SseEmitter.class), any());
    }
}
