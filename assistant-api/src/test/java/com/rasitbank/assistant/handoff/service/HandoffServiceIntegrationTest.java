package com.rasitbank.assistant.handoff.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.handoff.domain.dto.HandoffResponse;
import com.rasitbank.assistant.handoff.domain.dto.RequestHandoffRequest;
import com.rasitbank.assistant.handoff.domain.entity.HandoffReason;
import com.rasitbank.assistant.support.FixedClockTestConfiguration;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.TurnFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aynı konuşma için aynı anda gelen iki aktarma isteğinin hatasız bittiğini, tek kayıt oluştuğunu ve ikisine de aynı
 * cevabın döndüğünü doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(FixedClockTestConfiguration.class)
class HandoffServiceIntegrationTest extends PostgresIntegrationTest {

    private static final int REPEATS = 20;

    @Autowired
    private HandoffService handoffService;

    @Autowired
    private JdbcTemplate jdbc;

    private TurnFixtures turns;

    @BeforeEach
    void setUp() {
        turns = new TurnFixtures(jdbc);
        turns.clean();
    }

    @Test
    void givenTwoConcurrentRequests_whenBothFinish_thenOneRowAndSameResponse() throws Exception {
        List<long[]> cases = new ArrayList<>();
        for (int i = 0; i < REPEATS; i++) {
            long conversationId = turns.conversation();
            cases.add(new long[] {conversationId, turns.notFoundTurn(conversationId),
                    turns.notFoundTurn(conversationId)});
        }
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (long[] c : cases) {
                CountDownLatch start = new CountDownLatch(1);
                Future<HandoffResponse> a = pool.submit(() -> requestAfter(start, c[0], c[1]));
                Future<HandoffResponse> b = pool.submit(() -> requestAfter(start, c[0], c[2]));
                start.countDown();
                HandoffResponse first = a.get(10, TimeUnit.SECONDS);
                HandoffResponse second = b.get(10, TimeUnit.SECONDS);

                assertThat(first).isEqualTo(second);
                assertThat(first.queue()).isEqualTo("bireysel");
                assertThat(first.createdAt()).isEqualTo(FixedClockTestConfiguration.NOW);
                assertThat(jdbc.queryForObject("select count(*) from handoffs where conversation_id = ?",
                        Integer.class, c[0])).isEqualTo(1);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(turns.count("handoffs")).isEqualTo(REPEATS);
    }

    private HandoffResponse requestAfter(CountDownLatch start, long conversationId, long turnId) {
        try {
            start.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        return handoffService.requestHandoff(conversationId, new RequestHandoffRequest(turnId, HandoffReason.NO_ANSWER));
    }
}
