package com.rasitbank.assistant.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.evaluation.domain.entity.GoldenQuestion;
import com.rasitbank.assistant.evaluation.repository.GoldenQuestionRepository;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.client.ChunkMatchesResponse;
import com.rasitbank.assistant.retrieval.client.ChunkTextMatch;
import com.rasitbank.assistant.retrieval.client.MatchedChunk;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Soru metninin normalize tam eşleşmesini ve retrieval-api çağrısının yalnızca eşleşen sorular için yapıldığını doğrular.
 */
class GoldenAnswerLocatorTest {

    private static final String GOLDEN_QUESTION = "Kartımı kaybettim, ne yapmalıyım?";
    private static final String ANSWER = "Kartınızı kaybettiyseniz ya da kartınız çalındıysa vakit kaybetmeden bildirin.";

    private final GoldenQuestionRepository repository = mock(GoldenQuestionRepository.class);
    private final RetrievalClient retrievalClient = mock(RetrievalClient.class);
    private final GoldenAnswerLocator locator = new GoldenAnswerLocator(repository, retrievalClient);

    private static GoldenQuestion golden(String text, String answer) {
        return new GoldenQuestion(1, 1, "Kartlar", text, SuggestionTextNormalizer.normalize(text), 19, answer);
    }

    private static ChunkMatchesResponse matches(long... chunkIds) {
        List<MatchedChunk> chunks = java.util.Arrays.stream(chunkIds)
                .mapToObj(id -> new MatchedChunk(id, 10001, "Rehber", "2026.10", 19)).toList();
        return new ChunkMatchesResponse(List.of(new ChunkTextMatch(ANSWER, chunks)));
    }

    @Test
    void givenQuestionDifferingInCaseAndPunctuation_whenLocateAnswerChunks_thenMatchesGoldenQuestion() {
        // Arrange
        String asked = "KARTIMI kaybettim ne yapmalıyım";
        String key = SuggestionTextNormalizer.normalize(asked);
        given(repository.findByNormalizedTextIn(Set.of(key))).willReturn(List.of(golden(GOLDEN_QUESTION, ANSWER)));
        given(retrievalClient.findChunkMatches(List.of(ANSWER))).willReturn(matches(151));

        // Act
        Map<String, Set<Long>> result = locator.locateAnswerChunks(List.of(asked));

        // Assert
        assertThat(result).containsOnlyKeys(key);
        assertThat(result.get(key)).containsExactly(151L);
    }

    @Test
    void givenQuestionWithExtraWord_whenLocateAnswerChunks_thenNoMatchAndNoRetrievalCall() {
        // Arrange: repository gerçek kuralı uygular: yalnızca tam normalize metin döner
        String golden = SuggestionTextNormalizer.normalize(GOLDEN_QUESTION);
        given(repository.findByNormalizedTextIn(org.mockito.ArgumentMatchers.anyCollection())).willAnswer(invocation -> {
            java.util.Collection<String> keys = invocation.getArgument(0);
            return keys.contains(golden) ? List.of(golden(GOLDEN_QUESTION, ANSWER)) : List.of();
        });

        // Act
        Map<String, Set<Long>> result = locator.locateAnswerChunks(List.of("Kartımı kaybettim, şimdi ne yapmalıyım?"));

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(retrievalClient);
    }

    @Test
    void givenChunkMatchFails_whenLocateAnswerChunks_thenReturnsEmpty() {
        // Arrange
        given(repository.findByNormalizedTextIn(org.mockito.ArgumentMatchers.anyCollection()))
                .willReturn(List.of(golden(GOLDEN_QUESTION, ANSWER)));
        given(retrievalClient.findChunkMatches(anyList())).willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        // Act
        Map<String, Set<Long>> result = locator.locateAnswerChunks(List.of(GOLDEN_QUESTION));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void givenAnswerTextNotFoundInAnyChunk_whenLocateAnswerChunks_thenQuestionOmitted() {
        // Arrange
        given(repository.findByNormalizedTextIn(org.mockito.ArgumentMatchers.anyCollection()))
                .willReturn(List.of(golden(GOLDEN_QUESTION, ANSWER)));
        given(retrievalClient.findChunkMatches(List.of(ANSWER)))
                .willReturn(new ChunkMatchesResponse(List.of(new ChunkTextMatch(ANSWER, List.of()))));

        // Act
        Map<String, Set<Long>> result = locator.locateAnswerChunks(List.of(GOLDEN_QUESTION));

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void givenTwoGoldenQuestions_whenLocateAnswerChunks_thenSingleRetrievalCallMapsByPosition() {
        // Arrange
        GoldenQuestion first = golden(GOLDEN_QUESTION, ANSWER);
        GoldenQuestion second = golden("Kart limitim nasıl artar?", "Limit artırımı için mobil uygulamadan başvuru yapabilirsiniz.");
        given(repository.findByNormalizedTextIn(org.mockito.ArgumentMatchers.anyCollection())).willReturn(List.of(first, second));
        given(retrievalClient.findChunkMatches(anyList())).willReturn(new ChunkMatchesResponse(List.of(
                new ChunkTextMatch("a", List.of(new MatchedChunk(151, 1, "R", "2026.10", 19))),
                new ChunkTextMatch("b", List.of(new MatchedChunk(160, 1, "R", "2026.10", 20),
                        new MatchedChunk(161, 1, "R", "2026.10", 20))))));

        // Act
        Map<String, Set<Long>> result = locator.locateAnswerChunks(List.of(GOLDEN_QUESTION, "Kart limitim nasıl artar?"));

        // Assert
        ArgumentCaptor<List<String>> texts = ArgumentCaptor.captor();
        verify(retrievalClient).findChunkMatches(texts.capture());
        assertThat(texts.getValue()).containsExactly(ANSWER, second.getAnswerText());
        assertThat(result.get(first.getNormalizedText())).containsExactly(151L);
        assertThat(result.get(second.getNormalizedText())).containsExactlyInAnyOrder(160L, 161L);
    }

    @Test
    void givenQuestionNormalizingToEmpty_whenLocateAnswerChunks_thenNoLookupAtAll() {
        // Act
        Map<String, Set<Long>> result = locator.locateAnswerChunks(List.of("???", "  "));

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(repository, retrievalClient);
    }
}
