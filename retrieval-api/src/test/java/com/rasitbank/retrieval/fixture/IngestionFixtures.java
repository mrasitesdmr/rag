package com.rasitbank.retrieval.fixture;

import com.rasitbank.retrieval.config.EmbeddingProperties;
import com.rasitbank.retrieval.config.IngestionProperties;
import com.rasitbank.retrieval.ingestion.parsing.PageLine;
import com.rasitbank.retrieval.ingestion.parsing.ParsedSection;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Testlerde kullanılan ayarları ve satır/kısım örneklerini üretir.
 */
public final class IngestionFixtures {

    public static final String HEADER_PATTERN = "^Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm .+$";
    public static final String FOOTER_PATTERN = "^Yürürlük: .+ · Sayfa \\d+ / \\d+$";

    private IngestionFixtures() {
    }

    public static IngestionProperties ingestionProperties() {
        return new IngestionProperties(1600, 2000, 200, 4, 96, HEADER_PATTERN, FOOTER_PATTERN,
                new IngestionProperties.Bootstrap(false, Path.of("yok.pdf"), "Raşit Bankası Ürün ve Hizmet Rehberi",
                        "2026.10"));
    }

    public static EmbeddingProperties embeddingProperties(String model, int dimensions, Duration backoff,
            int maxConcurrent) {
        return new EmbeddingProperties("http://localhost:11434", model, dimensions, Duration.ofSeconds(2),
                Duration.ofSeconds(5), 3, backoff, maxConcurrent);
    }

    public static ParsedSection section(String heading, int page, int lineCount, int lineLength, char endChar) {
        List<PageLine> lines = new ArrayList<>();
        for (int index = 0; index < lineCount; index++) {
            lines.add(new PageLine(page, line(index, lineLength, endChar)));
        }
        return new ParsedSection(heading, lines);
    }

    public static String line(int index, int length, char endChar) {
        String prefix = "L" + String.format("%03d", index) + " ";
        return (prefix + "x".repeat(length)).substring(0, length - 1) + endChar;
    }
}
