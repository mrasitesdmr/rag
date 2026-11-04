package com.rasitbank.assistant.generation;

import java.util.List;

/**
 * Dil modelinin cevabının tura kaydedilecek son halini taşır: denetlenmiş metin, "bulamadım" işareti, geçerli alıntı
 * numaraları ve giriş cümlesinden silinen alıntı sayısı.
 */
public record FinalAnswer(String answer, boolean notFoundAnswer, List<Integer> citations, int openerCitationsRemoved) {

    public FinalAnswer {
        citations = List.copyOf(citations);
    }
}
