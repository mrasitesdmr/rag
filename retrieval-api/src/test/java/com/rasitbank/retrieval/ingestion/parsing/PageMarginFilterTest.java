package com.rasitbank.retrieval.ingestion.parsing;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.ingestionProperties;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tekrar eden sayfa üst/alt bilgisinin eşik, kenar başına en fazla iki satır ve kısa belge kurallarıyla atıldığını doğrular.
 */
class PageMarginFilterTest {

    private final PageMarginFilter filter = new PageMarginFilter(ingestionProperties());

    @Test
    void givenSameFirstLineAndNumberedLastLineOnAllFourPages_whenRemove_thenBothDroppedEverywhere() {
        // Arrange
        List<List<String>> pages = List.of(
                List.of("Atlaskap Lojistik · El Kitabı", "Gövde bir", "Sayfa 1 / 4"),
                List.of("Atlaskap Lojistik · El Kitabı", "Gövde iki", "Sayfa 2 / 4"),
                List.of("Atlaskap Lojistik · El Kitabı", "Gövde üç", "Sayfa 3 / 4"),
                List.of("Atlaskap Lojistik · El Kitabı", "Gövde dört", "Sayfa 4 / 4"));

        // Act
        List<List<String>> result = filter.removeRepeatedMargins(pages);

        // Assert
        assertThat(result).containsExactly(List.of("Gövde bir"), List.of("Gövde iki"), List.of("Gövde üç"),
                List.of("Gövde dört"));
    }

    @Test
    void givenTwoRepeatedLastLinesOnAllPages_whenRemove_thenBothDroppedButThirdRepeatedLineKept() {
        // Arrange
        List<List<String>> pages = List.of(
                page("Gövde bir", 1), page("Gövde iki", 2), page("Gövde üç", 3), page("Gövde dört", 4));

        // Act
        List<List<String>> result = filter.removeRepeatedMargins(pages);

        // Assert
        assertThat(result).containsExactly(
                List.of("Ortak üst not", "Gövde bir", "Ortak not"),
                List.of("Ortak üst not", "Gövde iki", "Ortak not"),
                List.of("Ortak üst not", "Gövde üç", "Ortak not"),
                List.of("Ortak üst not", "Gövde dört", "Ortak not"));
    }

    @Test
    void givenFirstLineOnTwoOfFourPages_whenRemove_thenDropped() {
        // Arrange
        List<List<String>> pages = List.of(
                List.of("Ortak başlık", "gövde a", "son alfa"),
                List.of("Ortak başlık", "gövde b", "son beta"),
                List.of("üst gama", "gövde c", "son gama"),
                List.of("üst delta", "gövde d", "son delta"));

        // Act
        List<List<String>> result = filter.removeRepeatedMargins(pages);

        // Assert
        assertThat(result.get(0)).containsExactly("gövde a", "son alfa");
        assertThat(result.get(1)).containsExactly("gövde b", "son beta");
        assertThat(result.get(2)).containsExactly("üst gama", "gövde c", "son gama");
    }

    @Test
    void givenFirstLineOnOneOfFourPages_whenRemove_thenKept() {
        // Arrange
        List<List<String>> pages = List.of(
                List.of("Tek başlık", "gövde a", "son alfa"),
                List.of("üst beta", "gövde b", "son beta"),
                List.of("üst gama", "gövde c", "son gama"),
                List.of("üst delta", "gövde d", "son delta"));

        // Act
        List<List<String>> result = filter.removeRepeatedMargins(pages);

        // Assert
        assertThat(result).isEqualTo(pages);
    }

    @Test
    void givenTwoPagesWithSameFirstLine_whenRemove_thenNothingDropped() {
        // Arrange
        List<List<String>> pages = List.of(List.of("Ortak başlık", "gövde a"), List.of("Ortak başlık", "gövde b"));

        // Act
        List<List<String>> result = filter.removeRepeatedMargins(pages);

        // Assert
        assertThat(result).isEqualTo(pages);
    }

    @Test
    void givenBlankPagesBetweenTextPages_whenRemove_thenPageCountAndOrderUnchangedAndOnlyTextPagesCounted() {
        // Arrange
        List<List<String>> pages = List.of(
                List.of("Ortak başlık", "gövde a"),
                List.of(),
                List.of("Ortak başlık", "gövde b"),
                List.of(),
                List.of("üst gama", "gövde c"));

        // Act
        List<List<String>> result = filter.removeRepeatedMargins(pages);

        // Assert
        assertThat(result).containsExactly(List.of("gövde a"), List.of(), List.of("gövde b"), List.of(),
                List.of("üst gama", "gövde c"));
    }

    @Test
    void givenLinesDifferingOnlyInDigitsAndSpaces_whenMarginKey_thenKeysEqual() {
        // Act
        String first = PageMarginFilter.marginKey("Yürürlük: 1 Ekim 2026 · Sayfa 7 / 70");
        String second = PageMarginFilter.marginKey("Yürürlük: 1 Ekim 2026 ·  Sayfa 12 / 70");

        // Assert
        assertThat(first).isEqualTo(second);
        assertThat(PageMarginFilter.marginKey("Sayfa 7")).isNotEqualTo(PageMarginFilter.marginKey("Bölüm 7"));
    }

    private static List<String> page(String body, int number) {
        return List.of("Üst bir " + number, "Üst iki " + number, "Ortak üst not", body, "Ortak not",
                "31.10.2026 18:0" + number + " RAG: Dil Modeline Kendi Verinle Cevap Verdirmek",
                "file:///x/rag.html " + number + "/4");
    }
}
