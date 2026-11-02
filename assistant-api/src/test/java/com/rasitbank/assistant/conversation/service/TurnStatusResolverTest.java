package com.rasitbank.assistant.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.domain.entity.TurnStatus;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tur durumunun onaylanan kural sırasıyla (hata, Sadece LLM, bulamadım, Hayır, alıntı) hesaplandığını doğrular.
 */
class TurnStatusResolverTest {

    private final TurnStatusResolver resolver = new TurnStatusResolver();

    static Stream<Arguments> rows() {
        return Stream.of(
                Arguments.of("CHAT_MODEL_UNAVAILABLE", GenerationMode.RAG, false, null, List.of(), TurnStatus.INCOMPLETE),
                Arguments.of("STREAM_CANCELLED", GenerationMode.RAG, true, null, List.of(), TurnStatus.INCOMPLETE),
                Arguments.of("RETRIEVAL_UNAVAILABLE", GenerationMode.RAG, false, null, List.of(), TurnStatus.INCOMPLETE),
                Arguments.of("INTERNAL_ERROR", GenerationMode.LLM_ONLY, false, null, List.of(), TurnStatus.INCOMPLETE),
                Arguments.of(null, GenerationMode.LLM_ONLY, false, false, List.of(), TurnStatus.UNGROUNDED),
                Arguments.of(null, GenerationMode.LLM_ONLY, false, null, List.of(1), TurnStatus.UNGROUNDED),
                Arguments.of(null, GenerationMode.RAG, true, false, List.of(), TurnStatus.DONT_KNOW),
                Arguments.of(null, GenerationMode.RAG, true, null, List.of(), TurnStatus.DONT_KNOW),
                Arguments.of(null, GenerationMode.RAG, true, true, List.of(1), TurnStatus.DONT_KNOW),
                Arguments.of(null, GenerationMode.RAG, false, false, List.of(1), TurnStatus.WRONG),
                Arguments.of(null, GenerationMode.RAG, false, false, List.of(), TurnStatus.WRONG),
                Arguments.of(null, GenerationMode.RAG, false, null, List.of(1, 3), TurnStatus.GROUNDED),
                Arguments.of(null, GenerationMode.RAG, false, true, List.of(1), TurnStatus.GROUNDED),
                Arguments.of(null, GenerationMode.RAG, false, null, List.of(), TurnStatus.UNGROUNDED),
                Arguments.of(null, GenerationMode.RAG, false, true, List.of(), TurnStatus.UNGROUNDED));
    }

    @ParameterizedTest
    @MethodSource("rows")
    void givenTurnFields_whenResolve_thenStatusFollowsRuleOrder(String errorCode, GenerationMode mode,
            boolean notFoundAnswer, Boolean helpful, List<Integer> cited, TurnStatus expected) {
        // Arrange
        Turn turn = mock(Turn.class);
        given(turn.getErrorCode()).willReturn(errorCode);
        given(turn.getGenerationMode()).willReturn(mode);
        given(turn.isNotFoundAnswer()).willReturn(notFoundAnswer);
        given(turn.getHelpful()).willReturn(helpful);
        given(turn.getCited()).willReturn(cited);

        // Act
        TurnStatus status = resolver.resolve(turn);

        // Assert
        assertThat(status).isEqualTo(expected);
    }
}
