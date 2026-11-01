package com.rasitbank.assistant.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Gönderilen SSE olaylarını (ad ve veri nesnesi) sırayla kaydeden, istenen gönderimde IOException atabilen
 * ve istemci kopmasını simüle edebilen sahte emitter.
 */
public class RecordingSseEmitter extends SseEmitter {

    public record Event(String name, Object data) {
    }

    private final List<Event> events = Collections.synchronizedList(new ArrayList<>());
    private final List<String> timeline = Collections.synchronizedList(new ArrayList<>());
    private final CountDownLatch completed = new CountDownLatch(1);
    private final int failOnSend;
    private boolean runtimeFailure;
    private Runnable completionCallback = () -> { };
    private Runnable timeoutCallback = () -> { };
    private Consumer<Throwable> errorCallback = error -> { };

    public RecordingSseEmitter() {
        this(0);
    }

    /** failOnSend = n ise n'inci gönderim (1'den başlar) IOException atar; 0 ise hiç atmaz. */
    public RecordingSseEmitter(int failOnSend) {
        super(300_000L);
        this.failOnSend = failOnSend;
    }

    /** n'inci gönderim IOException yerine RuntimeException atar. */
    public static RecordingSseEmitter failingWithRuntimeExceptionOnSend(int failOnSend) {
        RecordingSseEmitter emitter = new RecordingSseEmitter(failOnSend);
        emitter.runtimeFailure = true;
        return emitter;
    }

    @Override
    public synchronized void send(SseEventBuilder builder) throws IOException {
        int attempt = events.size() + failedSends() + 1;
        if (failOnSend > 0 && attempt == failOnSend) {
            failed++;
            if (runtimeFailure) {
                throw new IllegalStateException("emitter broken");
            }
            throw new IOException("client disconnected");
        }
        Set<DataWithMediaType> parts = builder.build();
        String name = "message";
        Object data = null;
        for (DataWithMediaType part : parts) {
            Object value = part.getData();
            if (value instanceof String text) {
                if (text.startsWith("event:")) {
                    name = text.substring("event:".length()).lines().findFirst().orElse("").strip();
                }
            } else {
                data = value;
            }
        }
        events.add(new Event(name, data));
        timeline.add(name);
    }

    private int failed;

    private int failedSends() {
        return failed;
    }

    @Override
    public void complete() {
        timeline.add("complete");
        completed.countDown();
    }

    @Override
    public void onCompletion(Runnable callback) {
        this.completionCallback = callback;
    }

    @Override
    public void onTimeout(Runnable callback) {
        this.timeoutCallback = callback;
    }

    @Override
    public void onError(Consumer<Throwable> callback) {
        this.errorCallback = callback;
    }

    public void note(String marker) {
        timeline.add(marker);
    }

    public List<Event> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    public List<String> eventNames() {
        return events().stream().map(Event::name).toList();
    }

    public List<String> timeline() {
        synchronized (timeline) {
            return List.copyOf(timeline);
        }
    }

    public boolean awaitCompletion(long timeoutMillis) throws InterruptedException {
        return completed.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void simulateTimeout() {
        timeoutCallback.run();
    }

    public void simulateCompletionCallback() {
        completionCallback.run();
    }

    public void simulateError(Throwable error) {
        errorCallback.accept(error);
    }
}
