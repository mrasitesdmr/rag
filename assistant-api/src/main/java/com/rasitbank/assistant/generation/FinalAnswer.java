package com.rasitbank.assistant.generation;

import java.util.List;

/**
 * Dil modelinin cevabının tura kaydedilecek son halini taşır: denetlenmiş metin, "bulamadım" işareti, geçerli alıntı
 * numaraları, giriş cümlesinden silinen alıntı sayısı ve ilk cümle dışında yazıldığı için silinen giriş cümlesi sayısı.
 */
public record FinalAnswer(String answer, boolean notFoundAnswer, List<Integer> citations, int openerCitationsRemoved,
        int misplacedOpenersRemoved) {

    public FinalAnswer {
        citations = List.copyOf(citations);
    }
}
