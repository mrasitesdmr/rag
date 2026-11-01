package com.rasitbank.retrieval.chunk.repository;

/**
 * Bir kaynak sürümünün parçalarının ortalama token sayısını taşıyan sorgu projeksiyonudur.
 */
public interface VersionTokenAverage {

    long getSourceVersionId();

    double getAverageTokenCount();
}
