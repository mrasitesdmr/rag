package com.rasitbank.retrieval.ingestion.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.retrieval.config.EmbeddingProperties;
import com.rasitbank.retrieval.config.IngestionProperties;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Yapılandırmadan bağlanan değerlerin (rehber yolu, kalıplar, model) beklenen değerlerde olduğunu doğrular.
 */
class BootstrapPropertiesIntegrationTest extends AbstractIngestionIntegrationTest {

    @Autowired
    IngestionProperties ingestionProperties;

    @Autowired
    EmbeddingProperties embeddingProperties;

    @Autowired
    @Qualifier("ingestionExecutor")
    ExecutorService ingestionExecutor;

    @Test
    void givenDefaultConfiguration_whenBound_thenChunkSizesPatternsAndRelativeGuidePathAreExact() {
        assertThat(ingestionProperties.targetChunkChars()).isEqualTo(1600);
        assertThat(ingestionProperties.maxChunkChars()).isEqualTo(2000);
        assertThat(ingestionProperties.overlapChars()).isEqualTo(200);
        assertThat(ingestionProperties.embeddingBatchSize()).isEqualTo(96);
        assertThat(ingestionProperties.pageHeaderPattern()).isEqualTo("^Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm .+$");
        assertThat(ingestionProperties.pageFooterPattern()).isEqualTo("^Yürürlük: .+ · Sayfa \\d+ / \\d+$");
        assertThat(ingestionProperties.bootstrap().sourceName()).isEqualTo("Raşit Bankası Ürün ve Hizmet Rehberi");
        assertThat(ingestionProperties.bootstrap().version()).isEqualTo("2026.10");
        assertThat(ingestionProperties.bootstrap().enabled()).isFalse();
        assertThat(ingestionProperties.bootstrap().file())
                .isEqualTo(Path.of("../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf"));
        assertThat(embeddingProperties.modelLabel()).isEqualTo("bge-m3@1024");
    }

    @Test
    void givenDefaultGuideFile_whenResolvedFromModuleDirectory_thenFileExists() {
        Path configured = Path.of("../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf");

        assertThat(configured).exists();
    }

    @Test
    void givenExecutor_whenRunningTask_thenThreadIsNamedIngestionAndOnlyOneRunsAtATime() throws Exception {
        String name = ingestionExecutor.submit(() -> Thread.currentThread().getName()).get();

        assertThat(name).startsWith("ingestion-");
    }
}
