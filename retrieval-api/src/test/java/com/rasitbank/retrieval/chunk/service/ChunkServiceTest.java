package com.rasitbank.retrieval.chunk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.rasitbank.retrieval.chunk.domain.dto.ChunkSummaryResponse;
import com.rasitbank.retrieval.chunk.repository.ChunkRepository;
import com.rasitbank.retrieval.exception.NotFoundException;
import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import com.rasitbank.retrieval.source.repository.SourceRepository;
import com.rasitbank.retrieval.source.repository.SourceVersionRepository;
import com.rasitbank.retrieval.workspace.service.WorkspaceService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

/**
 * Parça listesinin sürümsüz kaynakta veritabanına gitmediğini ve filtre metninin Türkçe küçük harf ve tek boşluğa
 * çevrildiğini doğrular.
 */
@ExtendWith(MockitoExtension.class)
class ChunkServiceTest {

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private SourceVersionRepository sourceVersionRepository;

    @Mock
    private ChunkRepository chunkRepository;

    private ChunkService service() {
        return new ChunkService(workspaceService, sourceRepository, sourceVersionRepository, chunkRepository);
    }

    @Test
    void givenSourceWithoutActiveVersion_whenListChunks_thenEmptyPageWithoutQuery() {
        // Arrange
        Source source = Source.create(42, "Sürümsüz", SourceKind.PDF, Instant.now());
        given(sourceRepository.findByIdAndWorkspaceId(10001, 42)).willReturn(Optional.of(source));

        // Act
        Page<ChunkSummaryResponse> page = service().listChunks(42, 10001, "asgari", 0, 12);

        // Assert
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getSize()).isEqualTo(12);
        then(chunkRepository).should(org.mockito.Mockito.never()).findActiveVersionChunks(org.mockito.ArgumentMatchers.anyLong(),
                any(), any());
    }

    @Test
    void givenUnknownSource_whenListChunks_thenSourceNotFound() {
        // Arrange
        given(sourceRepository.findByIdAndWorkspaceId(99, 42)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> service().listChunks(42, 99, null, 0, 12));

        // Assert
        assertThat(thrown).isInstanceOf(NotFoundException.class);
        assertThat(((NotFoundException) thrown).getErrorCode()).isEqualTo("SOURCE_NOT_FOUND");
    }

    @Test
    void givenUnknownChunk_whenGetChunk_thenChunkNotFound() {
        // Arrange
        given(chunkRepository.findByIdAndWorkspaceId(7, 42)).willReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() -> service().getChunk(42, 7));

        // Assert
        assertThat(thrown).isInstanceOf(NotFoundException.class);
        assertThat(((NotFoundException) thrown).getErrorCode()).isEqualTo("CHUNK_NOT_FOUND");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            '  ASGARİ\n ÖDEME '|asgari ödeme
            KISA|kısa
            'bir   iki\tüç'|bir iki üç
            Kredi|kredi
            """)
    void givenQueries_whenNormalizeQuery_thenTurkishLowerCaseSingleSpaces(String raw, String expected) {
        // Act
        String normalized = ChunkService.normalizeQuery(raw);

        // Assert
        assertThat(normalized).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", "   ", "\n\t "})
    void givenBlankOrNullQuery_whenNormalizeQuery_thenNull(String raw) {
        // Act & Assert
        assertThat(ChunkService.normalizeQuery(raw)).isNull();
    }
}
