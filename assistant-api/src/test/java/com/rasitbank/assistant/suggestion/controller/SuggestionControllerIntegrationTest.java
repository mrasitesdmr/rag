package com.rasitbank.assistant.suggestion.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Öneri listesinin gerçek PostgreSQL üzerinde tam değiştirildiğini, sıranın korunduğunu ve hatalı yüklemenin eski
 * listeyi bozmadığını doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SuggestionControllerIntegrationTest extends PostgresIntegrationTest {

    private static final String URI = "/api/assistant/suggestions";
    private static final String SAMPLE = """
            {"categories":[
              {"name":"Kartlar","questions":[
                {"text":"Kartımı kaybettim, ne yapmalıyım?","starter":true,"followUps":["Yenisi kaç günde gelir?","Kartımı nasıl dondururum?"]},
                {"text":"Kart aidatı ne kadar?"}]},
              {"name":"Krediler","questions":[{"text":"Asgari ödeme tutarı nedir?","starter":true}]}]}
            """;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM suggestion_categories");
    }

    private void put(String body) {
        assertThat(mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange()).hasStatus(200);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    @Test
    void givenFreshDatabase_whenGetSuggestions_thenEmptySet() {
        // Act
        var result = mvc.get().uri(URI).exchange();

        // Assert
        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.questionCount").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.followUpCount").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.categories").asList().isEmpty();
    }

    @Test
    void givenValidSet_whenReplaceThenGet_thenSameOrderAndCounts() {
        // Act
        var put = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content(SAMPLE).exchange();
        var get = mvc.get().uri(URI).exchange();

        // Assert
        for (var result : new org.springframework.test.web.servlet.assertj.MvcTestResult[] {put, get}) {
            assertThat(result).hasStatus(200);
            assertThat(result).bodyJson().extractingPath("$.questionCount").isEqualTo(3);
            assertThat(result).bodyJson().extractingPath("$.followUpCount").isEqualTo(2);
            assertThat(result).bodyJson().extractingPath("$.categories[0].name").isEqualTo("Kartlar");
            assertThat(result).bodyJson().extractingPath("$.categories[1].name").isEqualTo("Krediler");
            assertThat(result).bodyJson().extractingPath("$.categories[0].questions[0].starter").isEqualTo(true);
            assertThat(result).bodyJson().extractingPath("$.categories[0].questions[0].followUps").asList()
                    .containsExactly("Yenisi kaç günde gelir?", "Kartımı nasıl dondururum?");
            assertThat(result).bodyJson().extractingPath("$.categories[0].questions[1].text")
                    .isEqualTo("Kart aidatı ne kadar?");
            assertThat(result).bodyJson().extractingPath("$.categories[0].questions[1].starter").isEqualTo(false);
            assertThat(result).bodyJson().extractingPath("$.categories[1].questions[0].followUps").asList().isEmpty();
        }
    }

    @Test
    void givenExistingSet_whenReplaceWithAnother_thenOnlyNewSetRemains() {
        // Arrange
        put(SAMPLE);

        // Act
        put("{\"categories\":[{\"name\":\"Hesaplar\",\"questions\":[{\"text\":\"Hesap açmak için ne gerekir?\"}]}]}");
        var result = mvc.get().uri(URI).exchange();

        // Assert
        assertThat(result).bodyJson().extractingPath("$.categories").asList().hasSize(1);
        assertThat(result).bodyJson().extractingPath("$.categories[0].name").isEqualTo("Hesaplar");
        assertThat(count("suggestion_categories")).isEqualTo(1);
        assertThat(count("suggested_questions")).isEqualTo(1);
        assertThat(count("suggested_follow_ups")).isZero();
    }

    @Test
    void givenExistingSet_whenInvalidReplace_thenSetUnchanged() {
        // Arrange
        put(SAMPLE);

        // Act
        var bad = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categories\":[{\"name\":\"X\",\"questions\":[{\"text\":\"\"}]}]}").exchange();
        var get = mvc.get().uri(URI).exchange();

        // Assert
        assertThat(bad).hasStatus(400);
        assertThat(bad).bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_FAILED");
        assertThat(get).bodyJson().extractingPath("$.questionCount").isEqualTo(3);
        assertThat(get).bodyJson().extractingPath("$.categories[0].name").isEqualTo("Kartlar");
    }

    @Test
    void givenExistingSet_whenReplaceWithEmptyCategories_thenEmptySet() {
        // Arrange
        put(SAMPLE);

        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON).content("{\"categories\":[]}").exchange();

        // Assert
        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.questionCount").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.categories").asList().isEmpty();
        assertThat(count("suggested_questions")).isZero();
        assertThat(count("suggested_follow_ups")).isZero();
    }

    @Test
    void givenPaddedTextsAndOmittedFields_whenReplace_thenStrippedAndDefaults() {
        // Act
        var result = mvc.put().uri(URI).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categories\":[{\"name\":\"  Kartlar \",\"questions\":[{\"text\":\"  Kartımı kaybettim?  \","
                        + "\"followUps\":[\" Yenisi ne zaman? \"]}]}],\"extra\":1}")
                .exchange();

        // Assert
        assertThat(result).hasStatus(200);
        assertThat(result).bodyJson().extractingPath("$.categories[0].name").isEqualTo("Kartlar");
        assertThat(result).bodyJson().extractingPath("$.categories[0].questions[0].text").isEqualTo("Kartımı kaybettim?");
        assertThat(result).bodyJson().extractingPath("$.categories[0].questions[0].starter").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.categories[0].questions[0].followUps").asList()
                .containsExactly("Yenisi ne zaman?");
    }
}
