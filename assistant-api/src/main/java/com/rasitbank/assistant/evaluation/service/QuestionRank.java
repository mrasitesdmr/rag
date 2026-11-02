package com.rasitbank.assistant.evaluation.service;

import org.jspecify.annotations.Nullable;

/**
 * Bir sorunun doğru parçasının vector sırasını ve prompt'taki yerini taşır; parça oraya girmediyse boştur.
 */
public record QuestionRank(@Nullable Integer vectorRank, @Nullable Integer promptPosition) {
}
