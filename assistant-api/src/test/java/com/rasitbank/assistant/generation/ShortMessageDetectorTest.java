package com.rasitbank.assistant.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Kısa mesaj tanımını (soru işareti yoksa en çok 10, varsa en çok 5 kelime) ve harf ya da rakam içermeyen mesajı
 * doğrular.
 */
class ShortMessageDetectorTest {

    private final ShortMessageDetector detector = new ShortMessageDetector();

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Selam | true",
            "?bir iki üç dört beş altı | false",
            "Nasılsın? | true",
            "Merhaba, iyi misin? | true",
            "Bir iki üç dört beş? | true",
            "Bir iki üç dört beş altı? | false",
            "Bir iki üç dört beş altı yedi sekiz dokuz on | true",
            "Bir iki üç dört beş altı yedi sekiz dokuz on onbir | false",
            "Kredi limitim ne kadar | true",
            "Gold kart aidatı ne kadar? | true",
            "Ücret değişiklikleri bana ne kadar önceden bildiriliyor? | false",
            "???  | false",
            "🙂 !! | false",
            "'   ' | false"})
    void givenMessages_whenIsShortMessage_thenTenWordsWithoutQuestionMarkOrFiveWithIt(String message,
            boolean expected) {
        // Act
        boolean isShort = detector.isShortMessage(message);

        // Assert
        assertThat(isShort).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "??? | true",
            "?! | true",
            "🙂 | true",
            "... !! | true",
            "a? | false",
            "2? | false",
            "Selam | false"})
    void givenMessages_whenHasNoWords_thenTrueOnlyWithoutLettersOrDigits(String message, boolean expected) {
        // Act
        boolean noWords = detector.hasNoWords(message);

        // Assert
        assertThat(noWords).isEqualTo(expected);
    }
}
