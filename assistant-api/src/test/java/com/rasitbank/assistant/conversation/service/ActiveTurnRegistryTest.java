package com.rasitbank.assistant.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Süren turun sorusunu ve başlama anını tuttuğunu, ikinci başlatmayı reddettiğini ve bitince bıraktığını doğrular.
 */
class ActiveTurnRegistryTest {

    private final ActiveTurnRegistry registry = new ActiveTurnRegistry();

    @Test
    void givenActiveTurn_whenTryStartAgain_thenRejectedAndSnapshotKept() {
        // Arrange
        Instant first = Instant.parse("2026-11-02T10:00:00Z");
        registry.tryStart(7L, "Birinci soru", first);

        // Act
        boolean second = registry.tryStart(7L, "İkinci soru", Instant.parse("2026-11-02T10:00:09Z"));

        // Assert
        assertThat(second).isFalse();
        assertThat(registry.findActiveTurn(7L)).contains(new ActiveTurn("Birinci soru", first));
    }

    @Test
    void givenFinishedTurn_whenFindActiveTurn_thenEmptyAndConversationStartableAgain() {
        // Arrange
        registry.tryStart(7L, "Soru", Instant.EPOCH);
        registry.finish(7L);

        // Act
        var found = registry.findActiveTurn(7L);

        // Assert
        assertThat(found).isEmpty();
        assertThat(registry.activeTurns()).isEmpty();
        assertThat(registry.tryStart(7L, "Yeni", Instant.EPOCH)).isTrue();
    }

    @Test
    void givenTwoConversations_whenActiveTurnsRead_thenCopyHoldsBothAndIsDetached() {
        // Arrange
        registry.tryStart(1L, "a", Instant.EPOCH);
        registry.tryStart(2L, "b", Instant.EPOCH);

        // Act
        var snapshot = registry.activeTurns();
        registry.finish(1L);

        // Assert
        assertThat(snapshot).containsOnlyKeys(1L, 2L);
        assertThat(registry.activeTurns()).containsOnlyKeys(2L);
    }
}
