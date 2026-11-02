package com.rasitbank.assistant.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.retrieval.service.RetrievedChunk;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import com.rasitbank.assistant.support.ContextFixtures;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Cevap parçasının vector sırasını ve prompt'taki yerini bulur; fixture'da chunkId = 100 + vector sırası.
 */
class AnswerChunkRankerTest {

    private final AnswerChunkRanker ranker = new AnswerChunkRanker();

    @Test
    void givenAnswerChunkAtVectorSevenRerankedToSecond_whenRank_thenVectorSevenPromptTwo() {
        // Arrange: prompt sırası vector 3, 7, 1, 2, 9; cevap parçası 107 (vector 7) prompt'ta 2.
        RetrievedContext context = ContextFixtures.rerankContext(3, 7, 1, 2, 9);

        // Act
        QuestionRank rank = ranker.rank(context, Set.of(107L));

        // Assert
        assertThat(rank.vectorRank()).isEqualTo(7);
        assertThat(rank.promptPosition()).isEqualTo(2);
    }

    @Test
    void givenAnswerInTwoChunks_whenRank_thenUsesFirstInEachOrder() {
        // Arrange: A = 109 (vector 9) prompt'ta 1.; B = 103 (vector 3) prompt'ta 4.
        RetrievedContext context = ContextFixtures.rerankContext(9, 5, 6, 3, 8);

        // Act
        QuestionRank rank = ranker.rank(context, Set.of(109L, 103L));

        // Assert: vectorRank adaylar vector sırasıyla ilk geçen (3), prompt yeri prompt sırasıyla ilk geçen (1)
        assertThat(rank.vectorRank()).isEqualTo(3);
        assertThat(rank.promptPosition()).isEqualTo(1);
    }

    @Test
    void givenAnswerChunkNotInCandidates_whenRank_thenBothNull() {
        // Act
        QuestionRank rank = ranker.rank(ContextFixtures.rerankContext(1, 2, 3, 4, 5), Set.of(999L));

        // Assert
        assertThat(rank.vectorRank()).isNull();
        assertThat(rank.promptPosition()).isNull();
    }

    @Test
    void givenAnswerChunkInCandidatesButNotInPrompt_whenRank_thenVectorRankSetPromptNull() {
        // Act: 112 adaylarda 12. sırada, prompt'ta yok
        QuestionRank rank = ranker.rank(ContextFixtures.rerankContext(1, 2, 3, 4, 5), Set.of(112L));

        // Assert
        assertThat(rank.vectorRank()).isEqualTo(12);
        assertThat(rank.promptPosition()).isNull();
    }

    @Test
    void givenTopKContext_whenRankFirstChunk_thenVectorOnePromptOne() {
        // Act
        QuestionRank rank = ranker.rank(ContextFixtures.topKContext(), Set.of(101L));

        // Assert
        assertThat(rank).isEqualTo(new QuestionRank(1, 1));
    }
}
