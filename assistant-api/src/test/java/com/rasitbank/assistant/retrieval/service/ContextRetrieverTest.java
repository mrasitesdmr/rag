package com.rasitbank.assistant.retrieval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.client.RerankClient;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.support.SearchHitFixtures;
import com.rasitbank.assistant.support.TestProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prompt'a girecek parçaların stratejiye göre doğru seçildiğini, sıralandığını ve reranker düşünce vector
 * sırasına dönüldüğünü doğrular.
 */
@ExtendWith(MockitoExtension.class)
class ContextRetrieverTest {

    @Mock
    private RetrievalClient retrievalClient;

    @Mock
    private RerankClient rerankClient;

    private RerankerStatusTracker tracker;

    private ContextRetriever retriever(AssistantProperties properties) {
        tracker = new RerankerStatusTracker(properties);
        return new ContextRetriever(retrievalClient, rerankClient, tracker, properties);
    }

    private static List<Double> scores(int count, double baseline, Object... rankScorePairs) {
        Double[] scores = new Double[count];
        Arrays.fill(scores, baseline);
        for (int i = 0; i < rankScorePairs.length; i += 2) {
            scores[(Integer) rankScorePairs[i] - 1] = (Double) rankScorePairs[i + 1];
        }
        return new ArrayList<>(Arrays.asList(scores));
    }

    private static List<Integer> vectorRanks(List<RetrievedChunk> chunks) {
        return chunks.stream().map(RetrievedChunk::vectorRank).toList();
    }

    @Test
    void givenRerankStrategy_whenRetrieveContext_thenSendsTwentyCandidatesAndPromptsTopFiveByScore() {
        given(retrievalClient.search("soru", 20)).willReturn(SearchHitFixtures.hits(20));
        given(rerankClient.rerankChunks(eq("soru"), anyList()))
                .willReturn(scores(20, 1.0, 7, 9.1, 2, 8.0, 15, 7.5, 1, 6.0, 3, 5.0));

        RetrievedContext context = retriever(TestProperties.defaults()).retrieveContext("soru");

        assertThat(vectorRanks(context.contextChunks())).containsExactly(7, 2, 15, 1, 3);
        assertThat(context.strategy()).isEqualTo(RetrievalStrategy.RERANK);
        assertThat(context.candidates()).isEqualTo(20);
        assertThat(context.topK()).isEqualTo(5);
        assertThat(context.candidateChunks()).hasSize(20);
        assertThat(vectorRanks(context.candidateChunks())).isEqualTo(java.util.stream.IntStream.rangeClosed(1, 20)
                .boxed().toList());
        assertThat(context.contextChunks().get(0).rerankScore()).isEqualTo(9.1);
        assertThat(context.candidateChunks().get(9).rerankScore()).isEqualTo(1.0);
        assertThat(context.rerankLatencyMs()).isNotNull();
        ArgumentCaptor<List<String>> documents = ArgumentCaptor.captor();
        verify(rerankClient).rerankChunks(eq("soru"), documents.capture());
        assertThat(documents.getValue()).hasSize(20);
        assertThat(documents.getValue().get(0)).isEqualTo("H101\n\nC101");
        assertThat(documents.getValue().get(19)).isEqualTo("H120\n\nC120");
    }

    @Test
    void givenSlowReranker_whenRetrieveContext_thenRerankLatencyMeasuredInMilliseconds() {
        given(retrievalClient.search(anyString(), anyInt())).willAnswer(invocation -> {
            Thread.sleep(60);
            return SearchHitFixtures.hits(3);
        });
        given(rerankClient.rerankChunks(anyString(), anyList())).willAnswer(invocation -> {
            Thread.sleep(80);
            return List.of(1.0, 2.0, 3.0);
        });

        RetrievedContext context = retriever(TestProperties.defaults()).retrieveContext("soru");

        assertThat(context.retrievalLatencyMs()).isBetween(60L, 1000L);
        assertThat(context.rerankLatencyMs()).isBetween(80L, 1000L);
    }

    @Test
    void givenEqualTopScores_whenRetrieveContext_thenLowerVectorRankFirst() {
        given(retrievalClient.search(anyString(), anyInt())).willReturn(SearchHitFixtures.hits(20));
        given(rerankClient.rerankChunks(anyString(), anyList()))
                .willReturn(scores(20, 1.0, 9, 9.0, 3, 9.0, 12, 4.0, 5, 3.0, 1, 2.0, 2, 1.5));

        RetrievedContext context = retriever(TestProperties.defaults()).retrieveContext("soru");

        assertThat(vectorRanks(context.contextChunks())).containsExactly(3, 9, 12, 5, 1);
    }

