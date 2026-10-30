package com.rasitbank.retrieval.ingestion.parsing;

/**
 * PDF'ten çıkarılmış, bulunduğu sayfa numarasıyla birlikte tek bir metin satırıdır.
 */
public record PageLine(int pageNumber, String text) {
}
