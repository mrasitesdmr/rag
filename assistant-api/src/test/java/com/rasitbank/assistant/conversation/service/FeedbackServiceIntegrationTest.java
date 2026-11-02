package com.rasitbank.assistant.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rasitbank.assistant.conversation.domain.dto.SubmitFeedbackRequest;
import com.rasitbank.assistant.support.FixedClockTestConfiguration;
import com.rasitbank.assistant.support.PostgresIntegrationTest;
import com.rasitbank.assistant.support.TurnFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * Aynı tura aynı anda gelen zıt geri bildirimlerin tek bir değerle bittiğini ve turun diğer kolonlarının bozulmadığını
 * (kayıp güncelleme olmadığını) doğrular.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(FixedClockTestConfiguration.class)
class FeedbackServiceIntegrationTest extends PostgresIntegrationTest {

    private static final int REPEATS = 20;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private JdbcTemplate jdbc;

    private TurnFixtures turns;

    @BeforeEach
    void setUp() {
        turns = new TurnFixtures(jdbc);
        turns.clean();
    }

    @Test
    void givenConcurrentOppositeFeedbacks_whenBothFinish_thenOneValueAndOtherColumnsUnchanged() throws Exception {
        long conversationId = turns.conversation();
        List<Long> turnIds = new ArrayList<>();
        for (int i = 0; i < REPEATS; i++) {
            turnIds.add(turns.answeredTurn(conversationId));
        }
        Map<Long, Map<String, Object>> before = new java.util.HashMap<>();
        for (long id : turnIds) {
            before.put(id, otherColumns(id));
        }
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (long id : turnIds) {
                CountDownLatch start = new CountDownLatch(1);
                Future<?> yes = pool.submit(() -> submitAfter(start, id, true));
                Future<?> no = pool.submit(() -> submitAfter(start, id, false));
                start.countDown();
                yes.get(10, TimeUnit.SECONDS);
                no.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        for (long id : turnIds) {
            Map<String, Object> row = jdbc.queryForMap("select helpful, feedback_at from conversation_turns where id = ?", id);
            assertThat(row.get("helpful")).isInstanceOf(Boolean.class);
            assertThat(row.get("feedback_at")).isNotNull();
            assertThat(otherColumns(id)).isEqualTo(before.get(id));
        }
    }

    private void submitAfter(CountDownLatch start, long turnId, boolean helpful) {
        try {
            start.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        }
        feedbackService.submitFeedback(turnId, new SubmitFeedbackRequest(helpful));
    }

    private Map<String, Object> otherColumns(long turnId) {
        return jdbc.queryForMap(
                "select answer, cited::text as cited, not_found_answer, latency_ms, error_code, question from conversation_turns where id = ?",
                turnId);
    }
}
