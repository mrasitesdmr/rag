package com.rasitbank.assistant.suggestion.service;

import com.rasitbank.assistant.suggestion.domain.dto.ReplaceSuggestionsRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestedQuestionRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestedQuestionResponse;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionCategoryRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionCategoryResponse;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionSetResponse;
import com.rasitbank.assistant.suggestion.domain.entity.SuggestedFollowUp;
import com.rasitbank.assistant.suggestion.domain.entity.SuggestedQuestion;
import com.rasitbank.assistant.suggestion.domain.entity.SuggestionCategory;
import com.rasitbank.assistant.suggestion.repository.SuggestedFollowUpRepository;
import com.rasitbank.assistant.suggestion.repository.SuggestedQuestionRepository;
import com.rasitbank.assistant.suggestion.repository.SuggestionCategoryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kayıtlı öneri listesini gösterim sırasıyla okur ve yüklenen listeyle tek transaction'da bütün olarak değiştirir.
 */
@Service
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);

    private final SuggestionCategoryRepository categoryRepository;
    private final SuggestedQuestionRepository questionRepository;
    private final SuggestedFollowUpRepository followUpRepository;

    public SuggestionService(SuggestionCategoryRepository categoryRepository,
            SuggestedQuestionRepository questionRepository, SuggestedFollowUpRepository followUpRepository) {
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
        this.followUpRepository = followUpRepository;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SuggestionSetResponse getSuggestions() {
        // Repeatable read gives the three queries one snapshot, so a replace committed in between cannot mix two lists.
        return toResponse(categoryRepository.findAllByOrderByPositionAsc(), questionRepository.findAllInDisplayOrder(),
                followUpRepository.findAllInDisplayOrder());
    }

    @Transactional
    public SuggestionSetResponse replaceSuggestions(ReplaceSuggestionsRequest request) {
        categoryRepository.lockForReplace();
        categoryRepository.deleteAllInBatch();
        List<SuggestionCategory> categories = new ArrayList<>();
        List<SuggestedQuestion> questions = new ArrayList<>();
        List<SuggestedFollowUp> followUps = new ArrayList<>();
        List<SuggestionCategoryRequest> categoryRequests = Objects.requireNonNull(request.categories());
        for (int categoryPosition = 0; categoryPosition < categoryRequests.size(); categoryPosition++) {
            SuggestionCategoryRequest categoryRequest = categoryRequests.get(categoryPosition);
            SuggestionCategory category = categoryRepository.save(
                    new SuggestionCategory(categoryPosition, Objects.requireNonNull(categoryRequest.name()).strip()));
            categories.add(category);
            List<SuggestedQuestionRequest> questionRequests = Objects.requireNonNull(categoryRequest.questions());
            for (int questionPosition = 0; questionPosition < questionRequests.size(); questionPosition++) {
                SuggestedQuestionRequest questionRequest = questionRequests.get(questionPosition);
                SuggestedQuestion question = questionRepository.save(new SuggestedQuestion(category, questionPosition,
                        Objects.requireNonNull(questionRequest.text()).strip(), questionRequest.isStarter()));
                questions.add(question);
                List<String> followUpTexts = questionRequest.followUpsOrEmpty();
                for (int followUpPosition = 0; followUpPosition < followUpTexts.size(); followUpPosition++) {
                    followUps.add(followUpRepository.save(
                            new SuggestedFollowUp(question, followUpPosition, followUpTexts.get(followUpPosition).strip())));
                }
            }
        }
        log.info("Suggestions replaced categories={} questions={} followUps={}",
                categories.size(), questions.size(), followUps.size());
        return toResponse(categories, questions, followUps);
    }

    private static SuggestionSetResponse toResponse(List<SuggestionCategory> categories,
            List<SuggestedQuestion> questions, List<SuggestedFollowUp> followUps) {
        Map<Long, List<String>> followUpsByQuestionId = new HashMap<>();
        for (SuggestedFollowUp followUp : followUps) {
            followUpsByQuestionId.computeIfAbsent(idOf(followUp.getQuestion().getId()), id -> new ArrayList<>())
                    .add(followUp.getText());
        }
        Map<Long, List<SuggestedQuestionResponse>> questionsByCategoryId = new HashMap<>();
        for (SuggestedQuestion question : questions) {
            List<String> questionFollowUps = followUpsByQuestionId.getOrDefault(idOf(question.getId()), List.of());
            questionsByCategoryId.computeIfAbsent(idOf(question.getCategory().getId()), id -> new ArrayList<>())
                    .add(new SuggestedQuestionResponse(question.getText(), question.isStarter(), questionFollowUps));
        }
        List<SuggestionCategoryResponse> categoryResponses = categories.stream()
                .map(category -> new SuggestionCategoryResponse(category.getName(),
                        questionsByCategoryId.getOrDefault(idOf(category.getId()), List.of())))
                .toList();
        return new SuggestionSetResponse(questions.size(), followUps.size(), categoryResponses);
    }

    private static long idOf(@Nullable Long id) {
        return Objects.requireNonNull(id);
    }
}
