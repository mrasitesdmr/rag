package com.rasitbank.assistant.suggestion.domain.validation;

import com.rasitbank.assistant.suggestion.domain.dto.ReplaceSuggestionsRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestedQuestionRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionCategoryRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Yüklenen öneri listesinde toplam soru sınırını ve normalize edilmiş metinlerin benzersizliğini denetler; her ihlali
 * kendi alan yoluyla bildirir.
 */
public class SuggestionSetValidator implements ConstraintValidator<ValidSuggestionSet, ReplaceSuggestionsRequest> {

    private static final String CATEGORIES = "categories";
    private static final String MUST_NOT_BE_BLANK = "must not be blank";
    private static final String MUST_BE_UNIQUE = "must be unique";
    private static final String MUST_DIFFER_FROM_QUESTION = "must differ from the question";
    private static final String TOTAL_QUESTIONS_EXCEEDED = "total questions must be at most "
            + ReplaceSuggestionsRequest.QUESTIONS_MAX_TOTAL;

    @Override
    public boolean isValid(@Nullable ReplaceSuggestionsRequest request, ConstraintValidatorContext context) {
        if (request == null || request.categories() == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        List<@Nullable SuggestionCategoryRequest> categories = request.categories();
        boolean isValid = validateTotalQuestions(categories, context);
        isValid &= validateCategoryNames(categories, context);
        isValid &= validateQuestions(categories, context);
        return isValid;
    }

    private static boolean validateTotalQuestions(List<@Nullable SuggestionCategoryRequest> categories,
            ConstraintValidatorContext context) {
        int totalQuestions = 0;
        for (SuggestionCategoryRequest category : categories) {
            if (category != null && category.questions() != null) {
                totalQuestions += category.questions().size();
            }
        }
        if (totalQuestions <= ReplaceSuggestionsRequest.QUESTIONS_MAX_TOTAL) {
            return true;
        }
        context.buildConstraintViolationWithTemplate(TOTAL_QUESTIONS_EXCEEDED)
                .addPropertyNode(CATEGORIES)
                .addConstraintViolation();
        return false;
    }

    private static boolean validateCategoryNames(List<@Nullable SuggestionCategoryRequest> categories,
            ConstraintValidatorContext context) {
        boolean isValid = true;
        Set<String> seenNames = new HashSet<>();
        for (int categoryIndex = 0; categoryIndex < categories.size(); categoryIndex++) {
            SuggestionCategoryRequest category = categories.get(categoryIndex);
            if (category == null) {
                continue;
            }
            String message = textViolation(category.name(), seenNames, null);
            if (message != null) {
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(CATEGORIES)
                        .addPropertyNode("name").inIterable().atIndex(categoryIndex)
                        .addConstraintViolation();
                isValid = false;
            }
        }
        return isValid;
    }

    private static boolean validateQuestions(List<@Nullable SuggestionCategoryRequest> categories,
            ConstraintValidatorContext context) {
        boolean isValid = true;
        Set<String> seenQuestions = new HashSet<>();
        for (int categoryIndex = 0; categoryIndex < categories.size(); categoryIndex++) {
            SuggestionCategoryRequest category = categories.get(categoryIndex);
            if (category == null || category.questions() == null) {
                continue;
            }
            List<@Nullable SuggestedQuestionRequest> questions = category.questions();
            for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
                SuggestedQuestionRequest question = questions.get(questionIndex);
                if (question == null) {
                    continue;
                }
                String message = textViolation(question.text(), seenQuestions, null);
                if (message != null) {
                    context.buildConstraintViolationWithTemplate(message)
                            .addPropertyNode(CATEGORIES)
                            .addPropertyNode("questions").inIterable().atIndex(categoryIndex)
                            .addPropertyNode("text").inIterable().atIndex(questionIndex)
                            .addConstraintViolation();
                    isValid = false;
                }
                isValid &= validateFollowUps(question, categoryIndex, questionIndex, context);
            }
        }
        return isValid;
    }

    private static boolean validateFollowUps(SuggestedQuestionRequest question, int categoryIndex, int questionIndex,
            ConstraintValidatorContext context) {
        if (question.followUps() == null) {
            return true;
        }
        String normalizedQuestion = isBlank(question.text()) ? null : SuggestionTextNormalizer.normalize(question.text());
        boolean isValid = true;
        Set<String> seenFollowUps = new HashSet<>();
        List<@Nullable String> followUps = question.followUps();
        for (int followUpIndex = 0; followUpIndex < followUps.size(); followUpIndex++) {
            String message = textViolation(followUps.get(followUpIndex), seenFollowUps, normalizedQuestion);
            if (message != null) {
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(CATEGORIES)
                        .addPropertyNode("questions").inIterable().atIndex(categoryIndex)
                        .addPropertyNode("followUps").inIterable().atIndex(questionIndex)
                        .addBeanNode().inIterable().atIndex(followUpIndex)
                        .addConstraintViolation();
                isValid = false;
            }
        }
        return isValid;
    }

    private static @Nullable String textViolation(@Nullable String text, Set<String> seenTexts,
            @Nullable String forbiddenText) {
        if (isBlankOnlyAfterStrip(text)) {
            return MUST_NOT_BE_BLANK;
        }
        if (isBlank(text)) {
            return null;
        }
        String normalizedText = SuggestionTextNormalizer.normalize(text);
        if (normalizedText.equals(forbiddenText)) {
            return MUST_DIFFER_FROM_QUESTION;
        }
        return seenTexts.add(normalizedText) ? null : MUST_BE_UNIQUE;
    }

    private static boolean isBlank(@Nullable String text) {
        return text == null || text.isBlank();
    }

    private static boolean isBlankOnlyAfterStrip(@Nullable String text) {
        // @NotBlank trims only ASCII control and space characters, strip() before storing also removes Unicode spaces.
        return text != null && text.isBlank() && !text.trim().isEmpty();
    }
}
