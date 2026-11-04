package com.rasitbank.assistant.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.support.ChatResponses;
import com.rasitbank.assistant.support.TestProperties;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

/**
 * Etiket çağrısını doğrular: çıktının tam etikete eşlenmesi (başka her şey "diğer"), hata ve zaman aşımında "diğer"
 * ile aboneliğin iptali, modele sadece etiket talimatı ve müşteri mesajının gitmesi, talimattaki örneklerin ölçüm
 * listesini ezberletmemesi.
 */
class MessageLabelerTest {

    private static final String CUSTOMER_MESSAGE = "Çok makbule geçti";

    private final ChatModel chatModel = mock(ChatModel.class);

    private MessageLabeler labeler(AssistantProperties properties) {
        return new MessageLabeler(chatModel, new ClassPathResource("prompts/message-label.st"), properties);
    }

    static Stream<Arguments> modelOutputs() {
        return Stream.of(
                Arguments.of("SELAM", MessageLabel.GREETING),
                Arguments.of(" tesekkur.", MessageLabel.THANKS),
                Arguments.of("TEŞEKKÜR", MessageLabel.THANKS),
                Arguments.of("\"Veda\"", MessageLabel.FAREWELL),
                Arguments.of("DİĞER", MessageLabel.OTHER),
                Arguments.of("Diğer.", MessageLabel.OTHER),
                Arguments.of("Merhaba!", MessageLabel.OTHER),
                Arguments.of("SELAM VEDA", MessageLabel.OTHER),
                Arguments.of("SELAM ama kartımı kaybettim", MessageLabel.OTHER),
                Arguments.of("", MessageLabel.OTHER),
                Arguments.of("   ", MessageLabel.OTHER));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @MethodSource("modelOutputs")
    void givenModelOutputs_whenParseLabel_thenMapsExactLabelsElseOther(String output, MessageLabel expected) {
        // Act
        MessageLabel label = MessageLabeler.parseLabel(output);

        // Assert
        assertThat(label).isEqualTo(expected);
    }

    @Test
    void givenModelAnswersTesekkurInTokens_whenLabelMessage_thenThanksNotFailed() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.just(ChatResponses.text("TESEK"), ChatResponses.text("KUR")));

        // Act
        MessageLabelResult result = labeler(TestProperties.defaults()).labelMessage(CUSTOMER_MESSAGE);

