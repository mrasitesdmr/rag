package com.rasitbank.retrieval.ingestion.parsing;

import java.util.List;

/**
 * Okunmuş PDF'in sayfa ve bölüm sayısını ve başlıklara göre ayrılmış kısımlarını taşır.
 */
public record ParsedDocument(int pageCount, int chapterCount, List<ParsedSection> sections) {

    public ParsedDocument {
        sections = List.copyOf(sections);
    }
}
