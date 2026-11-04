package com.rasitbank.assistant.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

/**
 * Kalıp listesinin selam, teşekkür ve vedayı yazım çeşitlerine rağmen tanıdığını, soru içeren ya da bilinmeyen metne
 * ve tek başına hitap sözcüğüne dokunmadığını doğrular.
 */
class SmallTalkDetectorTest {

    private final SmallTalkDetector detector = new SmallTalkDetector(
            new ClassPathResource("small-talk/greeting.txt"), new ClassPathResource("small-talk/thanks.txt"),
            new ClassPathResource("small-talk/farewell.txt"), new ClassPathResource("small-talk/address.txt"));

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"Selam", "SELAM!!", "selaaam", "Merhaba, nasılsın?", "İyi günler 🙂", "selamün aleyküm",
            "Naber dostum, ne var ne yok", "slm", "  günaydın  ", "MERHABA!!!", "Hey, hello"})
    void givenGreetingSpellingsCasePunctuationAndEmoji_whenDetect_thenGreeting(String message) {
        // Act
        Optional<MessageLabel> label = detector.detect(message);

        // Assert
        assertThat(label).contains(MessageLabel.GREETING);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Teşekkürler, görüşürüz | THANKS",
            "Merhaba, görüşürüz | FAREWELL",
            "Merhaba, teşekkür ederim | THANKS",
            "tesekkurler | THANKS",
            "saol | THANKS",
            "eyv | THANKS",
            "Teşekkür ederim. | THANKS",
            "Çok teşekkürler dostum | THANKS",
            "Sağ ol | THANKS",
            "Görüşürüz | FAREWELL",
            "iyi geceler | FAREWELL",
            "Hoşça kal | FAREWELL"})
    void givenThanksAndFarewellPhrases_whenDetect_thenThanksBeatsFarewellBeatsGreeting(String message,
            MessageLabel expected) {
        // Act
        Optional<MessageLabel> label = detector.detect(message);

        // Assert
        assertThat(label).contains(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"Merhaba, kartımı kaybettim", "Selam kart aidatı ne kadar", "Bitcoin alıp satabilir miyim?",
            "asdfgh", "dostum", "   ", "", "!!!", "kanka asistan", "Teşekkürler ama kartım hâlâ gelmedi",
            "Selamlar herkese", "Keyifler nasıl?", "Çok makbule geçti", "Görüşmek dileğiyle",
            "Merhabalar efendim, hayırlı işler"})
    void givenQuestionUnknownTextOrAddressWordAlone_whenDetect_thenEmpty(String message) {
        // Act
        Optional<MessageLabel> label = detector.detect(message);

        // Assert
        assertThat(label).isEmpty();
    }

    @Test
    void givenDetectorOverLabelNeverOther_whenDetectAnyMatch_thenLabelIsSmallTalk() {
        // Act
        Optional<MessageLabel> label = detector.detect("Merhaba efendim");

        // Assert
        assertThat(label).hasValueSatisfying(value -> assertThat(value.isSmallTalk()).isTrue());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Çok işime yaradı sağ olun | true",
            "Her şey için sağ olun, iyi akşamlar | true",
            "Bir iki üç dört beş altı yedi sekiz dokuz on | true",
            "Bir iki üç dört beş altı yedi sekiz dokuz on onbir | false",
            "Peki Platinum? | false",
            "ya kredi kartında | true",
            "🙂 !! | false",
            "?evet | false"})
    void givenMessages_whenIsShortMessage_thenUpToTenWordsWithoutQuestionMark(String message, boolean expected) {
        // Act
        boolean isShort = detector.isShortMessage(message);

        // Assert
        assertThat(isShort).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "SELAAAM!! | selam",
            "Teşekkürler | tesekurler",
            "İyi Günler 🙂 | iyi gunler",
            "Çok  Şükür | cok sukur",
            "Ağır Öğün Üzüm | agir ogun uzum"})
    void givenSpellingVariants_whenPhraseKey_thenLowercaseAsciiWithoutRepeatedLetters(String text, String expected) {
        // Act
        String key = SmallTalkDetector.phraseKey(text);

        // Assert
        assertThat(key).isEqualTo(expected);
    }

    @Test
    void givenMeasurementList_whenDetect_thenListedGreetingsMatchAndUnlistedOnesDoNot() throws IOException {
        // Arrange
        List<String> lines = Files.readAllLines(Path.of("scripts/negative-messages.txt"), StandardCharsets.UTF_8);
        List<String> listed = messages(lines, "small-talk-listed");
        List<String> unlisted = messages(lines, "small-talk-unlisted");

        // Act
        List<String> listedMisses = listed.stream().filter(message -> detector.detect(message).isEmpty()).toList();
        List<String> unlistedHits = unlisted.stream().filter(message -> detector.detect(message).isPresent()).toList();

        // Assert
        assertThat(listed).hasSize(13);
        assertThat(unlisted).hasSize(5);
        assertThat(listedMisses).isEmpty();
        assertThat(unlistedHits).isEmpty();
    }

    private static List<String> messages(List<String> lines, String category) {
        return lines.stream()
                .filter(line -> line.startsWith(category + " | "))
                .map(line -> line.substring((category + " | ").length()))
                .toList();
    }
}
