package com.rasitbank.assistant.retrieval.client;

import java.util.List;

/**
 * retrieval-api'nin metin eşleştirme ucuna gönderilen, hangi parçalarda geçtiği aranacak metinleri taşır.
 */
public record FindChunkMatchesRequest(List<String> texts) {

    public FindChunkMatchesRequest {
        texts = List.copyOf(texts);
    }
}
