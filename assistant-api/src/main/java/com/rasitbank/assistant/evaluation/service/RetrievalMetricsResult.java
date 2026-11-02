package com.rasitbank.assistant.evaluation.service;

/**
 * Ölçülen soruların metriklerini taşır: isabet ve kaçan sayısı, recall@k ve MRR.
 */
public record RetrievalMetricsResult(int hitCount, int missedCount, double recallAtK, double mrr) {
}
