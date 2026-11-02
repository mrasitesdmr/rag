package com.rasitbank.assistant.evaluation.service;

import com.rasitbank.assistant.evaluation.domain.dto.GoldenQuestionRequest;
import com.rasitbank.assistant.evaluation.domain.dto.GoldenSetSummaryResponse;
import com.rasitbank.assistant.evaluation.domain.dto.ReplaceGoldenSetRequest;
import com.rasitbank.assistant.evaluation.domain.entity.GoldenQuestion;
import com.rasitbank.assistant.evaluation.domain.entity.GoldenSet;
import com.rasitbank.assistant.evaluation.repository.GoldenQuestionRepository;
import com.rasitbank.assistant.evaluation.repository.GoldenSetRepository;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.NotFoundException;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Golden set'i okur ve ürün ekibinin yüklediği setle tek transaction'da bütün olarak değiştirir; aynı sürüm adıyla
 * farklı içeriği reddeder.
 */
@Service
public class GoldenSetService {

    private static final Logger log = LoggerFactory.getLogger(GoldenSetService.class);

    private static final char FIELD_SEPARATOR = '\u001F';
    private static final char QUESTION_SEPARATOR = '\u001E';

    private final GoldenSetRepository goldenSetRepository;
    private final GoldenQuestionRepository goldenQuestionRepository;
    private final Clock clock;

    public GoldenSetService(GoldenSetRepository goldenSetRepository, GoldenQuestionRepository goldenQuestionRepository,
            Clock clock) {
        this.goldenSetRepository = goldenSetRepository;
        this.goldenQuestionRepository = goldenQuestionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public GoldenSetSummaryResponse getGoldenSet() {
        return findGoldenSet().orElseThrow(() -> new NotFoundException("GOLDEN_SET_NOT_LOADED",
                "No golden set has been loaded"));
    }

    @Transactional(readOnly = true)
    public Optional<GoldenSetSummaryResponse> findGoldenSet() {
        return goldenSetRepository.findFirstByOrderByIdDesc()
                .map(goldenSet -> GoldenSetSummaryResponse.from(goldenSet,
                        goldenQuestionRepository.countByGoldenSetId(Objects.requireNonNull(goldenSet.getId()))));
    }

    @Transactional(readOnly = true)
    public Optional<GoldenSetSnapshot> findGoldenSetSnapshot() {
        return goldenSetRepository.findFirstByOrderByIdDesc()
                .map(goldenSet -> new GoldenSetSnapshot(goldenSet.getVersion(),
                        goldenQuestionRepository.findByGoldenSetIdOrderByPosition(
                                Objects.requireNonNull(goldenSet.getId()))));
    }

    @Transactional
    public GoldenSetSummaryResponse replaceGoldenSet(ReplaceGoldenSetRequest request) {
        goldenSetRepository.lockForReplace();
        String version = Objects.requireNonNull(request.version()).strip();
        List<GoldenQuestionRequest> questionRequests = Objects.requireNonNull(request.questions());
        String contentHash = contentHash(questionRequests);
        Optional<GoldenSet> current = goldenSetRepository.findFirstByOrderByIdDesc();
        if (current.isPresent() && current.get().getVersion().equals(version)
                && !current.get().getContentHash().equals(contentHash)) {
            throw new ConflictException("GOLDEN_SET_VERSION_CONFLICT",
                    "A golden set with this version and a different content is stored: " + version);
        }
        goldenSetRepository.deleteAllInBatch();
        GoldenSet goldenSet = goldenSetRepository.save(new GoldenSet(version, contentHash, Instant.now(clock)));
        long goldenSetId = Objects.requireNonNull(goldenSet.getId());
        List<GoldenQuestion> questions = new ArrayList<>(questionRequests.size());
        for (int index = 0; index < questionRequests.size(); index++) {
            GoldenQuestionRequest questionRequest = questionRequests.get(index);
            String text = Objects.requireNonNull(questionRequest.text()).strip();
            questions.add(new GoldenQuestion(goldenSetId, index + 1,
                    Objects.requireNonNull(questionRequest.category()).strip(), text,
                    SuggestionTextNormalizer.normalize(text), Objects.requireNonNull(questionRequest.pageNumber()),
                    Objects.requireNonNull(questionRequest.answerText()).strip()));
        }
        goldenQuestionRepository.saveAll(questions);
        log.info("Golden set replaced version={} questionCount={}", version, questions.size());
        return GoldenSetSummaryResponse.from(goldenSet, questions.size());
    }

    static String contentHash(List<GoldenQuestionRequest> questions) {
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < questions.size(); index++) {
            GoldenQuestionRequest question = questions.get(index);
            if (index > 0) {
                content.append(QUESTION_SEPARATOR);
            }
            content.append(Objects.requireNonNull(question.category()).strip()).append(FIELD_SEPARATOR)
                    .append(Objects.requireNonNull(question.text()).strip()).append(FIELD_SEPARATOR)
                    .append(Objects.requireNonNull(question.pageNumber())).append(FIELD_SEPARATOR)
                    .append(Objects.requireNonNull(question.answerText()).strip());
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
