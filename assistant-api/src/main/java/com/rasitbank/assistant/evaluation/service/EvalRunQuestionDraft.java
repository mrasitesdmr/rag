package com.rasitbank.assistant.evaluation.service;

import java.util.List;

/**
 * Çalıştırma başlarken kopyalanacak bir golden soruyu ve cevabının geçtiği parça kimliklerini taşır.
 */
public record EvalRunQuestionDraft(int position, String category, String text, int pageNumber, String answerText,
        List<Long> answerChunkIds) {

    public EvalRunQuestionDraft {
        answerChunkIds = List.copyOf(answerChunkIds);
    }

    public boolean isMeasured() {
        return !answerChunkIds.isEmpty();
    }
}
