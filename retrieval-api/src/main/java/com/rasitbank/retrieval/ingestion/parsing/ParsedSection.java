package com.rasitbank.retrieval.ingestion.parsing;

import java.util.List;

/**
 * Belgenin bir başlık altındaki kısmıdır: başlık metni ve o başlığa ait satırlar.
 */
public record ParsedSection(String heading, List<PageLine> lines) {

    public ParsedSection {
        lines = List.copyOf(lines);
    }
}
