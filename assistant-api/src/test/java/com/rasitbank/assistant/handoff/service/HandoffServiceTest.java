package com.rasitbank.assistant.handoff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.repository.ConversationRepository;
import com.rasitbank.assistant.conversation.repository.TurnRepository;
import com.rasitbank.assistant.handoff.domain.dto.HandoffResponse;
import com.rasitbank.assistant.handoff.domain.dto.RequestHandoffRequest;
import com.rasitbank.assistant.handoff.domain.entity.Handoff;
import com.rasitbank.assistant.handoff.domain.entity.HandoffReason;
import com.rasitbank.assistant.handoff.repository.HandoffRepository;
import com.rasitbank.assistant.support.TestProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Aktarma servisinin ayarlı kuyruk adını ve saati kayda yazdığını doğrular.
 */
class HandoffServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:31:05Z");

    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final TurnRepository turnRepository = mock(TurnRepository.class);
    private final HandoffRepository handoffRepository = mock(HandoffRepository.class);

    @Test
    void givenConfiguredQueue_whenRequestHandoff_thenInsertsThatQueue() {
        HandoffService service = new HandoffService(conversationRepository, turnRepository, handoffRepository,
                TestProperties.withHandoffQueue("kurumsal"), Clock.fixed(NOW, ZoneOffset.UTC));
        given(conversationRepository.existsById(1L)).willReturn(true);
        Turn turn = mock(Turn.class);
        given(turn.getErrorCode()).willReturn(null);
        given(turn.isNotFoundAnswer()).willReturn(true);
        given(turnRepository.findInConversation(1L, 12L)).willReturn(Optional.of(turn));
        given(handoffRepository.insertIfAbsent(anyLong(), anyLong(), eq("NO_ANSWER"), eq("kurumsal"), eq(NOW)))
                .willReturn(1);
        Handoff stored = mock(Handoff.class);
        given(stored.getConversationId()).willReturn(1L);
        given(stored.getTurnId()).willReturn(12L);
        given(stored.getReason()).willReturn(HandoffReason.NO_ANSWER);
        given(stored.getQueue()).willReturn("kurumsal");
        given(stored.getCreatedAt()).willReturn(NOW);
        given(handoffRepository.findByConversationId(1L)).willReturn(Optional.of(stored));

        HandoffResponse response = service.requestHandoff(1L, new RequestHandoffRequest(12L, HandoffReason.NO_ANSWER));

        verify(handoffRepository).insertIfAbsent(1L, 12L, "NO_ANSWER", "kurumsal", NOW);
        assertThat(response.queue()).isEqualTo("kurumsal");
        assertThat(response.createdAt()).isEqualTo(NOW);
    }
}
