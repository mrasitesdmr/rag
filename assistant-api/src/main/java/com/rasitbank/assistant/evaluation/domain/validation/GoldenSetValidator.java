package com.rasitbank.assistant.evaluation.domain.validation;

import com.rasitbank.assistant.evaluation.domain.dto.GoldenQuestionRequest;
import com.rasitbank.assistant.evaluation.domain.dto.ReplaceGoldenSetRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Yüklenen golden set'te kırpılmış cevap cümlelerinin uzunluğunu ve normalize soru metinlerinin benzersizliğini
 * denetler; her ihlali kendi alan yoluyla bildirir.
 */
public class GoldenSetValidator implements ConstraintValidator<ValidGoldenSet, ReplaceGoldenSetRequest> {

    private static final String QUESTIONS = "questions";
    private static final String MUST_NOT_BE_BLANK = "must not be blank";
    private static final String MUST_BE_UNIQUE = "must be unique";
    private static final String ANSWER_TEXT_SIZE = "size must be between "
            + GoldenQuestionRequest.ANSWER_TEXT_MIN_LENGTH + " and " + GoldenQuestionRequest.ANSWER_TEXT_MAX_LENGTH
            + " characters";

    @Override
    public boolean isValid(@Nullable ReplaceGoldenSetRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean isValid = true;
        if (isBlankOnlyAfterStrip(request.version())) {
            context.buildConstraintViolationWithTemplate(MUST_NOT_BE_BLANK)
                    .addPropertyNode("version")
                    .addConstraintViolation();
            isValid = false;
        }
        if (request.questions() != null) {
            isValid &= validateQuestions(request.questions(), context);
        }
        return isValid;
    }

    private static boolean validateQuestions(List<@Nullable GoldenQuestionRequest> questions,
            ConstraintValidatorContext context) {
        boolean isValid = true;
        Set<String> seenTexts = new HashSet<>();
        for (int index = 0; index < questions.size(); index++) {
            GoldenQuestionRequest question = questions.get(index);
            if (question == null) {
                continue;
            }
            if (isBlankOnlyAfterStrip(question.category())) {
                addViolation(context, MUST_NOT_BE_BLANK, index, "category");
                isValid = false;
            }
            String textViolation = textViolation(question.text(), seenTexts);
            if (textViolation != null) {
                addViolation(context, textViolation, index, "text");
                isValid = false;
            }
            String answerTextViolation = answerTextViolation(question.answerText());
            if (answerTextViolation != null) {
                addViolation(context, answerTextViolation, index, "answerText");
                isValid = false;
            }
        }
        return isValid;
    }

    private static @Nullable String textViolation(@Nullable String text, Set<String> seenTexts) {
        if (isBlankOnlyAfterStrip(text)) {
            return MUST_NOT_BE_BLANK;
        }
        if (text == null || text.isBlank()) {
            return null;
        }
        return seenTexts.add(SuggestionTextNormalizer.normalize(text)) ? null : MUST_BE_UNIQUE;
    }

    private static @Nullable String answerTextViolation(@Nullable String answerText) {
        if (isBlankOnlyAfterStrip(answerText)) {
            return MUST_NOT_BE_BLANK;
        }
        if (answerText == null || answerText.isBlank()) {
            return null;
        }
        int length = answerText.strip().length();
        boolean isInRange = length >= GoldenQuestionRequest.ANSWER_TEXT_MIN_LENGTH
                && length <= GoldenQuestionRequest.ANSWER_TEXT_MAX_LENGTH;
        return isInRange ? null : ANSWER_TEXT_SIZE;
    }

    private static void addViolation(ConstraintValidatorContext context, String message, int index, String field) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(QUESTIONS)
                .addPropertyNode(field).inIterable().atIndex(index)
                .addConstraintViolation();
    }

    private static boolean isBlankOnlyAfterStrip(@Nullable String text) {
        // @NotBlank trims only ASCII control and space characters, strip() before storing also removes Unicode spaces.
        return text != null && text.isBlank() && !text.trim().isEmpty();
    }
}
