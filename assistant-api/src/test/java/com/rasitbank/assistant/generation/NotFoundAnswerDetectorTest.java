package com.rasitbank.assistant.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.support.TestProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Bulamadım tespitinin sabit cümleyi harf ve boşluk farkına rağmen bulduğunu, başka cevaplara dokunmadığını doğrular.
 */
class NotFoundAnswerDetectorTest {

    private final NotFoundAnswerDetector detector = new NotFoundAnswerDetector(TestProperties.defaults());

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "\"Bu konuda rehberimizde bilgi bulamadım.\" | true",
            "\"bu konuda REHBERİMİZDE  bilgi bulamadım. [1]\" | true",
            "\"Üzgünüm. Bu konuda rehberimizde bilgi bulamadım.\" | true",
            "\"Bu konuda\nrehberimizde bilgi   bulamadım.\" | true",
            "\"Kartını kapat [1].\" | false",
            "\"Bu konuda rehberimizde bilgi var.\" | false",
            "\"\" | false"
    })
    void givenAnswers_whenIsNotFoundAnswer_thenTrueOnlyWhenFixedSentenceIncluded(String answer, boolean expected) {
        assertThat(detector.isNotFoundAnswer(answer)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "\"Diğer bankaların EFT ücretlerini rehberimizde bulamadım. [3]\" | true",
            "\"Rehberimizde ARADIM ama BULAMADIM.\" | true",
            "\"Rehberimizde bulamadim\" | true",
            "\"Aidat rehberimizde 750 TL olarak geçiyor [1]. Başka bir ücret bulamadım.\" | false",
            "\"Bu bilgi rehberimizde yer alıyor [1].\" | false",
            "\"Kartı bulamadım diyorsan hemen kapat [1].\" | false"})
    void givenRehberimizdeAndBulamadimInSameOrDifferentSentencesOrAbsent_whenIsNotFoundAnswer_thenTrueOnlyInSameSentence(
            String answer, boolean expected) {
        assertThat(detector.isNotFoundAnswer(answer)).isEqualTo(expected);
    }

    @Test
    void givenDetector_whenNotFoundAnswer_thenConfiguredSentence() {
        assertThat(detector.notFoundAnswer()).isEqualTo(TestProperties.NOT_FOUND_ANSWER);
    }
}
