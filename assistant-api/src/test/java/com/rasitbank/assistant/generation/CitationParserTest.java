package com.rasitbank.assistant.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Alıntı ayrıştırmanın aralık, tekrar ve sıra kurallarını doğrular.
 */
class CitationParserTest {

    private final CitationParser parser = new CitationParser();

    @Test
    void givenAnswerWithRepeatedAndOutOfRangeNumbers_whenParseCitations_thenInRangeUniqueInFirstAppearanceOrder() {
        var citations = parser.parseCitations("A [2]. B [1]. C [2] [0] [6] [12]", 5);

        assertThat(citations).containsExactly(2, 1);
    }

    @Test
    void givenThreeContextChunks_whenParseCitations_thenFourIsIgnored() {
        var citations = parser.parseCitations("x [4] y [3]", 3);

        assertThat(citations).containsExactly(3);
    }

    @Test
    void givenAnswerWithoutBrackets_whenParseCitations_thenEmpty() {
        assertThat(parser.parseCitations("Kartını kapat.", 5)).isEmpty();
    }

    @Test
    void givenNumberBeyondIntRange_whenParseCitations_thenIgnoredWithoutError() {
        assertThat(parser.parseCitations("a [99999999999999] b [1]", 5)).containsExactly(1);
    }

    @Test
    void givenNonNumericBrackets_whenParseCitations_thenIgnored() {
        assertThat(parser.parseCitations("a [x] [1a] [ 2 ] [-1] [3]", 5)).containsExactly(3);
    }

    @Test
    void givenEmptyContext_whenParseCitations_thenNoCitation() {
        assertThat(parser.parseCitations("a [1]", 0)).isEmpty();
    }

    @Test
    void givenNumberEqualToContextSize_whenParseCitations_thenIncluded() {
        assertThat(parser.parseCitations("a [5]", 5)).containsExactly(5);
    }
}
