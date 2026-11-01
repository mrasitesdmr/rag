package com.rasitbank.assistant.retrieval.client;

/**
 * retrieval-api'nin vektör arama ucuna giden isteği taşır: aranacak metin ve en fazla kaç parça döneceği.
 */
public record SearchChunksRequest(String query, int topK) {
}
