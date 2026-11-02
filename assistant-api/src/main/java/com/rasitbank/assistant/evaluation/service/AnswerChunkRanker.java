package com.rasitbank.assistant.evaluation.service;

import com.rasitbank.assistant.retrieval.service.RetrievedChunk;
import com.rasitbank.assistant.retrieval.service.RetrievedContext;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Retrieval sonucunda cevabın geçtiği parçalardan ilkinin vector sırasını ve prompt'taki yerini bulur.
 */
@Component
public class AnswerChunkRanker {

    public QuestionRank rank(RetrievedContext context, Set<Long> answerChunkIds) {
        Integer vectorRank = context.candidateChunks().stream()
                .filter(chunk -> answerChunkIds.contains(chunk.hit().chunkId()))
                .map(RetrievedChunk::vectorRank)
                .findFirst()
                .orElse(null);
        return new QuestionRank(vectorRank, promptPosition(context.contextChunks(), answerChunkIds));
    }

    private static @Nullable Integer promptPosition(List<RetrievedChunk> contextChunks, Set<Long> answerChunkIds) {
        for (int index = 0; index < contextChunks.size(); index++) {
            if (answerChunkIds.contains(contextChunks.get(index).hit().chunkId())) {
                return index + 1;
            }
        }
        return null;
    }
}
