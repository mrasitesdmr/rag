package com.rasitbank.assistant.generation;

import com.rasitbank.assistant.suggestion.domain.dto.SuggestionTextNormalizer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Mesajın tamamının selam, teşekkür ya da veda kalıplarından oluşup oluşmadığına ifade listelerine bakarak karar
 * verir; harf büyüklüğü, noktalama, emoji, Türkçe karakter yerine ASCII ve harf uzatma fark etmez. Hitap sözcükleri
 * tek başına selamlaşma sayılmaz. Takip mesajının etiket için yeterince kısa olup olmadığına da karar verir.
 */
@Component
public class SmallTalkDetector {

    private static final Pattern REPEATED_CHARACTER = Pattern.compile("(.)\\1+");
    private static final String COMMENT_PREFIX = "#";
    private static final int MAX_PHRASE_WORDS = 4;
    private static final int SHORT_MESSAGE_MAX_WORDS = 10;

    private record Phrase(List<String> words, MessageLabel label) {
    }

    private final List<Phrase> phrases;

    public SmallTalkDetector(
            @Value("classpath:small-talk/greeting.txt") Resource greetingPhrases,
            @Value("classpath:small-talk/thanks.txt") Resource thanksPhrases,
            @Value("classpath:small-talk/farewell.txt") Resource farewellPhrases,
            @Value("classpath:small-talk/address.txt") Resource addressPhrases) {
        List<Phrase> loaded = new ArrayList<>();
        loaded.addAll(readPhrases(greetingPhrases, MessageLabel.GREETING));
        loaded.addAll(readPhrases(thanksPhrases, MessageLabel.THANKS));
        loaded.addAll(readPhrases(farewellPhrases, MessageLabel.FAREWELL));
        loaded.addAll(readPhrases(addressPhrases, MessageLabel.OTHER));
        this.phrases = List.copyOf(loaded);
    }

    public Optional<MessageLabel> detect(String message) {
        String key = phraseKey(message);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        List<String> words = Arrays.asList(key.split(" "));
        List<Set<Set<MessageLabel>>> reachable = new ArrayList<>(words.size() + 1);
        for (int i = 0; i <= words.size(); i++) {
            reachable.add(new HashSet<>());
        }
        reachable.get(0).add(EnumSet.noneOf(MessageLabel.class));
        for (int start = 0; start < words.size(); start++) {
            if (reachable.get(start).isEmpty()) {
                continue;
            }
            for (Phrase phrase : phrases) {
                int end = start + phrase.words().size();
                if (end <= words.size() && words.subList(start, end).equals(phrase.words())) {
                    for (Set<MessageLabel> labels : reachable.get(start)) {
                        Set<MessageLabel> extended = EnumSet.noneOf(MessageLabel.class);
                        extended.addAll(labels);
                        if (phrase.label().isSmallTalk()) {
                            extended.add(phrase.label());
                        }
                        reachable.get(end).add(extended);
                    }
                }
            }
        }
        return strongestLabel(reachable.get(words.size()));
    }

    public boolean isShortMessage(String message) {
        String normalized = SuggestionTextNormalizer.normalize(message);
        if (normalized.isEmpty() || message.indexOf('?') >= 0) {
            return false;
        }
        return normalized.split(" ").length <= SHORT_MESSAGE_MAX_WORDS;
    }

    public static String phraseKey(String text) {
        String normalized = SuggestionTextNormalizer.normalize(text);
        return REPEATED_CHARACTER.matcher(foldTurkishLetters(normalized)).replaceAll("$1");
    }

    static String foldTurkishLetters(String text) {
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

    private static Optional<MessageLabel> strongestLabel(Set<Set<MessageLabel>> coverings) {
        Set<MessageLabel> found = EnumSet.noneOf(MessageLabel.class);
        coverings.forEach(found::addAll);
        for (MessageLabel label : List.of(MessageLabel.THANKS, MessageLabel.FAREWELL, MessageLabel.GREETING)) {
            if (found.contains(label)) {
                return Optional.of(label);
            }
        }
        return Optional.empty();
    }

    private static List<Phrase> readPhrases(Resource resource, MessageLabel label) {
        String content;
        try {
            content = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Small talk phrase list cannot be read: " + resource.getDescription(),
                    exception);
        }
        return content.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith(COMMENT_PREFIX))
                .map(SmallTalkDetector::phraseKey)
                .filter(key -> !key.isEmpty())
                .map(key -> new Phrase(List.of(key.split(" ")), label))
                .map(phrase -> requireShortPhrase(phrase, resource))
                .distinct()
                .toList();
    }

    private static Phrase requireShortPhrase(Phrase phrase, Resource resource) {
        if (phrase.words().size() > MAX_PHRASE_WORDS) {
            throw new IllegalStateException("Small talk phrase longer than " + MAX_PHRASE_WORDS + " words in "
                    + resource.getDescription());
        }
        return phrase;
    }
}
