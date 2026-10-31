package com.rasitbank.retrieval.source.domain.dto;

/**
 * Doğrulamadan geçmiş bir belge yüklemesini taşır: kaynak adı, sürüm etiketi, dosya adı ve PDF baytları.
 */
public record SourceUpload(String name, String version, String fileName, byte[] content) {
}
