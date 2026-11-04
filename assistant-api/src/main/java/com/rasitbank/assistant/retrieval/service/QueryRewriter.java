package com.rasitbank.assistant.retrieval.service;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.conversation.domain.entity.QueryRewriteStatus;
import com.rasitbank.assistant.generation.HistoryTurn;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

/**
 * Takip sorusunu müşterinin önceki sorularıyla (asistanın cevapları olmadan) dil modeline verip tek başına
 * anlaşılır bir arama sorgusuna çevirir; başarısız olursa aramanın müşterinin sorusuyla yapılmasını sağlar.
 */
@Service
public class QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriter.class);

    private static final Pattern STANDALONE_QUERY_LABEL = Pattern.compile("^(?i)(standalone query|arama sorusu):");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String TOO_LONG_REASON = "TooLong";
    private static final String EMPTY_REASON = "Empty";

    private final ChatModel chatModel;
    private final PromptTemplate rewriteTemplate;
    private final AssistantProperties.QueryRewrite settings;

    public QueryRewriter(ChatModel chatModel, @Value("classpath:prompts/query-rewrite.st") Resource rewriteTemplate,
            AssistantProperties properties) {
        this.chatModel = chatModel;
        this.rewriteTemplate = PromptTemplate.builder().resource(rewriteTemplate).build();
        this.settings = properties.queryRewrite();
    }

    public QueryRewriteResult rewriteQuery(String question, List<HistoryTurn> history) {
        if (!settings.enabled()) {
            return new QueryRewriteResult(QueryRewriteStatus.DISABLED, question, null);
        }
        if (history.isEmpty()) {
            return new QueryRewriteResult(QueryRewriteStatus.NO_HISTORY, question, null);
        }
        Prompt prompt = new Prompt(List.of(new UserMessage(rewriteTemplate.render(
                Map.of("history", historyText(history), "query", question)))));
        long startNanos = System.nanoTime();
        String output;
        try {
            output = callModel(prompt);
        } catch (RuntimeException exception) {
            return failed(question, startNanos, Exceptions.unwrap(exception).getClass().getSimpleName());
        }
        String standaloneQuery = normalize(output);
        if (standaloneQuery.isEmpty()) {
            return rejected(QueryRewriteStatus.EMPTY, question, startNanos, EMPTY_REASON);
        }
        if (standaloneQuery.length() > settings.maxLength()) {
            return failed(question, startNanos, TOO_LONG_REASON);
        }
        return new QueryRewriteResult(QueryRewriteStatus.REWRITTEN, standaloneQuery, elapsedMs(startNanos));
    }

    private String callModel(Prompt prompt) {
        Flux<ChatResponse> responses = chatModel.stream(prompt);
        String output = responses
                .map(QueryRewriter::textOf)
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .timeout(settings.timeout())
                .block();
        return Objects.requireNonNullElse(output, "");
    }

    private QueryRewriteResult failed(String question, long startNanos, String reason) {
        return rejected(QueryRewriteStatus.FAILED, question, startNanos, reason);
    }

    private static QueryRewriteResult rejected(QueryRewriteStatus status, String question, long startNanos,
            String reason) {
        long durationMs = elapsedMs(startNanos);
        log.warn("Query rewrite failed, searching with the original question status={} reason={} durationMs={}",
                status, reason, durationMs);
        return new QueryRewriteResult(status, question, durationMs);
    }

    private static String historyText(List<HistoryTurn> history) {
        return history.stream()
                .map(turn -> "- " + turn.question())
                .collect(Collectors.joining("\n"));
    }

    private static String normalize(String output) {
        String text = STANDALONE_QUERY_LABEL.matcher(output.strip()).replaceFirst("").strip();
        text = removeSurroundingQuotes(text);
        return WHITESPACE.matcher(text).replaceAll(" ").strip();
    }

    private static String removeSurroundingQuotes(String text) {
        if (text.length() >= 2 && ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("“") && text.endsWith("”")))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static String textOf(ChatResponse response) {
        Generation generation = response.getResult();
        if (generation == null) {
            return "";
        }
        return Objects.requireNonNullElse(generation.getOutput().getText(), "");
    }

    private static long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
