package com.rasitbank.assistant.retrieval.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.rasitbank.assistant.config.RestClientConfig;
import com.rasitbank.assistant.exception.ServiceUnavailableException;
import com.rasitbank.assistant.support.TestProperties;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Arama istemcisinin isteği çalışma alanı 42'ye doğru kurduğunu, yanıtı eksiksiz okuduğunu ve her hatayı
 * RETRIEVAL_UNAVAILABLE yaptığını doğrular.
 */
class RetrievalClientTest {

    private static void assertRetrievalUnavailable(Throwable thrown) {
        assertThat(thrown).isInstanceOfSatisfying(ServiceUnavailableException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo("RETRIEVAL_UNAVAILABLE"));
    }

    @Test
    void givenHits_whenSearch_thenPostsQueryAndTopKToWorkspace42() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://retrieval/api/workspaces/42/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"query\":\"q\",\"topK\":20}", true))
                .andRespond(withSuccess("{\"hits\":[{\"chunkId\":151,\"chunkNo\":31,\"pageNumber\":19,"
                        + "\"heading\":\"2.16 Kayıp ya da çalıntı bildirimi\",\"content\":\"içerik\","
                        + "\"sourceId\":10001,\"sourceName\":\"Rehber\",\"version\":\"2026.10\","
                        + "\"distance\":0.3124}]}", MediaType.APPLICATION_JSON));
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        List<SearchHit> hits = client.search("q", 20);

        assertThat(hits).containsExactly(new SearchHit(151, 31, 19, "2.16 Kayıp ya da çalıntı bildirimi", "içerik",
                10001, "Rehber", "2026.10", 0.3124));
        server.verify();
    }

    @Test
    void givenNoHits_whenSearch_thenEmptyList() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("http://retrieval/api/workspaces/42/search"))
                .andRespond(withSuccess("{\"hits\":[]}", MediaType.APPLICATION_JSON));
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        assertThat(client.search("q", 5)).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] HTTP {0}")
    @CsvSource({"503", "409", "500", "404", "400"})
    void givenRetrievalReturnsErrorStatus_whenSearch_thenRetrievalUnavailable(int status) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("http://retrieval/api/workspaces/42/search"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"errorCode\":\"EMBEDDING_UNAVAILABLE\"}"));
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        assertThatThrownBy(() -> client.search("q", 5)).satisfies(RetrievalClientTest::assertRetrievalUnavailable);
    }

    @Test
    void givenUnreadableErrorBody_whenSearch_thenStillRetrievalUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("http://retrieval/api/workspaces/42/search"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.TEXT_HTML).body("<html>"));
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        assertThatThrownBy(() -> client.search("q", 5)).satisfies(RetrievalClientTest::assertRetrievalUnavailable);
    }

    @Test
    void givenSuccessWithEmptyBody_whenSearch_thenRetrievalUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("http://retrieval/api/workspaces/42/search"))
                .andRespond(withSuccess());
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        assertThatThrownBy(() -> client.search("q", 5)).satisfies(RetrievalClientTest::assertRetrievalUnavailable);
    }

    @Test
    void givenConnectionRefused_whenSearch_thenRetrievalUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        var properties = TestProperties.withRetrievalApi("http://127.0.0.1:" + closedPort);
        RetrievalClient client = new RetrievalClient(
                new RestClientConfig().retrievalRestClient(RestClient.builder(), properties), properties);

        assertThatThrownBy(() -> client.search("q", 5)).satisfies(RetrievalClientTest::assertRetrievalUnavailable);
    }

    @Test
    void givenWorkspace_whenGetWorkspace_thenGetsWorkspace42AndReadsEmbeddingFields() {
        // Arrange
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://retrieval/api/workspaces/42")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"workspaceId\":42,\"name\":\"Raşit Bankası · Dijital Asistan\","
                        + "\"embeddingModel\":\"bge-m3\",\"embeddingDimensions\":1024,\"extra\":1}",
                        MediaType.APPLICATION_JSON));
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        // Act
        WorkspaceResponse workspace = client.getWorkspace();

        // Assert
        assertThat(workspace).isEqualTo(new WorkspaceResponse(42, "Raşit Bankası · Dijital Asistan", "bge-m3", 1024));
        server.verify();
    }

    @Test
    void givenSourcePage_whenListSources_thenSendsPageAndSizeAndReadsNullableChunkCount() {
        // Arrange
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://retrieval/api/workspaces/42/sources?page=1&size=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"content\":[{\"sourceId\":10001,\"name\":\"Rehber\","
                        + "\"version\":\"2026.10\",\"chunkCount\":61},{\"sourceId\":10002,\"name\":\"Yeni\","
                        + "\"version\":\"v1\",\"chunkCount\":null}],\"page\":1,\"size\":100,"
                        + "\"totalElements\":102,\"totalPages\":2}", MediaType.APPLICATION_JSON));
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        // Act
        SourcePageResponse page = client.listSources(1, 100);

        // Assert
        assertThat(page.content()).containsExactly(new SourceResponse(10001, "Rehber", "2026.10", 61),
                new SourceResponse(10002, "Yeni", "v1", null));
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(102);
        server.verify();
    }

    @ParameterizedTest(name = "[{index}] HTTP {0}")
    @CsvSource({"503", "404", "500"})
    void givenErrorStatus_whenGetWorkspace_thenRetrievalUnavailable(int status) {
        // Arrange
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer.bindTo(builder).build().expect(requestTo("http://retrieval/api/workspaces/42"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        // Act
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(client::getWorkspace);

        // Assert
        assertRetrievalUnavailable(thrown);
    }

    @Test
    void givenErrorStatus_whenListSources_thenRetrievalUnavailable() {
        // Arrange
        RestClient.Builder builder = RestClient.builder().baseUrl("http://retrieval");
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://retrieval/api/workspaces/42/sources?page=0&size=100"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        RetrievalClient client = new RetrievalClient(builder.build(), TestProperties.defaults());

        // Act
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> client.listSources(0, 100));

        // Assert
        assertRetrievalUnavailable(thrown);
    }
}
