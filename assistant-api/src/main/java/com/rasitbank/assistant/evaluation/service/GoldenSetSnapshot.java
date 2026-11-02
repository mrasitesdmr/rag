package com.rasitbank.assistant.evaluation.service;

import com.rasitbank.assistant.evaluation.domain.entity.GoldenQuestion;
import java.util.List;

/**
 * Ölçüm başlarken okunan golden set'i taşır: sürüm ve sorular golden set sırasıyla.
 */
public record GoldenSetSnapshot(String version, List<GoldenQuestion> questions) {

    public GoldenSetSnapshot {
        questions = List.copyOf(questions);
    }
}
