package com.rasitbank.retrieval.exception;

/**
 * Doğrulama hatasında tek bir alanın ihlalini (alan adı ve sabit İngilizce mesaj) taşır.
 */
public record Violation(String field, String message) {
}
