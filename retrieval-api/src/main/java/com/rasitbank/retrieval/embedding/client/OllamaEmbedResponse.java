package com.rasitbank.retrieval.embedding.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Ollama'nın /api/embed ucunun döndürdüğü, giriş sırasıyla gelen vektörlerdir.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaEmbedResponse(@Nullable List<float[]> embeddings) {
}
