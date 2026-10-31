package com.rasitbank.retrieval.ingestion.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.rasitbank.retrieval.source.domain.entity.Source;
import com.rasitbank.retrieval.source.domain.entity.SourceKind;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Kaynak adının veritabanında harf farksız tekil olduğunu ve arama sorgusunun aynı kuralı kullandığını doğrular.
 */
class SourceNameIndexIntegrationTest extends AbstractIngestionIntegrationTest {

    @Test
    void givenSourceRehber_whenSourceREHBERSaved_thenUniqueIndexRejects() {
        // Arrange
        sourceRepository.saveAndFlush(Source.create(42, "Rehber", SourceKind.PDF, Instant.now()));

        // Act
        Throwable thrown = catchThrowable(
                () -> sourceRepository.saveAndFlush(Source.create(42, "REHBER", SourceKind.PDF, Instant.now())));

        // Assert
        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(count("sources")).isEqualTo(1);
    }

    @Test
    void givenSourceRehber_whenFindByLowerNameUpperCase_thenFindsIt() {
        // Arrange
        long id = sourceRepository.saveAndFlush(Source.create(42, "Rehber", SourceKind.PDF, Instant.now())).getId();

        // Act
        var found = sourceRepository.findByLowerName(42, "REHBER");

        // Assert
        assertThat(found).map(Source::getId).contains(id);
        assertThat(sourceRepository.findByLowerName(42, "Başka")).isEmpty();
    }
}