        // Assert
        assertThat(result.label()).isEqualTo(MessageLabel.THANKS);
        assertThat(result.failed()).isFalse();
        assertThat(result.latencyMs()).isNotNegative();
    }

    @Test
    void givenModelError_whenLabelMessage_thenOtherAndFailed() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.error(new IllegalStateException("model down")));

        // Act
        MessageLabelResult result = labeler(TestProperties.defaults()).labelMessage(CUSTOMER_MESSAGE);

        // Assert
        assertThat(result.label()).isEqualTo(MessageLabel.OTHER);
        assertThat(result.failed()).isTrue();
    }

    @Test
    void givenModelNeverCompletes_whenLabelMessageWithShortTimeout_thenOtherFailedAndSubscriptionCancelled() {
        // Arrange
        AtomicBoolean cancelled = new AtomicBoolean();
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.<org.springframework.ai.chat.model.ChatResponse>never().doOnCancel(() -> cancelled.set(true)));

        // Act
        MessageLabelResult result = labeler(TestProperties.withLabelTimeout(Duration.ofMillis(10)))
                .labelMessage(CUSTOMER_MESSAGE);

        // Assert
        assertThat(result.label()).isEqualTo(MessageLabel.OTHER);
        assertThat(result.failed()).isTrue();
        assertThat(cancelled).isTrue();
    }

    @Test
    void givenMessage_whenLabelMessage_thenSystemIsLabelInstructionAndUserIsMessageOnly() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("DIGER")));

        // Act
        labeler(TestProperties.defaults()).labelMessage(CUSTOMER_MESSAGE);

        // Assert
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        org.mockito.BDDMockito.then(chatModel).should().stream(prompt.capture());
        List<Message> messages = prompt.getValue().getInstructions();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(messages.get(0).getText()).startsWith("Bir bankanın dijital asistanına gelen müşteri mesajını "
                + "sınıflandırıyorsun. Mesaja cevap yazma.").contains("SELAM:", "TESEKKUR:", "VEDA:", "DIGER:")
                .doesNotContain(CUSTOMER_MESSAGE, "<parcalar>");
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo(CUSTOMER_MESSAGE);
    }

    @Test
    void givenMessage_whenLabelMessage_thenSystemMessageContainsLabelExamples() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("DIGER")));

        // Act
        labeler(TestProperties.defaults()).labelMessage("Hey");

        // Assert
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        org.mockito.BDDMockito.then(chatModel).should().stream(prompt.capture());
        List<Message> messages = prompt.getValue().getInstructions();
        assertThat(messages.get(0).getText()).contains("Bu bilgi çok yardımcı oldu → TESEKKUR",
                "Ya bireysel kredide → DIGER");
        assertThat(messages.get(1).getText()).isEqualTo("Hey");
    }

    @Test
    void givenMessage_whenLabelMessage_thenSystemMessageTreatsSelfIntroductionAsGreeting() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("SELAM")));

        // Act
        labeler(TestProperties.defaults()).labelMessage("Ahmet");

        // Assert
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        org.mockito.BDDMockito.then(chatModel).should().stream(prompt.capture());
        List<Message> messages = prompt.getValue().getInstructions();
        assertThat(messages.get(0).getText()).contains("kendini tanıtma ya da sadece adını yazma",
                "Ben Zeynep → SELAM");
        assertThat(messages.get(1).getText()).isEqualTo("Ahmet");
    }

    @Test
    void givenMessage_whenLabelMessage_thenSystemMessageContainsAbbreviatedGreetingExamples() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(ChatResponses.text("SELAM")));

        // Act
        labeler(TestProperties.defaults()).labelMessage("Mrhb");

        // Assert
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.captor();
        org.mockito.BDDMockito.then(chatModel).should().stream(prompt.capture());
        String system = prompt.getValue().getInstructions().get(0).getText();
        assertThat(system).contains("Mrhb → SELAM", "Nbr kanka → SELAM", "Tşkkrler → TESEKKUR", "Grşrz → VEDA");
        assertThat(system.lines().filter(line -> line.startsWith("Nber → ")).toList()).isEmpty();
    }

    @Test
    void givenLabelPromptMeasurementListAndGoldenSet_whenCompared_thenNoMessageOverlapsAnExample() throws IOException {
        // Arrange
        String promptText = Files.readString(Path.of("src/main/resources/prompts/message-label.st"),
                StandardCharsets.UTF_8);
        List<String> exampleKeys = promptText.lines()
                .filter(line -> line.contains(" → "))
                .map(line -> phraseKey(line.substring(0, line.indexOf(" → "))))
                .toList();
        List<String> measuredKeys = Files.readAllLines(Path.of("scripts/negative-messages.txt"), StandardCharsets.UTF_8)
                .stream()
                .filter(line -> !line.startsWith("#") && line.contains(" | "))
                .map(line -> line.substring(line.indexOf(" | ") + 3))
                .flatMap(text -> java.util.Arrays.stream(text.split(" >> ")))
                .map(MessageLabelerTest::phraseKey)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        List<String> goldenKeys = goldenQuestions().stream().map(MessageLabelerTest::phraseKey).toList();
        measuredKeys.addAll(goldenKeys);

        // Act
        List<String> leaked = exampleKeys.stream().filter(measuredKeys::contains).toList();
        List<String> contained = exampleKeys.stream()
                .flatMap(example -> measuredKeys.stream().filter(measured -> overlapsAtWordBoundary(example, measured))
                        .map(measured -> example + " ~ " + measured))
                .toList();

        // Assert
        assertThat(exampleKeys).hasSizeGreaterThanOrEqualTo(28);
        assertThat(goldenKeys).hasSize(100);
        assertThat(measuredKeys).contains(phraseKey("Nber"), phraseKey("Ahmet"), phraseKey("Ben Ayşe"),
                phraseKey("Adım Mehmet"), phraseKey("Nasılsın?"), phraseKey("Merhaba, iyi misin?"),
                phraseKey("Çok makbule geçti"),
                phraseKey("Selamlar herkese"), phraseKey("Keyifler nasıl?"),
                phraseKey("Görüşmek dileğiyle"),
                phraseKey("Merhabalar efendim, hayırlı işler"));
        assertThat(leaked).isEmpty();
        assertThat(contained).isEmpty();
    }

    private static List<String> goldenQuestions() throws IOException {
        tools.jackson.databind.JsonNode root = new tools.jackson.databind.json.JsonMapper()
                .readTree(Files.readString(Path.of("../docs/rehber/golden/rehber-golden-v1.json"),
                        StandardCharsets.UTF_8));
        List<String> questions = new java.util.ArrayList<>();
        root.get("questions").forEach(node -> questions.add(node.get("text").asString()));
        return questions;
    }

    private static String phraseKey(String text) {
        String folded = SuggestionTextNormalizer.normalize(text)
                .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i').replace('ö', 'o').replace('ş', 's')
                .replace('ü', 'u').replace('â', 'a').replace('î', 'i').replace('û', 'u');
        return folded.replaceAll("(.)\\1+", "$1");
    }

    private static boolean overlapsAtWordBoundary(String example, String measured) {
        if (measured.split(" ").length < 3) {
            return false;
        }
        return (" " + example + " ").contains(" " + measured + " ")
                || (" " + measured + " ").contains(" " + example + " ");
    }

    @Test
    void givenSlowModel_whenLabelMessage_thenLatencyCoversModelCall() {
        // Arrange
        given(chatModel.stream(any(Prompt.class))).willReturn(
                Flux.just(ChatResponses.text("VEDA")).delayElements(Duration.ofMillis(150)));

        // Act
        MessageLabelResult result = labeler(TestProperties.defaults()).labelMessage("Hadi bana müsaade");

        // Assert
        assertThat(result.label()).isEqualTo(MessageLabel.FAREWELL);
        assertThat(result.latencyMs()).isBetween(100L, 5_000L);
    }
}
