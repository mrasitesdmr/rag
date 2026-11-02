package com.rasitbank.assistant.suggestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Normalizasyonun web ile aynı sonucu verdiğini (Türkçe küçük harf, noktalama, boşluk) doğrular.
 */
class SuggestionTextNormalizerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "  KARTIMI  kaybettim, ne yapmalıyım?|kartımı kaybettim ne yapmalıyım",
            "İŞLEM|işlem",
            "ISPARTA|ısparta",
            "Kart-aidatı!!|kart aidatı",
            "Havale 2 saat?|havale 2 saat"})
    void givenTurkishAndPunctuation_whenNormalize_thenLabEquivalent(String input, String expected) {
        // Act
        String result = SuggestionTextNormalizer.normalize(input);

        // Assert
        assertThat(result).isEqualTo(expected);
    }
}
