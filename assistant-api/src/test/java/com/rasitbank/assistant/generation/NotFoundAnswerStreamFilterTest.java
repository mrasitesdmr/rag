package com.rasitbank.assistant.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.support.TestProperties;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Akış filtresinin "bulamadım" cümlesinin başını tuttuğunu, cümle tamamlanınca cümleyi tek parça verip sonrasını
 * (alıntı numarası dahil) bıraktığını, ayrışan metni olduğu gibi geçirdiğini doğrular.
 */
class NotFoundAnswerStreamFilterTest {

    private static final String SENTENCE = TestProperties.NOT_FOUND_ANSWER;

    private final NotFoundAnswerDetector detector = new NotFoundAnswerDetector(TestProperties.defaults());

    private static List<String> accept(NotFoundAnswerStreamFilter filter, String... tokens) {
        List<String> emitted = new ArrayList<>();
        for (String token : tokens) {
            emitted.add(filter.accept(token));
        }
        return emitted;
    }

    @Test
    void givenSentenceThenCitationTokens_whenAccept_thenEmitsSentenceOnceAndSuppressesRest() {
        // Arrange
        NotFoundAnswerStreamFilter filter = detector.newStreamFilter();

        // Act
        List<String> emitted = accept(filter, "Bu konuda ", "rehberimizde bilgi ", "bulamadım.", " [5]");

        // Assert
        assertThat(emitted).containsExactly("", "", SENTENCE, "");
        assertThat(filter.isNotFoundAnswer()).isTrue();
        assertThat(filter.finish()).isEmpty();
    }

    @Test
    void givenAnswerStartingWithBu_whenAccept_thenFlushesHeldTextOnDivergence() {
        // Arrange
        NotFoundAnswerStreamFilter filter = detector.newStreamFilter();

        // Act
        List<String> emitted = accept(filter, "Bu ", "kartın yıllık ", "aidatı 750 TL'dir [1].");

        // Assert
        assertThat(emitted).containsExactly("", "Bu kartın yıllık ", "aidatı 750 TL'dir [1].");
        assertThat(filter.isNotFoundAnswer()).isFalse();
        assertThat(filter.finish()).isEmpty();
    }

    @Test
    void givenModelStopsMidPrefix_whenFinish_thenReturnsHeldText() {
        // Arrange
        NotFoundAnswerStreamFilter filter = detector.newStreamFilter();

        // Act
        String emitted = filter.accept("Bu konuda");
        String remaining = filter.finish();

        // Assert
        assertThat(emitted).isEmpty();
        assertThat(remaining).isEqualTo("Bu konuda");
        assertThat(filter.isNotFoundAnswer()).isFalse();
    }

    @Test
    void givenUpperCaseAndExtraSpaces_whenAccept_thenEmitsConfiguredSentence() {
        // Arrange
        NotFoundAnswerStreamFilter filter = detector.newStreamFilter();

        // Act
        String emitted = filter.accept(" BU  konuda rehberimizde bilgi bulamadım. [2]");

        // Assert
        assertThat(emitted).isEqualTo(SENTENCE);
        assertThat(filter.isNotFoundAnswer()).isTrue();
    }

    @Test
    void givenSentenceAcrossManySmallTokens_whenAccept_thenHeldUntilCompleteThenOneToken() {
        // Arrange
        NotFoundAnswerStreamFilter filter = detector.newStreamFilter();

        // Act
        List<String> emitted = accept(filter, "Bu", " kon", "uda rehber", "imizde bilgi bulamadım", ".", " [1]", " daha");

        // Assert
        assertThat(emitted).containsExactly("", "", "", "", SENTENCE, "", "");
    }

    @Test
    void givenWhitespaceOnlyTokenFirst_whenAccept_thenStillHolding() {
        // Arrange
        NotFoundAnswerStreamFilter filter = detector.newStreamFilter();

        // Act
        List<String> emitted = accept(filter, "  ", "Bu konuda rehberimizde bilgi bulamadım.");

        // Assert
        assertThat(emitted).containsExactly("", SENTENCE);
    }

    @Test
    void givenTextDivergingAfterPartialMatch_whenFinishAfterPass_thenNothingLeft() {
        // Arrange
        NotFoundAnswerStreamFilter filter = detector.newStreamFilter();

        // Act
        String first = filter.accept("Bu konuda rehberimizde bilgi var");
        String rest = filter.accept(" [2].");
        String remaining = filter.finish();

        // Assert
        assertThat(first).isEqualTo("Bu konuda rehberimizde bilgi var");
        assertThat(rest).isEqualTo(" [2].");
        assertThat(remaining).isEmpty();
    }
}
