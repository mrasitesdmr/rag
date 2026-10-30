package com.rasitbank.retrieval.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.ingestion.chunking.ChunkDraft;
import com.rasitbank.retrieval.ingestion.domain.dto.IngestionJobResponse;
import org.junit.jupiter.api.Test;

/**
 * Embedding girdisinin ve durum adresinin biçimini doğrular.
 */
class ResponseAndDraftTest {

    @Test
    void givenDraft_whenEmbeddingInput_thenHeadingBlankLineContent() {
        ChunkDraft draft = new ChunkDraft(0, 5, "2.3 Ek kart", "içerik", 3);

        assertThat(draft.embeddingInput()).isEqualTo("2.3 Ek kart\n\niçerik");
    }

    @Test
    void givenWorkspaceAndJob_whenStatusUrl_thenPathMatchesContract() {
        assertThat(IngestionJobResponse.statusUrl(42, 7)).isEqualTo("/api/workspaces/42/ingestion-jobs/7");
    }
}
