package com.rasitbank.retrieval.ingestion.parsing;

import java.util.List;

/**
 * Okunmuş PDF'in sayfa ve bölüm sayısını, başlıklara göre ayrılmış kısımlarını ve atılan üst/alt bilgi satırı sayısını taşır.
 */
public record ParsedDocument(int pageCount, int chapterCount, List<ParsedSection> sections, int removedMarginLines) {

    public ParsedDocument {
        sections = List.copyOf(sections);
    }
}
