package com.rasitbank.retrieval.chunk.repository;

/**
 * Metin eşleştirme sorgusunun bir satırıdır: hangi sıradaki metnin hangi güncel parçada geçtiği.
 */
public interface ChunkTextMatchRow {

    long getOrdinality();

    long getChunkId();

    long getSourceId();

    String getSourceName();

    String getVersion();

    int getPageNumber();
}
