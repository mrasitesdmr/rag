package com.rasitbank.assistant.conversation.service;

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
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.generation.PromptBuilder;
import com.rasitbank.assistant.retrieval.service.ContextRetriever;
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

    private final ConversationService conversationService = mock(ConversationService.class);
    private final ActiveTurnRegistry registry = new ActiveTurnRegistry();
    private final ContextRetriever contextRetriever = mock(ContextRetriever.class);
    private final AnswerStreamer answerStreamer = mock(AnswerStreamer.class);
    private final TurnService turnService = mock(TurnService.class);
    private final PromptBuilder promptBuilder = new PromptBuilder(new ClassPathResource("prompts/rag-system.st"),
            new ClassPathResource("prompts/rag-user.st"), TestProperties.defaults());
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);

    private final AnswerService service = new AnswerService(conversationService, registry, contextRetriever,
            promptBuilder, answerStreamer, turnService, TestProperties.defaults(), "qwen3.5:9b", clock);

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
                "Bu konuda rehberimizde bilgi bulamadım."), org.mockito.ArgumentMatchers.same(emitter));
        verify(answerStreamer, never()).streamModelAnswer(any(), any(), any());
        assertThat(draft.getValue().conversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(draft.getValue().chatModel()).isEqualTo("qwen3.5:9b");
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
        assertThat(registry.tryStart(CONVERSATION_ID)).as("next request must not get 409").isTrue();
    }

    @Test
    void givenRetrievalUnavailableAndRecordFails_whenSendMessage_thenStillThrowsRetrievalUnavailable() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));
        given(turnService.recordTurn(any(), any())).willThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Soru")))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(registry.tryStart(CONVERSATION_ID)).isTrue();
    }

    @Test
    void givenTurnInProgress_whenSendMessage_thenThrowsTurnInProgressAndDoesNotSearch() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        registry.tryStart(CONVERSATION_ID);

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Soru")))
                .isInstanceOfSatisfying(ConflictException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("TURN_IN_PROGRESS"));

        verifyNoInteractions(contextRetriever, answerStreamer);
        verify(turnService, never()).recordTurn(any(), any());
        assertThat(registry.tryStart(CONVERSATION_ID)).as("first turn stays active").isFalse();
    }

    @Test
    void givenUnknownConversation_whenSendMessage_thenThrowsConversationNotFound() {
        given(conversationService.getConversation(anyLong()))
                .willThrow(new NotFoundException("CONVERSATION_NOT_FOUND", "missing"));

        assertThatThrownBy(() -> service.sendMessage(99L, request("Soru")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("CONVERSATION_NOT_FOUND"));

        verifyNoInteractions(contextRetriever, answerStreamer, turnService);
        assertThat(registry.tryStart(99L)).isTrue();
    }

    @Test
    void givenRerankedContext_whenSendMessage_thenModelReceivesChunksInRerankOrder() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext("Kartımı kaybettim"))
                .willReturn(ContextFixtures.rerankContext(7, 2, 15, 1, 3));

        service.sendMessage(CONVERSATION_ID, request("  Kartımı kaybettim  "));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        ArgumentCaptor<TurnDraft> draft = ArgumentCaptor.captor();
        verify(answerStreamer).streamModelAnswer(draft.capture(), prompt.capture(), any(SseEmitter.class));
        Message user = prompt.getValue().getInstructions().get(1);
        assertThat(user.getText()).startsWith("<parcalar>\n[1] H107\nC107\n\n[2] H102\nC102\n\n[3] H115\nC115\n\n"
                + "[4] H101\nC101\n\n[5] H103\nC103\n</parcalar>");
        assertThat(user.getText()).endsWith("Soru: Kartımı kaybettim");
        assertThat(draft.getValue().question()).isEqualTo("Kartımı kaybettim");
        assertThat(draft.getValue().searchQuery()).isEqualTo("Kartımı kaybettim");
        verify(contextRetriever).retrieveContext("Kartımı kaybettim");
        verify(answerStreamer, never()).streamFixedAnswer(any(), any(), any());
    }

    @Test
    void givenStreamStarted_whenSendMessage_thenConversationStaysActiveUntilStreamerFinishes() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.topKContext());

        service.sendMessage(CONVERSATION_ID, request("Soru"));

        assertThat(registry.tryStart(CONVERSATION_ID)).isFalse();
    }

    @Test
    void givenStreamerThrowsBeforeStreaming_whenSendMessage_thenConversationReleased() {
        given(conversationService.getConversation(CONVERSATION_ID)).willReturn(mock(Conversation.class));
        given(contextRetriever.retrieveContext(anyString())).willReturn(ContextFixtures.topKContext());
        doThrow(new IllegalStateException("boom")).when(answerStreamer).streamModelAnswer(any(), any(), any());

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, request("Soru")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(registry.tryStart(CONVERSATION_ID)).isTrue();
    }
}
