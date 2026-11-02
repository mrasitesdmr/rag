package com.rasitbank.assistant.suggestion.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.suggestion.domain.dto.ReplaceSuggestionsRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestedQuestionRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionCategoryRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionSetResponse;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aynı anda gelen iki liste yüklemesinden sonra kayıtlı listenin gönderilen listelerden tam biri olduğunu doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SuggestionServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private JdbcTemplate jdbc;

    private static ReplaceSuggestionsRequest request(String category, int questions) {
        List<SuggestedQuestionRequest> list = new java.util.ArrayList<>();
        for (int i = 0; i < questions; i++) {
            list.add(new SuggestedQuestionRequest(category + " sorusu " + i, false, List.of()));
        }
        return new ReplaceSuggestionsRequest(List.of(new SuggestionCategoryRequest(category, list)));
    }

    @RepeatedTest(3)
    void givenTwoConcurrentReplaces_whenBothFinish_thenSetIsExactlyOneOfThem() throws Exception {
        // Arrange
        jdbc.update("DELETE FROM suggestion_categories");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<SuggestionSetResponse> first = pool.submit(() -> {
                start.await();
                return suggestionService.replaceSuggestions(request("Kartlar", 3));
            });
            Future<SuggestionSetResponse> second = pool.submit(() -> {
                start.await();
                return suggestionService.replaceSuggestions(request("Hesaplar", 2));
            });

            // Act
            start.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
            SuggestionSetResponse result = suggestionService.getSuggestions();

            // Assert
            assertThat(result.categories()).hasSize(1);
            String name = result.categories().get(0).name();
            assertThat(name).isIn("Kartlar", "Hesaplar");
            assertThat(result.questionCount()).isEqualTo(name.equals("Kartlar") ? 3 : 2);
            assertThat(result.categories().get(0).questions()).hasSize(result.questionCount());
        } finally {
            pool.shutdownNow();
        }
    }
}
