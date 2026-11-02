package com.rasitbank.assistant.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.conversation.domain.dto.ConversationResponse;
import com.rasitbank.assistant.conversation.domain.dto.CreateConversationRequest;
import com.rasitbank.assistant.conversation.domain.entity.Channel;
import com.rasitbank.assistant.conversation.domain.entity.Conversation;
import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.RetrievalStrategy;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.domain.entity.TurnChunk;
import com.rasitbank.assistant.conversation.repository.TurnRepository;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.support.ContextFixtures;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Konuşma ve turun veritabanına yazılıp aynı değerlerle okunduğunu (entity eşlemesi) ve olmayan konuşmanın
 * CONVERSATION_NOT_FOUND verdiğini doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ConversationPersistenceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private TurnService turnService;

    @Autowired
    private TurnRepository turnRepository;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void givenCreatedConversation_whenGetConversation_thenChannelAndCreatedAtRead() {
        ConversationResponse created = conversationService.createConversation(
                new CreateConversationRequest(Channel.PREVIEW));

        Conversation loaded = conversationService.getConversation(created.conversationId());

        assertThat(created.channel()).isEqualTo(Channel.PREVIEW);
        assertThat(created.createdAt()).isCloseTo(Instant.now(), within(java.time.Duration.ofMinutes(1)));
        assertThat(loaded.getChannel()).isEqualTo(Channel.PREVIEW);
        assertThat(loaded.getCreatedAt()).isCloseTo(created.createdAt(), within(java.time.Duration.ofMillis(1)));
    }

    @Test
    void givenUnknownId_whenGetConversation_thenConversationNotFound() {
        assertThatThrownBy(() -> conversationService.getConversation(987_654_321L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("CONVERSATION_NOT_FOUND"));
    }

    @Test
    void givenRecordedTurn_whenReadBack_thenEveryColumnAndCandidateMapsToEntity() {
        long conversationId = conversationService.createConversation(new CreateConversationRequest(Channel.PREVIEW))
                .conversationId();
        TurnDraft draft = new TurnDraft(conversationId, GenerationMode.RAG, "Kartımı kaybettim", "Kartımı kaybettim",
                QueryRewriteStatus.NO_HISTORY, null, 0, "qwen3.5:9b",
                Instant.parse("2026-11-01T10:00:00Z"), System.nanoTime(),
                ContextFixtures.rerankContext(7, 2, 15, 1, 3));
        long turnId = turnService.recordTurn(draft,
                new TurnOutcome("Kapat [1]", false, List.of(1, 3), 2154, 61, 3920L, 900L, 4730L, null));

        transactions.executeWithoutResult(status -> {
            Turn turn = turnRepository.findById(turnId).orElseThrow();

            assertThat(turn.getConversation().getId()).isEqualTo(conversationId);
            assertThat(turn.getTurnNo()).isEqualTo(1);
            assertThat(turn.getQuestion()).isEqualTo("Kartımı kaybettim");
            assertThat(turn.getSearchQuery()).isEqualTo("Kartımı kaybettim");
            assertThat(turn.getGenerationMode()).isEqualTo(GenerationMode.RAG);
            assertThat(turn.getRetrievalStrategy()).isEqualTo(RetrievalStrategy.RERANK);
            assertThat(turn.getCandidates()).isEqualTo(20);
            assertThat(turn.getTopK()).isEqualTo(5);
            assertThat(turn.getChatModel()).isEqualTo("qwen3.5:9b");
            assertThat(turn.getAnswer()).isEqualTo("Kapat [1]");
            assertThat(turn.isNotFoundAnswer()).isFalse();
            assertThat(turn.getCited()).containsExactly(1, 3);
            assertThat(turn.getPromptTokens()).isEqualTo(2154);
            assertThat(turn.getCompletionTokens()).isEqualTo(61);
            assertThat(turn.getRetrievalLatencyMs()).isEqualTo(84);
            assertThat(turn.getRerankLatencyMs()).isEqualTo(712);
            assertThat(turn.getGenerationLatencyMs()).isEqualTo(3920);
            assertThat(turn.getTimeToFirstTokenMs()).isEqualTo(900);
            assertThat(turn.getLatencyMs()).isEqualTo(4730);
            assertThat(turn.getErrorCode()).isNull();
            assertThat(turn.getCreatedAt()).isEqualTo(Instant.parse("2026-11-01T10:00:00Z"));
            assertThat(turn.getCompletedAt()).isAfter(turn.getCreatedAt());
            assertThat(turn.getChunks()).hasSize(20);
            TurnChunk vectorSeven = turn.getChunks().stream().filter(chunk -> chunk.getVectorRank() == 7)
                    .findFirst().orElseThrow();
            assertThat(vectorSeven.getPromptPosition()).isEqualTo(1);
            assertThat(vectorSeven.getChunkId()).isEqualTo(107L);
            assertThat(vectorSeven.getChunkNo()).isEqualTo(7);
            assertThat(vectorSeven.getPageNumber()).isEqualTo(17);
            assertThat(vectorSeven.getHeading()).isEqualTo("H107");
            assertThat(vectorSeven.getSourceId()).isEqualTo(10001L);
            assertThat(vectorSeven.getVersion()).isEqualTo("2026.10");
            assertThat(vectorSeven.getDistance()).isCloseTo(0.37, within(1e-9));
            assertThat(vectorSeven.getRerankScore()).isCloseTo(9.3, within(1e-9));
        });
    }
}
