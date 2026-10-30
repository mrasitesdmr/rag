package com.rasitbank.retrieval.ingestion.service;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.embeddingProperties;
import static com.rasitbank.retrieval.fixture.IngestionFixtures.ingestionProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.rasitbank.retrieval.config.EmbeddingProperties;
import com.rasitbank.retrieval.embedding.client.EmbeddingClient;
import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.rasitbank.retrieval.ingestion.chunking.ChunkDraft;
import com.rasitbank.retrieval.ingestion.chunking.TextChunker;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionJob;
import com.rasitbank.retrieval.ingestion.domain.entity.IngestionStage;
import com.rasitbank.retrieval.ingestion.parsing.PageLine;
import com.rasitbank.retrieval.ingestion.parsing.ParsedDocument;
import com.rasitbank.retrieval.ingestion.parsing.ParsedSection;
import com.rasitbank.retrieval.ingestion.parsing.PdfDocumentParser;
import com.rasitbank.retrieval.workspace.domain.entity.Workspace;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Runner'ın aşama sırasını, 96'lık grupları, hata kodu eşlemesini ve yarım yazmama kuralını doğrular.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IngestionJobRunnerTest {

    private static final long JOB_ID = 7L;
    private static final byte[] CONTENT = {1, 2, 3};

    @Mock
    private IngestionJobLifecycleService lifecycle;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private PdfDocumentParser parser;
    @Mock
    private TextChunker chunker;
    @Mock
    private EmbeddingClient embeddingClient;
    @Mock
    private ExecutorService executor;
    @Mock
    private Workspace workspace;

    private final ParsedDocument document = new ParsedDocument(70, 11,
            List.of(new ParsedSection("1 A", List.of(new PageLine(5, "x.")))));

    private IngestionJobRunner runnerFor(EmbeddingProperties embeddingProperties) {
        return new IngestionJobRunner(executor, lifecycle, workspaceService, parser, chunker, embeddingClient,
                embeddingProperties, ingestionProperties());
    }

    private IngestionJobRunner runner() {
        return runnerFor(embeddingProperties("bge-m3", 1024, Duration.ZERO, 2));
    }

    private static List<ChunkDraft> drafts(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new ChunkDraft(index, 5, "1 A", "icerik " + index, 3)).toList();
    }

    @BeforeEach
    void setUp() {
        IngestionJob job = IngestionJob.queued(42, 10001, "2026.10", "r.pdf", 3, Instant.EPOCH);
        given(lifecycle.start(JOB_ID)).willReturn(job);
        given(workspaceService.getWorkspace(42)).willReturn(workspace);
        given(workspace.embeddingModelLabel()).willReturn("bge-m3@1024");
        given(parser.parse(CONTENT)).willReturn(document);
        given(embeddingClient.embed(anyList())).willAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream().map(input -> new float[1024]).toList();
        });
    }

    @Test
    void given150Chunks_whenRun_thenEmbedsInBatchesOf96And54InOrderAndRecordsProgress96Then150() {
        List<ChunkDraft> drafts = drafts(150);
        given(chunker.chunk(document.sections())).willReturn(drafts);

        runner().run(JOB_ID, CONTENT);

        ArgumentCaptor<List<String>> batches = ArgumentCaptor.captor();
        then(embeddingClient).should(Mockito.times(2)).embed(batches.capture());
        assertThat(batches.getAllValues()).extracting(List::size).containsExactly(96, 54);
        assertThat(batches.getAllValues().getFirst())
                .isEqualTo(drafts.subList(0, 96).stream().map(ChunkDraft::embeddingInput).toList());
        assertThat(batches.getAllValues().get(1).getFirst()).isEqualTo(drafts.get(96).embeddingInput());
        InOrder order = Mockito.inOrder(lifecycle);
        order.verify(lifecycle).start(JOB_ID);
        order.verify(lifecycle).moveTo(JOB_ID, IngestionStage.CHUNKING);
        order.verify(lifecycle).recordTotalChunks(JOB_ID, 150);
        order.verify(lifecycle).moveTo(JOB_ID, IngestionStage.EMBEDDING);
        order.verify(lifecycle).recordEmbeddingProgress(JOB_ID, 96);
        order.verify(lifecycle).recordEmbeddingProgress(JOB_ID, 150);
        order.verify(lifecycle).moveTo(JOB_ID, IngestionStage.WRITING);
        ArgumentCaptor<List<float[]>> vectors = ArgumentCaptor.captor();
        order.verify(lifecycle).complete(eq(JOB_ID), eq(document), eq(drafts), vectors.capture());
        assertThat(vectors.getValue()).hasSize(150);
        then(lifecycle).should(never()).fail(anyLong(), anyString());
    }

    @Test
    void givenEmbeddingUnavailableOnSecondBatch_whenRun_thenFailsWithEmbeddingUnavailableAndNeverCompletes() {
        given(chunker.chunk(document.sections())).willReturn(drafts(150));
        given(embeddingClient.embed(anyList()))
                .willReturn(IntStream.range(0, 96).mapToObj(index -> new float[1024]).toList())
                .willThrow(new ServiceUnavailableException("EMBEDDING_UNAVAILABLE", "down"));

        runner().run(JOB_ID, CONTENT);

        then(lifecycle).should().recordEmbeddingProgress(JOB_ID, 96);
        then(lifecycle).should().fail(JOB_ID, "EMBEDDING_UNAVAILABLE");
        then(lifecycle).should(never()).moveTo(JOB_ID, IngestionStage.WRITING);
        then(lifecycle).should(never()).complete(anyLong(), any(), anyList(), anyList());
    }

    @Test
    void givenDimensionMismatchFromClient_whenRun_thenFailsWithThatErrorCode() {
        given(chunker.chunk(document.sections())).willReturn(drafts(2));
        given(embeddingClient.embed(anyList()))
                .willThrow(new ServiceUnavailableException("EMBEDDING_DIMENSION_MISMATCH", "512"));

        runner().run(JOB_ID, CONTENT);

        then(lifecycle).should().fail(JOB_ID, "EMBEDDING_DIMENSION_MISMATCH");
        then(lifecycle).should(never()).complete(anyLong(), any(), anyList(), anyList());
    }

    @Test
    void givenConfiguredModelDiffersFromWorkspace_whenRun_thenFailsWithEmbeddingModelMismatchBeforeParsing() {
        IngestionJobRunner mismatched = runnerFor(embeddingProperties("nomic-embed-text", 768, Duration.ZERO, 2));

        mismatched.run(JOB_ID, CONTENT);

        then(lifecycle).should().fail(JOB_ID, "EMBEDDING_MODEL_MISMATCH");
        then(parser).shouldHaveNoInteractions();
        then(embeddingClient).shouldHaveNoInteractions();
    }

    @Test
    void givenParserThrowsRuntimeException_whenRun_thenFailsWithInternalError() {
        given(parser.parse(CONTENT)).willThrow(new IllegalStateException("boom"));

        runner().run(JOB_ID, CONTENT);

        then(lifecycle).should().fail(JOB_ID, "INTERNAL_ERROR");
    }

    @Test
    void givenParserThrowsPdfUnreadable_whenRun_thenFailsWithPdfUnreadable() {
        given(parser.parse(CONTENT)).willThrow(
                new com.rasitbank.retrieval.exception.UnprocessableContentException("PDF_UNREADABLE", "bad"));

        runner().run(JOB_ID, CONTENT);

        then(lifecycle).should().fail(JOB_ID, "PDF_UNREADABLE");
        then(lifecycle).should(never()).moveTo(JOB_ID, IngestionStage.CHUNKING);
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
