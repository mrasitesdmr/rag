package com.rasitbank.assistant.retrieval.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rasitbank.assistant.config.RestClientConfig;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.support.TestProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Rerank istemcisinin isteği doğru kurduğunu, puanları belge sırasına hizaladığını ve her bozukluğu, hatayı ve
 * zaman aşımını RERANKER_UNAVAILABLE yaptığını doğrular.
 */
class RerankClientTest {

    private static final List<String> DOCUMENTS = List.of("a", "b", "c");

    private static ServiceUnavailableException unavailable(Throwable thrown) {
        assertThat(thrown).isInstanceOf(ServiceUnavailableException.class);
        return (ServiceUnavailableException) thrown;
    }

    @Test
    void givenUnorderedResults_whenRerankChunks_thenScoresAlignedToDocumentIndex() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://reranker");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://reranker/v1/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"model\":\"bge-reranker-v2-m3\",\"query\":\"q\","
                        + "\"documents\":[\"a\",\"b\",\"c\"]}", true))
                .andRespond(withSuccess("{\"results\":[{\"index\":2,\"relevance_score\":5.36},"
                        + "{\"index\":0,\"relevance_score\":-11.04},{\"index\":1,\"relevance_score\":0.5}]}",
                        MediaType.APPLICATION_JSON));
        RerankClient client = new RerankClient(builder.build(), TestProperties.defaults());

        List<Double> scores = client.rerankChunks("q", DOCUMENTS);

        assertThat(scores).containsExactly(-11.04, 0.5, 5.36);
        server.verify();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "{\"results\":[{\"index\":0,\"relevance_score\":1.0},{\"index\":1,\"relevance_score\":2.0}]}",
            "{\"results\":[{\"index\":0,\"relevance_score\":1.0},{\"index\":0,\"relevance_score\":2.0},"
                    + "{\"index\":2,\"relevance_score\":3.0}]}",
            "{\"results\":[{\"index\":0,\"relevance_score\":1.0},{\"index\":1,\"relevance_score\":2.0},"
                    + "{\"index\":3,\"relevance_score\":3.0}]}",
            "{\"results\":[{\"index\":0,\"relevance_score\":1.0},{\"index\":1,\"relevance_score\":2.0},"
                    + "{\"index\":-1,\"relevance_score\":3.0}]}",
            "{\"results\":[{\"index\":0,\"relevance_score\":1.0},{\"index\":1,\"relevance_score\":2.0},"
                    + "{\"index\":2}]}",
            "{\"results\":[{\"index\":0,\"relevance_score\":1.0},{\"index\":1,\"relevance_score\":2.0},"
                    + "{\"relevance_score\":3.0}]}",
            "{\"results\":[{\"index\":0,\"relevance_score\":1.0},{\"index\":1,\"relevance_score\":2.0},"
                    + "{\"index\":2,\"relevance_score\":3.0},{\"index\":0,\"relevance_score\":4.0}]}",
            "{}",
            "{\"results\":[]}"
    })
    void givenMissingDuplicateOrOutOfRangeResult_whenRerankChunks_thenRerankerUnavailable(String body) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://reranker");
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("http://reranker/v1/rerank"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        RerankClient client = new RerankClient(builder.build(), TestProperties.defaults());

        assertThatThrownBy(() -> client.rerankChunks("q", DOCUMENTS)).satisfies(thrown ->
                assertThat(unavailable(thrown).getErrorCode()).isEqualTo("RERANKER_UNAVAILABLE"));
    }

    @Test
    void givenHttp500_whenRerankChunks_thenRerankerUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://reranker");
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("http://reranker/v1/rerank"))
                .andRespond(withServerError());
        RerankClient client = new RerankClient(builder.build(), TestProperties.defaults());

        assertThatThrownBy(() -> client.rerankChunks("q", DOCUMENTS)).satisfies(thrown ->
                assertThat(unavailable(thrown).getErrorCode()).isEqualTo("RERANKER_UNAVAILABLE"));
    }

    @Test
    void givenEmptyBody_whenRerankChunks_thenRerankerUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://reranker");
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("http://reranker/v1/rerank"))
                .andRespond(withSuccess());
        RerankClient client = new RerankClient(builder.build(), TestProperties.defaults());

        assertThatThrownBy(() -> client.rerankChunks("q", DOCUMENTS)).satisfies(thrown ->
                assertThat(unavailable(thrown).getErrorCode()).isEqualTo("RERANKER_UNAVAILABLE"));
    }

    @Test
    void givenRerankerSlowerThanReadTimeout_whenRerankChunks_thenThrowsRerankerUnavailable() throws IOException {
        HttpServer slowServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slowServer.createContext("/v1/rerank", exchange -> {
            try {
                Thread.sleep(2_000);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException | IOException ignored) {
                // test bitti, istemci bağlantıyı kapattı
            } finally {
                exchange.close();
            }
        });
        slowServer.start();
        try {
            var properties = TestProperties.withReranker("http://127.0.0.1:" + slowServer.getAddress().getPort(),
                    Duration.ofMillis(200));
            RerankClient client = new RerankClient(
                    new RestClientConfig().rerankRestClient(RestClient.builder(), properties), properties);

            long startNanos = System.nanoTime();
            assertThatThrownBy(() -> client.rerankChunks("q", DOCUMENTS)).satisfies(thrown ->
                    assertThat(unavailable(thrown).getErrorCode()).isEqualTo("RERANKER_UNAVAILABLE"));
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

            assertThat(elapsedMs).isLessThan(1_500);
        } finally {
            slowServer.stop(0);
        }
    }
}
