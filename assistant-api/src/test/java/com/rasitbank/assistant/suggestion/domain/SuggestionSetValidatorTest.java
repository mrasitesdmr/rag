package com.rasitbank.assistant.suggestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.suggestion.domain.dto.ReplaceSuggestionsRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestedQuestionRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionCategoryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Listeler arası kuralları (normalize benzersizlik, takip kuralları, toplam 500 sınırı) alan yollarıyla doğrular.
 */
class SuggestionSetValidatorTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static SuggestedQuestionRequest question(String text, String... followUps) {
        return new SuggestedQuestionRequest(text, false, List.of(followUps));
    }

    private static SuggestionCategoryRequest category(String name, SuggestedQuestionRequest... questions) {
        return new SuggestionCategoryRequest(name, List.of(questions));
    }

    private static Set<String> violations(ReplaceSuggestionsRequest request) {
        Set<ConstraintViolation<ReplaceSuggestionsRequest>> found = VALIDATOR.validate(request);
        return found.stream().map(v -> v.getPropertyPath() + " | " + v.getMessage()).collect(Collectors.toSet());
    }

    @Test
    void givenQuestionDuplicatedAcrossCategoriesAfterNormalization_whenValidate_thenLaterOneReported() {
        // Arrange
        var request = new ReplaceSuggestionsRequest(List.of(
                category("Kartlar", question("Kartımı kaybettim?")),
                category("Hesaplar", question("kartımı  KAYBETTİM"))));

        // Act
        Set<String> result = violations(request);

        // Assert
        assertThat(result).containsExactly("categories[1].questions[0].text | must be unique");
    }

    @Test
    void givenCategoryNamesDifferingInCase_whenValidate_thenLaterNameReported() {
        // Arrange
        var request = new ReplaceSuggestionsRequest(List.of(
                category("Kartlar", question("A soru")), category("kartlar", question("B soru"))));

        // Act
        Set<String> result = violations(request);

        // Assert
        assertThat(result).containsExactly("categories[1].name | must be unique");
    }

    @Test
    void givenDuplicateFollowUpAndFollowUpEqualToQuestion_whenValidate_thenBothReported() {
        // Arrange
        var request = new ReplaceSuggestionsRequest(List.of(
                category("Kartlar", question("Kartımı kaybettim?", "Kartımı kaybettim!", "Yenisi ne zaman gelir?",
                        "yenisi ne zaman gelir"))));

        // Act
        Set<String> result = violations(request);

        // Assert
        assertThat(result).containsExactlyInAnyOrder(
                "categories[0].questions[0].followUps[0] | must differ from the question",
                "categories[0].questions[0].followUps[2] | must be unique");
    }

    @Test
    void givenUnicodeSpaceOnlyText_whenValidate_thenMustNotBeBlank() {
        // Arrange
        var request = new ReplaceSuggestionsRequest(List.of(category("Kartlar", question(" "))));

        // Act
        Set<String> result = violations(request);

        // Assert
        assertThat(result).containsExactly("categories[0].questions[0].text | must not be blank");
    }

    @Test
    void givenAsciiBlankText_whenValidate_thenReportedExactlyOnce() {
        // Arrange
        var request = new ReplaceSuggestionsRequest(List.of(category("Kartlar", question(" "))));

        // Act
        List<String> result = VALIDATOR.validate(request).stream()
                .map(v -> v.getPropertyPath() + " | " + v.getMessage()).toList();

        // Assert
        assertThat(result).containsExactly("categories[0].questions[0].text | must not be blank");
    }

    @Test
    void givenNullNameAndNullText_whenValidate_thenOnlyFieldConstraintsReported() {
        // Arrange
        var request = new ReplaceSuggestionsRequest(List.of(
                new SuggestionCategoryRequest(null, List.of(new SuggestedQuestionRequest(null, null, null)))));

        // Act
        Set<String> result = violations(request);

        // Assert
        assertThat(result).containsExactlyInAnyOrder("categories[0].name | must not be blank",
                "categories[0].questions[0].text | must not be blank");
    }

    @Test
    void givenFiveHundredOneQuestions_whenValidate_thenTotalViolation() {
        // Arrange
        var request = new ReplaceSuggestionsRequest(setOf(550, 50));

        // Act
        Set<String> result = violations(request);

        // Assert
        assertThat(result).containsExactly("categories | total questions must be at most 500");
    }

    @Test
    void givenExactlyFiveHundredQuestions_whenValidate_thenNoViolation() {
        // Arrange
        var request = new ReplaceSuggestionsRequest(setOf(500, 50));

        // Act
        Set<String> result = violations(request);

        // Assert
        assertThat(result).isEmpty();
    }

    private static List<SuggestionCategoryRequest> setOf(int totalQuestions, int perCategory) {
        List<SuggestionCategoryRequest> categories = new ArrayList<>();
        int created = 0;
        int categoryNo = 0;
        while (created < totalQuestions) {
            List<SuggestedQuestionRequest> questions = new ArrayList<>();
            for (int i = 0; i < perCategory && created < totalQuestions; i++, created++) {
                questions.add(question("Soru numarası " + created));
            }
            categories.add(new SuggestionCategoryRequest("Kategori " + categoryNo++, questions));
        }
        return categories;
    }
}
