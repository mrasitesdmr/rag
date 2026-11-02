package com.rasitbank.assistant.evaluation.domain.dto;

import com.rasitbank.assistant.evaluation.domain.entity.EvalRunQuestion;
import org.jspecify.annotations.Nullable;

/**
 * "Kaçanlar ve sırası değişenler" tablosunun bir satırını taşır: soru, doğru parçanın vector sırası ve prompt'taki yeri.
 */
public record EvalQuestionResultResponse(int position, String category, String text, @Nullable Integer vectorRank,
        @Nullable Integer promptPosition) {

    public static EvalQuestionResultResponse from(EvalRunQuestion question) {
        return new EvalQuestionResultResponse(question.getPosition(), question.getCategory(), question.getText(),
                question.getVectorRank(), question.getPromptPosition());
    }
}
