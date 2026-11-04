package com.rasitbank.assistant.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Sahte dil modelini etiket çağrısıyla cevap/yeniden yazma çağrılarını ayıracak biçimde kurar: etiket talimatıyla
 * gelen çağrıya sabit bir etiket, diğer çağrılara sırayla verilen akışlar döner.
 */
public final class ChatModelStubs {

    private static final String LABEL_PROMPT_START = "Bir bankanın dijital asistanına gelen müşteri mesajını";

    private ChatModelStubs() {
    }

    @SafeVarargs
    public static void stub(ChatModel chatModel, Flux<ChatResponse>... responses) {
        stubWithLabel(chatModel, "DIGER", responses);
    }

    @SafeVarargs
    public static void stubWithLabel(ChatModel chatModel, String labelOutput, Flux<ChatResponse>... responses) {
        List<Flux<ChatResponse>> outputs = List.of(responses);
        AtomicInteger calls = new AtomicInteger();
        given(chatModel.stream(any(Prompt.class))).willAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            if (isLabelCall(prompt)) {
                return Flux.just(ChatResponses.text(labelOutput));
            }
            return outputs.get(Math.min(calls.getAndIncrement(), outputs.size() - 1));
        });
    }

    public static List<Prompt> nonLabelPrompts(ChatModel chatModel) {
        org.mockito.ArgumentCaptor<Prompt> prompts = org.mockito.ArgumentCaptor.captor();
        org.mockito.Mockito.verify(chatModel, org.mockito.Mockito.atLeast(0)).stream(prompts.capture());
        return prompts.getAllValues().stream().filter(prompt -> !isLabelCall(prompt)).toList();
    }

    public static boolean isLabelCall(Prompt prompt) {
        Message first = prompt.getInstructions().getFirst();
        return first instanceof SystemMessage && first.getText().startsWith(LABEL_PROMPT_START);
    }
}
