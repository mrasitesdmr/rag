package com.rasitbank.retrieval.ingestion.parsing;

/**
 * Sayfadan okunmuş tek bir satırdır: metni ve boşluk dışındaki bütün karakterlerinin kalın yazı tipinde olup olmadığı.
 */
public record ExtractedLine(String text, boolean isBold) {
}
