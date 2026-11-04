package com.rasitbank.assistant.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.support.TestProperties;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Kaydedilecek RAG cevabının denetimini doğrular: bulamadım cevabı (sabit cümle ya da başka kelimelerle) tam cümleye
 * iner ve alıntısız kalır, giriş cümlesine bağlanan numaralar silinir, aralık dışı numaralar silinir, geçerli cevaba
 * dokunulmaz.
 */
class AnswerFinalizerTest {

    private static final String NOT_FOUND = TestProperties.NOT_FOUND_ANSWER;
    private static final int FIVE_CHUNKS = 5;

    private final AssistantProperties properties = TestProperties.defaults();
    private final AnswerFinalizer finalizer = new AnswerFinalizer(new NotFoundAnswerDetector(properties),
            new CitationParser(), properties);

    @Test
    void givenTextBeforeNotFoundSentence_whenFinalize_thenOnlySentenceWithoutCitations() {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer("Maalesef, bu konuda rehberimizde bilgi bulamadım. [2]",
                FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo(NOT_FOUND);
        assertThat(result.notFoundAnswer()).isTrue();
        assertThat(result.citations()).isEmpty();
    }

    @Test
    void givenNotFoundSentenceWithNumber_whenFinalize_thenSentenceWithoutNumber() {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer("Bu konuda rehberimizde bilgi bulamadım. [5]", FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo(NOT_FOUND);
        assertThat(result.notFoundAnswer()).isTrue();
        assertThat(result.citations()).isEmpty();
    }

    @Test
    void givenOutOfRangeMarker_whenFinalize_thenRemovedAndValidCitationKept() {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer("Kartını kapat [1]. Yenisi gelir [6].", FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo("Kartını kapat [1]. Yenisi gelir.");
        assertThat(result.notFoundAnswer()).isFalse();
        assertThat(result.citations()).containsExactly(1);
    }

    @Test
    void givenZeroMarker_whenFinalize_thenRemovedBecauseNumberingStartsAtOne() {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer("Kartını kapat [0].", FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo("Kartını kapat.");
        assertThat(result.citations()).isEmpty();
    }

    @Test
    void givenGroundedAnswer_whenFinalize_thenAnswerAndCitationsUnchanged() {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer("Bu kartın yıllık aidatı 750 TL'dir [1]. Muafiyet var [3].",
                FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo("Bu kartın yıllık aidatı 750 TL'dir [1]. Muafiyet var [3].");
        assertThat(result.notFoundAnswer()).isFalse();
        assertThat(result.citations()).containsExactly(1, 3);
        assertThat(result.openerCitationsRemoved()).isZero();
    }

    static Stream<Arguments> paraphrasedNotFound() {
        return Stream.of(
                Arguments.of("Diğer bankaların EFT ücretlerini rehberimizde bulamadım. [3]"),
                Arguments.of("Başka bankanın kartının avantajlarını rehberimizde bulamadım; Raşit Bankası'nın kendi "
                        + "kart özellikleri hakkında bilgi verebilirim. [1]"),
                Arguments.of("REHBERİMİZDE ARADIM AMA BULAMADIM."));
    }

    @ParameterizedTest
    @MethodSource("paraphrasedNotFound")
    void givenParaphrasedNotFoundWithCitation_whenFinalize_thenFixedSentenceWithoutCitations(String modelText) {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer(modelText, FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo(NOT_FOUND);
        assertThat(result.notFoundAnswer()).isTrue();
        assertThat(result.citations()).isEmpty();
    }

    static Stream<Arguments> openerAnswers() {
        return Stream.of(
                Arguments.of("Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) "
                                + "göremiyorum. [1]",
                        "Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) göremiyorum.",
                        List.of(), 1),
                Arguments.of("Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum.[4][2] Raşit "
                                + "Bankası'nda konut kredisi aylık %2,79'dan başlar [1].",
                        "Başka bankaların ürünleri ve koşulları hakkında bilgi veremiyorum. Raşit Bankası'nda konut "
                                + "kredisi aylık %2,79'dan başlar [1].",
                        List.of(1), 2),
                Arguments.of("Yatırım tavsiyesi ya da piyasa tahmini veremiyorum [5]. Altın hesabı Raşit "
                                + "Mobil'den açılır [1].",
                        "Yatırım tavsiyesi ya da piyasa tahmini veremiyorum. Altın hesabı Raşit Mobil'den açılır [1].",
                        List.of(1), 1),
                Arguments.of("YATIRIM TAVSİYESİ YA DA PİYASA TAHMİNİ VEREMİYORUM. [3] [2]",
                        "YATIRIM TAVSİYESİ YA DA PİYASA TAHMİNİ VEREMİYORUM.", List.of(), 2));
    }

    @ParameterizedTest
    @MethodSource("openerAnswers")
    void givenAnswerStartingWithOpenerAndCitedOpener_whenFinalize_thenOpenerCitationsRemovedAndRestKept(
            String modelText, String expectedAnswer, List<Integer> expectedCitations, int expectedRemoved) {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer(modelText, FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo(expectedAnswer);
        assertThat(result.notFoundAnswer()).isFalse();
        assertThat(result.citations()).isEqualTo(expectedCitations);
        assertThat(result.openerCitationsRemoved()).isEqualTo(expectedRemoved);
    }

    static Stream<Arguments> misplacedOpenerAnswers() {
        return Stream.of(
                Arguments.of("Ek kartın yıllık aidatı yoktur [1]. Hesabına özel bilgileri (bakiye, limit, borç, "
                        + "başvuru durumu, hesap hareketleri) göremiyorum. Kredi kartında aidat 750 TL'dir [2].",
                        "Ek kartın yıllık aidatı yoktur [1]. Kredi kartında aidat 750 TL'dir [2].", List.of(1, 2), 1),
                Arguments.of("Aidat 750 TL'dir [1]. Hesabınıza özel bilgileri (bakiye, limit, borç, başvuru durumu, "
                        + "hesap hareketleri) göremiyorum [3].", "Aidat 750 TL'dir [1].", List.of(1), 1),
                Arguments.of("Aidat 750 TL'dir [1]. Başka bankaların ürünleri ve koşulları hakkında bilgi "
                        + "veremiyorum.", "Aidat 750 TL'dir [1].", List.of(1), 1),
                Arguments.of("Altın hesabı Raşit Mobil'den açılır [1]! Yatırım tavsiyesi ya da piyasa tahmini "
                        + "veremiyorum. [2]", "Altın hesabı Raşit Mobil'den açılır [1]!", List.of(1), 1),
                Arguments.of("Altın hesabı açılır [2]. Yatırım tavsiyesi ya da piyasa tahmini veremiyorum. [9]",
                        "Altın hesabı açılır [2].", List.of(2), 1));
    }

    @ParameterizedTest
    @MethodSource("misplacedOpenerAnswers")
    void givenOpenerAfterFirstSentence_whenFinalize_thenOpenerSentenceRemovedAndCitationsRecomputed(String modelText,
            String expectedAnswer, List<Integer> expectedCitations, int expectedRemoved) {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer(modelText, FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo(expectedAnswer);
        assertThat(result.citations()).isEqualTo(expectedCitations);
        assertThat(result.misplacedOpenersRemoved()).isEqualTo(expectedRemoved);
        assertThat(result.notFoundAnswer()).isFalse();
    }

    static Stream<Arguments> keptAnswers() {
        return Stream.of(
                Arguments.of("Hesabına özel bilgileri (bakiye, limit, borç, başvuru durumu, hesap hareketleri) "
                        + "göremiyorum. Limitini Raşit Mobil'de Kartlarım > Limit işlemleri'nden artırabilirsin [1].",
                        List.of(1)),
                Arguments.of("Kartını kapat [1]. Bakiyeni Raşit Mobil'de görebilirsin [2].", List.of(1, 2)));
    }

    @ParameterizedTest
    @MethodSource("keptAnswers")
    void givenOpenerAsFirstSentenceOrSentenceMentioningBalance_whenFinalize_thenKept(String modelText,
            List<Integer> expectedCitations) {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer(modelText, FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo(modelText);
        assertThat(result.citations()).isEqualTo(expectedCitations);
        assertThat(result.misplacedOpenersRemoved()).isZero();
    }

    @Test
    void givenSingleWordOpenerInConfiguration_whenFinalize_thenNoSentenceRemoved() {
        // Arrange
        AssistantProperties custom = new AssistantProperties(42, null, properties.retrievalApi(),
                properties.retrieval(), properties.reranker(),
                new AssistantProperties.Generation("rag", "Yok.", java.time.Duration.ofSeconds(1),
                        java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(1), "Hayır.", "İkinci cümle.",
                        "Üçüncü cümle."),
                properties.queryRewrite(), properties.history(), properties.handoff(), properties.smallTalk());
        AnswerFinalizer singleWord = new AnswerFinalizer(new NotFoundAnswerDetector(custom), new CitationParser(),
                custom);

        // Act
        FinalAnswer result = singleWord.finalizeRagAnswer("Kartını kapat [1]. Yenisi gelir [2].", FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo("Kartını kapat [1]. Yenisi gelir [2].");
        assertThat(result.misplacedOpenersRemoved()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Kartını hemen kapat [1]. Yenisi gelir [2].", "Kartını kapat", ""})
    void givenNoOpener_whenFinalize_thenAnswerKept(String modelText) {
        // Act
        FinalAnswer result = finalizer.finalizeRagAnswer(modelText, FIVE_CHUNKS);

        // Assert
        assertThat(result.answer()).isEqualTo(modelText.strip());
        assertThat(result.notFoundAnswer()).isFalse();
        assertThat(result.openerCitationsRemoved()).isZero();
    }
}
