package com.rasitbank.retrieval.embedding;

import static com.rasitbank.retrieval.fixture.IngestionFixtures.embeddingProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rasitbank.retrieval.config.EmbeddingClientConfig;
import com.rasitbank.retrieval.config.EmbeddingProperties;
import com.rasitbank.retrieval.embedding.client.EmbeddingClient;
import com.rasitbank.retrieval.exception.ServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * EmbeddingClient'ın istek biçimini, yeniden denemesini, boyut/sayı doğrulamasını ve eşzamanlılık sınırını doğrular.
 */
class EmbeddingClientTest {

    private static final String URL = "http://ollama.test/api/embed";

    private final EmbeddingProperties properties = embeddingProperties("bge-m3", 1024, Duration.ZERO, 2);

    private static String vector(int dimensions, float value) {
        return IntStream.range(0, dimensions).mapToObj(index -> Float.toString(value))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String body(String... vectors) {
        return "{\"model\":\"bge-m3\",\"embeddings\":[" + String.join(",", vectors) + "]}";
    }

    private record Setup(EmbeddingClient client, MockRestServiceServer server) {
    }

    private Setup mocked() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ollama.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Setup(new EmbeddingClient(builder.build(), properties), server);
    }

    @Test
    void givenTwoInputs_whenEmbed_thenPostsModelAndInputsAndReturnsVectorsInOrder() {
        Setup setup = mocked();
        setup.server().expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"model\":\"bge-m3\",\"input\":[\"a\",\"b\"]}", true))
                .andRespond(withSuccess(body(vector(1024, 0.25f), vector(1024, 0.5f)), MediaType.APPLICATION_JSON));

        List<float[]> vectors = setup.client().embed(List.of("a", "b"));

        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(0)).hasSize(1024).startsWith(0.25f);
        assertThat(vectors.get(1)[0]).isEqualTo(0.5f);
        setup.server().verify();
    }

    @Test
    void givenServerErrorThenSuccess_whenEmbed_thenRetriesOnceAndReturnsVectors() {
        Setup setup = mocked();
        setup.server().expect(ExpectedCount.once(), requestTo(URL)).andRespond(withServerError());
        setup.server().expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(body(vector(1024, 1f)), MediaType.APPLICATION_JSON));

        List<float[]> vectors = setup.client().embed(List.of("a"));

        assertThat(vectors).hasSize(1);
        setup.server().verify();
    }

    @Test
    void givenServerErrorOnEveryAttempt_whenEmbed_thenThrowsEmbeddingUnavailableAfterThreeAttempts() {
        Setup setup = mocked();
        setup.server().expect(ExpectedCount.times(3), requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> setup.client().embed(List.of("a"))).isInstanceOfSatisfying(
                ServiceUnavailableException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("EMBEDDING_UNAVAILABLE"));
        setup.server().verify();
    }

    @Test
    void givenClientError_whenEmbed_thenThrowsEmbeddingUnavailableWithoutRetry() {
        Setup setup = mocked();
        setup.server().expect(ExpectedCount.once(), requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> setup.client().embed(List.of("a"))).isInstanceOfSatisfying(
                ServiceUnavailableException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("EMBEDDING_UNAVAILABLE"));
        setup.server().verify();
    }

    @Test
    void given512DimensionVector_whenEmbed_thenThrowsEmbeddingDimensionMismatchWithoutRetry() {
        Setup setup = mocked();
        setup.server().expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(body(vector(512, 1f)), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> setup.client().embed(List.of("a"))).isInstanceOfSatisfying(
                ServiceUnavailableException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("EMBEDDING_DIMENSION_MISMATCH"));
        setup.server().verify();
    }

    @Test
    void givenOneVectorForTwoInputs_whenEmbed_thenThrowsEmbeddingUnavailable() {
        Setup setup = mocked();
        setup.server().expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(body(vector(1024, 1f)), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> setup.client().embed(List.of("a", "b"))).isInstanceOfSatisfying(
                ServiceUnavailableException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("EMBEDDING_UNAVAILABLE"));
    }

    @Test
    void givenNothingListeningOnPort9_whenEmbed_thenThrowsEmbeddingUnavailable() {
        EmbeddingProperties unreachable = new EmbeddingProperties("http://127.0.0.1:9", "bge-m3", 1024,
                Duration.ofSeconds(1), Duration.ofSeconds(1), 3, Duration.ZERO, 2);
        EmbeddingClient client = new EmbeddingClient(new EmbeddingClientConfig().embeddingRestClient(unreachable),
                unreachable);

        assertThatThrownBy(() -> client.embed(List.of("a"))).isInstanceOfSatisfying(
                ServiceUnavailableException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("EMBEDDING_UNAVAILABLE"));
    }

    @Test
    void givenFourConcurrentCalls_whenEmbed_thenAtMostTwoRequestsInFlight() throws Exception {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        AtomicInteger served = new AtomicInteger();
        CountDownLatch twoArrived = new CountDownLatch(2);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/api/embed", exchange -> {
            exchange.getRequestBody().readAllBytes();
            int now = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(now, Math::max);
            twoArrived.countDown();
            try {
                if (served.incrementAndGet() <= 2) {
                    twoArrived.await(5, TimeUnit.SECONDS);
                    Thread.sleep(300);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            inFlight.decrementAndGet();
            byte[] response = body(vector(1024, 1f)).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        ExecutorService callers = Executors.newFixedThreadPool(4);
        try {
            EmbeddingProperties live = new EmbeddingProperties("http://127.0.0.1:" + server.getAddress().getPort(),
                    "bge-m3", 1024, Duration.ofSeconds(2), Duration.ofSeconds(10), 3, Duration.ZERO, 2);
            EmbeddingClient client = new EmbeddingClient(new EmbeddingClientConfig().embeddingRestClient(live), live);

            List<Future<List<float[]>>> futures = IntStream.range(0, 4)
                    .mapToObj(index -> callers.submit(() -> client.embed(List.of("x" + index)))).toList();
            for (Future<List<float[]>> future : futures) {
                assertThat(future.get(15, TimeUnit.SECONDS)).hasSize(1);
            }

            assertThat(served.get()).isEqualTo(4);
            assertThat(maxInFlight.get()).isEqualTo(2);
        } finally {
            callers.shutdownNow();
            server.stop(0);
        }
    }

    @Test
    void givenEmptyInputList_whenEmbed_thenReturnsEmptyWithoutRequest() throws IOException {
        Setup setup = mocked();

        assertThat(setup.client().embed(List.of())).isEmpty();
        setup.server().verify();
    }

    @Test
    void givenBackoffOf60ms_whenEveryAttemptFails_thenWaitsOneThenTwoBackoffsBetweenAttempts() {
        EmbeddingProperties slow = embeddingProperties("bge-m3", 1024, Duration.ofMillis(60), 2);
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ollama.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.times(3), requestTo(URL)).andRespond(withServerError());
        EmbeddingClient client = new EmbeddingClient(builder.build(), slow);
        long start = System.nanoTime();

        assertThatThrownBy(() -> client.embed(List.of("a"))).isInstanceOf(ServiceUnavailableException.class);

        // 1 x 60 ms + 2 x 60 ms = 180 ms
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(180));
    }
}