    @Test
    void givenNegativeScores_whenRetrieveContext_thenHighestScoreStillFirst() {
        given(retrievalClient.search(anyString(), anyInt())).willReturn(SearchHitFixtures.hits(6));
        given(rerankClient.rerankChunks(anyString(), anyList()))
                .willReturn(List.of(-11.04, -3.0, -0.5, -7.0, -9.0, -2.0));

        RetrievedContext context = retriever(TestProperties.defaults()).retrieveContext("soru");

        assertThat(vectorRanks(context.contextChunks())).containsExactly(3, 6, 2, 4, 5);
    }

    @Test
    void givenTop5Strategy_whenRetrieveContext_thenSearchesFiveAndNeverCallsReranker() {
        given(retrievalClient.search("soru", 5)).willReturn(SearchHitFixtures.hits(5));

        RetrievedContext context = retriever(TestProperties.withStrategy("top5")).retrieveContext("soru");

        verify(retrievalClient).search("soru", 5);
        verifyNoInteractions(rerankClient);
        assertThat(context.strategy()).isEqualTo(RetrievalStrategy.TOP_K);
        assertThat(vectorRanks(context.contextChunks())).containsExactly(1, 2, 3, 4, 5);
        assertThat(context.contextChunks()).allSatisfy(chunk -> assertThat(chunk.rerankScore()).isNull());
        assertThat(context.rerankLatencyMs()).isNull();
    }

    @Test
    void givenRerankerUnavailable_whenRetrieveContext_thenFirstFiveInVectorOrderAsFallback() {
        given(retrievalClient.search("soru", 20)).willReturn(SearchHitFixtures.hits(20));
        given(rerankClient.rerankChunks(anyString(), anyList()))
                .willThrow(new ServiceUnavailableException("RERANKER_UNAVAILABLE", "down"));

        RetrievedContext context = retriever(TestProperties.defaults()).retrieveContext("soru");

        assertThat(context.strategy()).isEqualTo(RetrievalStrategy.RERANK_FALLBACK);
        assertThat(vectorRanks(context.contextChunks())).containsExactly(1, 2, 3, 4, 5);
        assertThat(context.contextChunks()).allSatisfy(chunk -> assertThat(chunk.rerankScore()).isNull());
        assertThat(context.candidateChunks()).hasSize(20)
                .allSatisfy(chunk -> assertThat(chunk.rerankScore()).isNull());
        assertThat(context.rerankLatencyMs()).isNotNull();
    }

    @Test
    void givenThreeHits_whenRetrieveContextWithRerank_thenRerankThreeAndPromptThree() {
        given(retrievalClient.search(anyString(), anyInt())).willReturn(SearchHitFixtures.hits(3));
        given(rerankClient.rerankChunks(anyString(), anyList())).willReturn(List.of(1.0, 3.0, 2.0));

        RetrievedContext context = retriever(TestProperties.defaults()).retrieveContext("soru");

        ArgumentCaptor<List<String>> documents = ArgumentCaptor.captor();
        verify(rerankClient).rerankChunks(anyString(), documents.capture());
        assertThat(documents.getValue()).hasSize(3);
        assertThat(vectorRanks(context.contextChunks())).containsExactly(2, 3, 1);
    }

    @Test
    void givenNoHits_whenRetrieveContext_thenEmptyContextAndRerankerNotCalled() {
        given(retrievalClient.search(anyString(), anyInt())).willReturn(List.of());

        RetrievedContext context = retriever(TestProperties.defaults()).retrieveContext("soru");

        assertThat(context.contextChunks()).isEmpty();
        assertThat(context.candidateChunks()).isEmpty();
        verify(rerankClient, never()).rerankChunks(anyString(), any());
    }

