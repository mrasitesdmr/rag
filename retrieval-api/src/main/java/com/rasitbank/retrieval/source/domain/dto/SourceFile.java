package com.rasitbank.retrieval.source.domain.dto;

/**
 * Bir kaynağın saklanan PDF'ini dosya adıyla birlikte controller'a taşır.
 */
public record SourceFile(String fileName, byte[] content) {
}
