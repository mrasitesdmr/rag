package com.rasitbank.assistant.generation;

import com.rasitbank.assistant.config.AssistantProperties;
import com.rasitbank.assistant.retrieval.service.RetrievedChunk;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Dil modeline giden prompt'u kurar: sabit sistem talimatı ve numaralı parçalarla müşterinin sorusunu taşıyan
 * kullanıcı mesajı. Parçalar ve soru talimata değil kullanıcı mesajına girer.
 */
@Component
public class PromptBuilder {

    private final String systemText;
    private final PromptTemplate userTemplate;

    public PromptBuilder(@Value("classpath:prompts/rag-system.st") Resource systemTemplate,
            @Value("classpath:prompts/rag-user.st") Resource userTemplate, AssistantProperties properties) {
        this.systemText = PromptTemplate.builder()
                .resource(systemTemplate)
                .build()
                .render(Map.of("notFoundAnswer", properties.generation().notFoundAnswer()));
        this.userTemplate = PromptTemplate.builder().resource(userTemplate).build();
    }

    public Prompt buildPrompt(String question, List<RetrievedChunk> contextChunks) {
        String chunks = IntStream.range(0, contextChunks.size())
                .mapToObj(i -> chunkBlock(i + 1, contextChunks.get(i)))
                .collect(Collectors.joining("\n\n"));
        String userText = userTemplate.render(Map.of("chunks", chunks, "question", question));
        return new Prompt(List.of(new SystemMessage(systemText), new UserMessage(userText)));
    }

    private static String chunkBlock(int n, RetrievedChunk chunk) {
        return "[" + n + "] " + chunk.hit().heading() + "\n" + chunk.hit().content();
    }
}
