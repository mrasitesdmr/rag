package com.rasitbank.assistant.generation;

import com.rasitbank.assistant.config.AssistantProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.Exceptions;

/**
 * Mesajın selamlaşma, teşekkür ya da veda olup olmadığını dil modeline sadece etiket olarak sorar (kısa mesajda
 * aramadan önce, uzun mesajda reranker puanı eşiğin altındaysa); modele cevap yazdırmaz, parça ve geçmiş vermez.
 * Hata, zaman aşımı ya da tanımsız çıktı "diğer" sayılır.
 */
@Service
public class MessageLabeler {

    private static final Logger log = LoggerFactory.getLogger(MessageLabeler.class);

    private static final Pattern EDGE_NON_WORD = Pattern.compile("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$");
    private static final Map<String, MessageLabel> LABELS = Map.of(
            "SELAM", MessageLabel.GREETING,
            "TESEKKUR", MessageLabel.THANKS,
            "VEDA", MessageLabel.FAREWELL,
            "DIGER", MessageLabel.OTHER);

    private final ChatModel chatModel;
    private final String labelInstruction;
    private final Duration labelTimeout;

    public MessageLabeler(ChatModel chatModel, @Value("classpath:prompts/message-label.st") Resource labelTemplate,
            AssistantProperties properties) {
        this.chatModel = chatModel;
        this.labelInstruction = readText(labelTemplate);
        this.labelTimeout = properties.smallTalk().labelTimeout();
    }

    public MessageLabelResult labelMessage(String message) {
        Prompt prompt = new Prompt(List.of(new SystemMessage(labelInstruction), new UserMessage(message)));
        long startNanos = System.nanoTime();
        try {
            MessageLabel label = parseLabel(callModel(prompt));
            return new MessageLabelResult(label, elapsedMs(startNanos), false);
        } catch (RuntimeException exception) {
            long durationMs = elapsedMs(startNanos);
            log.warn("Message label failed, treating as other reason={} durationMs={}",
                    Exceptions.unwrap(exception).getClass().getSimpleName(), durationMs);
            return new MessageLabelResult(MessageLabel.OTHER, durationMs, true);
        }
    }

    public static MessageLabel parseLabel(String output) {
        String word = EDGE_NON_WORD.matcher(output.strip()).replaceAll("");
        String key = foldTurkishLetters(word).toUpperCase(Locale.ROOT);
        return LABELS.getOrDefault(key, MessageLabel.OTHER);
    }

    private static String foldTurkishLetters(String text) {
        StringBuilder folded = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            folded.append(switch (text.charAt(i)) {
                case 'ç' -> 'c';
                case 'Ç' -> 'C';
                case 'ğ' -> 'g';
                case 'Ğ' -> 'G';
                case 'ı' -> 'i';
                case 'İ' -> 'I';
                case 'ö' -> 'o';
                case 'Ö' -> 'O';
                case 'ş' -> 's';
                case 'Ş' -> 'S';
                case 'ü' -> 'u';
                case 'Ü' -> 'U';
                case 'â' -> 'a';
                case 'î' -> 'i';
                case 'û' -> 'u';
                default -> text.charAt(i);
            });
        }
        return folded.toString();
    }

    private String callModel(Prompt prompt) {
        String output = chatModel.stream(prompt)
                .map(MessageLabeler::textOf)
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .timeout(labelTimeout)
                .block();
        return Objects.requireNonNullElse(output, "");
    }

    private static String textOf(ChatResponse response) {
        Generation generation = response.getResult();
        if (generation == null) {
            return "";
        }
        return Objects.requireNonNullElse(generation.getOutput().getText(), "");
    }

    private static String readText(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException exception) {
            throw new UncheckedIOException("Prompt template could not be read: " + resource.getDescription(),
                    exception);
        }
    }

    private static long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
