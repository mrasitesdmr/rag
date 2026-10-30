package com.rasitbank.retrieval.ingestion.chunking;

/**
 * Henüz embed edilmemiş bir parçadır: sıra no, sayfa no, başlık, içerik ve yaklaşık token sayısı.
 */
public record ChunkDraft(int chunkNo, int pageNumber, String heading, String content, int tokenCount) {

    public static final String HEADING_SEPARATOR = "\n\n";

    public String embeddingInput() {
        return heading + HEADING_SEPARATOR + content;
    }
}