    @Test
    void givenRetrievalUnavailable_whenRetrieveContext_thenExceptionPropagates() {
        given(retrievalClient.search(anyString(), anyInt()))
                .willThrow(new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down"));

        assertThatThrownBy(() -> retriever(TestProperties.defaults()).retrieveContext("soru"))
                .isInstanceOfSatisfying(ServiceUnavailableException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("RETRIEVAL_UNAVAILABLE"));
        verifyNoInteractions(rerankClient);
    }

    @Test
    void givenRerankerFails_whenRetrieveContext_thenTrackerDown() {
        // Arrange
        given(retrievalClient.search(anyString(), anyInt())).willReturn(SearchHitFixtures.hits(20));
        given(rerankClient.rerankChunks(anyString(), anyList()))
                .willThrow(new ServiceUnavailableException("RERANKER_UNAVAILABLE", "down"));
        ContextRetriever retriever = retriever(TestProperties.defaults());
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.UNKNOWN);

        // Act
        retriever.retrieveContext("soru");

        // Assert
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.DOWN);
    }

    @Test
    void givenRerankSucceeds_whenRetrieveContext_thenTrackerUp() {
        // Arrange
        given(retrievalClient.search(anyString(), anyInt())).willReturn(SearchHitFixtures.hits(3));
        given(rerankClient.rerankChunks(anyString(), anyList())).willReturn(List.of(1.0, 3.0, 2.0));
        ContextRetriever retriever = retriever(TestProperties.defaults());

        // Act
        retriever.retrieveContext("soru");

        // Assert
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.UP);
    }

    @Test
    void givenTopKStrategy_whenRetrieveContext_thenTrackerStaysNotUsed() {
        // Arrange
        given(retrievalClient.search("soru", 5)).willReturn(SearchHitFixtures.hits(5));
        ContextRetriever retriever = retriever(TestProperties.withStrategy("top5"));

        // Act
        retriever.retrieveContext("soru");

        // Assert
        assertThat(tracker.currentStatus()).isEqualTo(RerankerStatus.NOT_USED);
    }

    @Test
    void givenTopKStrategy_whenRetrieveCandidatesAndContext_thenSearchesTwentyAndKeepsFirstFive() {
        // Arrange
        given(retrievalClient.search("soru", 20)).willReturn(SearchHitFixtures.hits(20));

        // Act
        RetrievedContext context = retriever(TestProperties.withStrategy("top5")).retrieveCandidatesAndContext("soru");

        // Assert
        assertThat(context.strategy()).isEqualTo(RetrievalStrategy.TOP_K);
        assertThat(vectorRanks(context.candidateChunks())).isEqualTo(java.util.stream.IntStream.rangeClosed(1, 20)
                .boxed().toList());
        assertThat(vectorRanks(context.contextChunks())).containsExactly(1, 2, 3, 4, 5);
        verifyNoInteractions(rerankClient);
    }

    @Test
    void givenRerankStrategy_whenRetrieveCandidatesAndContext_thenSamePromptAsRetrieveContext() {
        // Arrange
        given(retrievalClient.search("soru", 20)).willReturn(SearchHitFixtures.hits(20));
        given(rerankClient.rerankChunks(eq("soru"), anyList()))
                .willReturn(scores(20, 1.0, 7, 9.1, 2, 8.0, 15, 7.5, 1, 6.0, 3, 5.0));

        // Act
        RetrievedContext context = retriever(TestProperties.defaults()).retrieveCandidatesAndContext("soru");

        // Assert
        assertThat(context.strategy()).isEqualTo(RetrievalStrategy.RERANK);
        assertThat(vectorRanks(context.contextChunks())).containsExactly(7, 2, 15, 1, 3);
        assertThat(context.candidateChunks()).hasSize(20);
    }

    @Test
    void givenRerankerDown_whenRetrieveCandidatesAndContext_thenFallbackWithTwentyCandidatesAndFirstFive() {
        // Arrange
        given(retrievalClient.search("soru", 20)).willReturn(SearchHitFixtures.hits(20));
        given(rerankClient.rerankChunks(eq("soru"), anyList()))
                .willThrow(new ServiceUnavailableException("RERANKER_UNAVAILABLE", "down"));

        // Act
        RetrievedContext context = retriever(TestProperties.defaults()).retrieveCandidatesAndContext("soru");

        // Assert
        assertThat(context.strategy()).isEqualTo(RetrievalStrategy.RERANK_FALLBACK);
        assertThat(context.candidateChunks()).hasSize(20);
        assertThat(vectorRanks(context.contextChunks())).containsExactly(1, 2, 3, 4, 5);
    }
}
