package com.rasitbank.retrieval.ingestion.parsing;

/**
 * PDF'ten çıkarılmış, bulunduğu sayfa numarasıyla birlikte tek bir metin satırıdır; soru-cevap kısmındaki soru
 * satırı olup olmadığını da taşır.
 */
public record PageLine(int pageNumber, String text, boolean isQuestion) {

    public PageLine(int pageNumber, String text) {
        this(pageNumber, text, false);
    }
}
