package com.rasitbank.retrieval.embedding.client;

import java.util.List;

/**
 * Ollama'nın /api/embed ucuna giden toplu embedding isteğinin gövdesidir.
 */
public record OllamaEmbedRequest(String model, List<String> input) {

    public OllamaEmbedRequest {
        input = List.copyOf(input);
    }
}
