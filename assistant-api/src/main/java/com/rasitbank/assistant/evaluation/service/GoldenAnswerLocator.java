package com.rasitbank.assistant.evaluation.service;

import com.rasitbank.assistant.evaluation.domain.entity.GoldenQuestion;
import com.rasitbank.assistant.evaluation.repository.GoldenQuestionRepository;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.retrieval.client.ChunkMatchesResponse;
import com.rasitbank.assistant.retrieval.client.ChunkTextMatch;
import com.rasitbank.assistant.retrieval.client.MatchedChunk;
import com.rasitbank.assistant.retrieval.client.RetrievalClient;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Golden set'te bulunan soruların cevap cümlesini içeren güncel parçaları bulur; soru normalize tam metinle eşleşir.
 * retrieval-api okunamazsa işaret vermez, hata da fırlatmaz. Çağıranın transaction'ı olmamalıdır.
 */
@Service
public class GoldenAnswerLocator {

    private static final Logger log = LoggerFactory.getLogger(GoldenAnswerLocator.class);

    private final GoldenQuestionRepository goldenQuestionRepository;
    private final RetrievalClient retrievalClient;

    public GoldenAnswerLocator(GoldenQuestionRepository goldenQuestionRepository, RetrievalClient retrievalClient) {
        this.goldenQuestionRepository = goldenQuestionRepository;
        this.retrievalClient = retrievalClient;
    }

    public Map<String, Set<Long>> locateAnswerChunks(Collection<String> questions) {
        Set<String> normalizedQuestions = questions.stream()
                .map(SuggestionTextNormalizer::normalize)
                .filter(question -> !question.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedQuestions.isEmpty()) {
            return Map.of();
        }
        List<GoldenQuestion> goldenQuestions = goldenQuestionRepository.findByNormalizedTextIn(normalizedQuestions);
        if (goldenQuestions.isEmpty()) {
            return Map.of();
        }
        ChunkMatchesResponse response;
        try {
            response = retrievalClient.findChunkMatches(
                    goldenQuestions.stream().map(GoldenQuestion::getAnswerText).toList());
        } catch (ServiceUnavailableException exception) {
            log.warn("Golden answer lookup skipped reason={}", exception.getErrorCode());
            return Map.of();
        }
        List<ChunkTextMatch> matches = response.matchesOrEmpty();
        Map<String, Set<Long>> answerChunks = new HashMap<>();
        for (int index = 0; index < goldenQuestions.size() && index < matches.size(); index++) {
            Set<Long> chunkIds = matches.get(index).chunksOrEmpty().stream()
                    .map(MatchedChunk::chunkId)
                    .collect(Collectors.toUnmodifiableSet());
            if (!chunkIds.isEmpty()) {
                answerChunks.put(goldenQuestions.get(index).getNormalizedText(), chunkIds);
            }
        }
        return Map.copyOf(answerChunks);
    }
}
